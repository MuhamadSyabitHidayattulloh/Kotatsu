package org.koitharu.kotatsu.core.parser.keiyoshi

import android.content.Context
import android.graphics.drawable.Drawable
import org.koitharu.kotatsu.parsers.model.MangaSource

data class KeiyoshiMangaSource(
	val packageName: String,
	val sourceId: Long,
	val sourceName: String,
	val sourceLang: String,
	val versionName: String,
	val versionCode: Long,
	val isNsfw: Boolean,
	val icon: Drawable?,
) : MangaSource {

	override val name: String
		get() = "keiyoshi:$packageName/$sourceId"

	fun isAvailable(context: Context): Boolean {
		return try {
			context.packageManager.getPackageInfo(packageName, 0) != null
		} catch (e: Exception) {
			false
		}
	}

	fun resolveName(context: Context): String {
		return sourceName.ifEmpty {
			try {
				val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
				context.packageManager.getApplicationLabel(appInfo).toString()
			} catch (e: Exception) {
				packageName
			}
		}
	}
}
