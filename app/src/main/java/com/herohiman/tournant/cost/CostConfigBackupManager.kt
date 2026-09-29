package com.herohiman.tournant.cost

import android.content.ContentResolver
import android.net.Uri
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CostConfigBackupManager {

	val moshi: Moshi by lazy {
		Moshi.Builder()
			.add(KotlinJsonAdapterFactory())
			.build()
	}

	val adapter: JsonAdapter<CostConfigBackupPayload> by lazy {
		moshi.adapter(CostConfigBackupPayload::class.java).indent("\t")
	}

	fun serializeToJson(payload: CostConfigBackupPayload): String {
		return adapter.toJson(payload)
	}

	fun deserializeFromJson(jsonString: String): CostConfigBackupPayload? {
		return try {
			adapter.fromJson(jsonString)
		} catch (e: Exception) {
			null
		}
	}

	fun exportToStream(payload: CostConfigBackupPayload, outputStream: OutputStream) {
		val json = serializeToJson(payload)
		outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
			writer.write(json)
			writer.flush()
		}
	}

	fun importFromStream(inputStream: InputStream): CostConfigBackupPayload? {
		return try {
			val json = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
			deserializeFromJson(json)
		} catch (e: Exception) {
			null
		}
	}

	fun exportToUri(contentResolver: ContentResolver, uri: Uri, payload: CostConfigBackupPayload): Boolean {
		return try {
			contentResolver.openOutputStream(uri)?.use { outputStream ->
				exportToStream(payload, outputStream)
				true
			} ?: false
		} catch (e: Exception) {
			false
		}
	}

	fun importFromUri(contentResolver: ContentResolver, uri: Uri): CostConfigBackupPayload? {
		return try {
			contentResolver.openInputStream(uri)?.use { inputStream ->
				importFromStream(inputStream)
			}
		} catch (e: Exception) {
			null
		}
	}

	fun generateBackupFilename(timestamp: Long = System.currentTimeMillis()): String {
		val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
		return "tournant_cost_config_${formatter.format(Date(timestamp))}.json"
	}
}
