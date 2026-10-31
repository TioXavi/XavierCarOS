package com.xavier.carlauncher.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Live vehicle telemetry retrieved via Bluetooth ELM327 OBD-II adapter.
 */
data class ObdData(
    val speedMph: Float = 0f,
    val rpm: Int = 0,
    val coolantTempC: Int = 0,
    val batteryVoltage: Float = 12.6f,
    val isConnected: Boolean = false,
    val connectionStatus: String = "Desconectado"
)

/**
 * Senior Automotive OBD-II Manager for Toyota Corolla.
 *
 * Communicates with ELM327 Bluetooth dongles over SPP (Serial Port Profile).
 * Queries standard SAE J1979 PIDs:
 * - 010D: Vehicle Speed Sensor (latencia cero vs GPS)
 * - 010C: Engine RPM
 * - 0105: Engine Coolant Temperature
 * - ATRV: Real-time 12V Battery Voltage
 */
class ObdManager(
    private val bluetoothAdapter: BluetoothAdapter?
) {
    companion object {
        private const val TAG = "XavierCarOS_OBD"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val KMH_TO_MPH = 0.621371f
    }

    private val _obdData = MutableStateFlow(ObdData())
    val obdData: StateFlow<ObdData> = _obdData.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun connectToAdapter(targetDeviceAddress: String? = null) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _obdData.value = _obdData.value.copy(connectionStatus = "Bluetooth apagado")
            return
        }

        scope.launch {
            try {
                _obdData.value = _obdData.value.copy(connectionStatus = "Buscando adaptador OBD2...")

                // Buscar dispositivo emparejado con nombre OBD, ELM o la dirección especificada
                val pairedDevices = bluetoothAdapter.bondedDevices
                val device: BluetoothDevice? = if (targetDeviceAddress != null) {
                    pairedDevices.firstOrNull { it.address.equals(targetDeviceAddress, ignoreCase = true) }
                } else {
                    pairedDevices.firstOrNull {
                        val name = it.name?.uppercase() ?: ""
                        name.contains("OBD") || name.contains("ELM") || name.contains("VGATE") || name.contains("CAR")
                    }
                }

                if (device == null) {
                    _obdData.value = _obdData.value.copy(connectionStatus = "No se encontró adaptador OBD2 emparejado")
                    Log.w(TAG, "No OBD-II device found in bonded devices")
                    return@launch
                }

                _obdData.value = _obdData.value.copy(connectionStatus = "Conectando a ${device.name}...")
                bluetoothAdapter.cancelDiscovery()

                val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()
                socket = s
                inputStream = s.inputStream
                outputStream = s.outputStream

                _obdData.value = _obdData.value.copy(isConnected = true, connectionStatus = "Conectado a ${device.name}")
                Log.i(TAG, "Connected to OBD-II device: ${device.name}")

                // Inicializar ELM327
                initElm327()

                // Comenzar bucle de sondeo
                startPolling()

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to OBD-II device", e)
                _obdData.value = _obdData.value.copy(
                    isConnected = false,
                    connectionStatus = "Error de conexión: ${e.localizedMessage}"
                )
                disconnect()
            }
        }
    }

    private suspend fun initElm327() = withContext(Dispatchers.IO) {
        sendCommand("ATZ")      // Reset
        delay(800)
        sendCommand("ATE0")     // Echo off
        delay(200)
        sendCommand("ATL0")     // Linefeeds off
        delay(200)
        sendCommand("ATSP0")    // Set protocol to Auto
        delay(300)
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive && socket?.isConnected == true) {
                try {
                    // 1. Leer Velocidad (PID 010D)
                    val speedResp = sendCommand("010D")
                    val speedMph = parseSpeedMph(speedResp)

                    // 2. Leer RPM (PID 010C)
                    val rpmResp = sendCommand("010C")
                    val rpm = parseRpm(rpmResp)

                    // 3. Leer Temperatura refrigerante (PID 0105)
                    val tempResp = sendCommand("0105")
                    val tempC = parseCoolantTemp(tempResp)

                    // 4. Leer Voltaje batería (ATRV)
                    val voltResp = sendCommand("ATRV")
                    val voltage = parseVoltage(voltResp)

                    _obdData.value = _obdData.value.copy(
                        speedMph = speedMph ?: _obdData.value.speedMph,
                        rpm = rpm ?: _obdData.value.rpm,
                        coolantTempC = tempC ?: _obdData.value.coolantTempC,
                        batteryVoltage = voltage ?: _obdData.value.batteryVoltage,
                        isConnected = true,
                        connectionStatus = "Activo"
                    )

                    delay(150) // Refresco a ~6 Hz para balance óptimo de CPU/CAN
                } catch (e: Exception) {
                    Log.w(TAG, "OBD polling tick error: ${e.message}")
                    delay(1000)
                }
            }
        }
    }

    private fun sendCommand(cmd: String): String {
        val out = outputStream ?: return ""
        val `in` = inputStream ?: return ""

        val fullCmd = "$cmd\r"
        out.write(fullCmd.toByteArray())
        out.flush()

        val buffer = StringBuilder()
        val byteBuf = ByteArray(64)
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < 800) {
            if (`in`.available() > 0) {
                val read = `in`.read(byteBuf)
                if (read > 0) {
                    val str = String(byteBuf, 0, read)
                    buffer.append(str)
                    if (buffer.contains(">")) {
                        break
                    }
                }
            } else {
                Thread.sleep(15)
            }
        }
        return buffer.toString().trim()
    }

    private fun parseSpeedMph(raw: String): Float? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "")
        val match = Regex("410D([0-9A-Fa-f]{2})").find(clean) ?: return null
        val hex = match.groupValues[1]
        val kmh = hex.toIntOrNull(16) ?: return null
        return kmh * KMH_TO_MPH
    }

    private fun parseRpm(raw: String): Int? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "")
        val match = Regex("410C([0-9A-Fa-f]{4})").find(clean) ?: return null
        val hex = match.groupValues[1]
        val value = hex.toIntOrNull(16) ?: return null
        return value / 4
    }

    private fun parseCoolantTemp(raw: String): Int? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "")
        val match = Regex("4105([0-9A-Fa-f]{2})").find(clean) ?: return null
        val hex = match.groupValues[1]
        val tempRaw = hex.toIntOrNull(16) ?: return null
        return tempRaw - 40
    }

    private fun parseVoltage(raw: String): Float? {
        val clean = raw.replace("V", "").replace("v", "").replace(">", "").trim()
        val match = Regex("(\\d+\\.\\d+)").find(clean) ?: return null
        return match.groupValues[1].toFloatOrNull()
    }

    fun disconnect() {
        pollingJob?.cancel()
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing OBD socket", e)
        } finally {
            socket = null
            inputStream = null
            outputStream = null
            _obdData.value = _obdData.value.copy(isConnected = false, connectionStatus = "Desconectado")
        }
    }
}

