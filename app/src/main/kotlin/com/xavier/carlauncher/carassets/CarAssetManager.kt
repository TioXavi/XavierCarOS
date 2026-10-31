package com.xavier.carlauncher.carassets

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.xavier.carlauncher.R
import java.io.File
import java.io.FileOutputStream

/**
 * Definition of the 7 vehicle graphic slots for XavierCarOS.
 */
enum class CarSlot(
    val title: String,
    val subtitle: String,
    val filename: String,
    val defaultDrawableRes: Int
) {
    BASE(
        title = "Vehículo Base (Luz Diurna)",
        subtitle = "Carrocería principal con luces apagadas",
        filename = "custom_corolla_base.png",
        defaultDrawableRes = R.drawable.corolla_base
    ),
    LIGHTS_ON(
        title = "Vehículo (Luces Encendidas / Noche)",
        subtitle = "Carrocería con faros principales y antinieblas iluminados",
        filename = "custom_corolla_lights_on.png",
        defaultDrawableRes = R.drawable.corolla_lights_on
    ),
    DOOR_FR(
        title = "Puerta Delantera Derecha (Copiloto)",
        subtitle = "Capa de puerta del copiloto abierta",
        filename = "custom_corolla_door_fr.png",
        defaultDrawableRes = R.drawable.corolla_door_fr
    ),
    DOOR_FL(
        title = "Puerta Delantera Izquierda (Conductor)",
        subtitle = "Capa de puerta del conductor abierta",
        filename = "custom_corolla_door_fl.png",
        defaultDrawableRes = R.drawable.corolla_door_fl
    ),
    DOOR_RR(
        title = "Puerta Trasera Derecha",
        subtitle = "Capa de puerta trasera derecha abierta",
        filename = "custom_corolla_door_rr.png",
        defaultDrawableRes = R.drawable.corolla_door_rr
    ),
    DOOR_RL(
        title = "Puerta Trasera Izquierda",
        subtitle = "Capa de puerta trasera izquierda abierta",
        filename = "custom_corolla_door_rl.png",
        defaultDrawableRes = R.drawable.corolla_door_rl
    ),
    HOOD(
        title = "Capó / Vano Motor",
        subtitle = "Capa del capó levantado mostrando el motor",
        filename = "custom_corolla_hood.png",
        defaultDrawableRes = R.drawable.corolla_hood
    ),
    TRUNK(
        title = "Maletero / Alerón",
        subtitle = "Capa del maletero con alerón deportivo levantado",
        filename = "custom_corolla_trunk.png",
        defaultDrawableRes = R.drawable.corolla_trunk
    );
}

/**
 * Manages custom vehicle assets stored in the app's internal persistent directory.
 */
object CarAssetManager {

    private fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, "custom_car_assets")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun hasCustomImage(context: Context, slot: CarSlot): Boolean {
        return File(getStorageDir(context), slot.filename).exists()
    }

    fun getCustomBitmap(context: Context, slot: CarSlot): ImageBitmap? {
        val file = File(getStorageDir(context), slot.filename)
        if (!file.exists()) return null
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveCustomImage(context: Context, slot: CarSlot, uri: Uri): Boolean {
        return try {
            val file = File(getStorageDir(context), slot.filename)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetSlot(context: Context, slot: CarSlot): Boolean {
        val file = File(getStorageDir(context), slot.filename)
        return if (file.exists()) file.delete() else true
    }

    fun resetAll(context: Context) {
        val dir = getStorageDir(context)
        dir.listFiles()?.forEach { it.delete() }
    }
}

