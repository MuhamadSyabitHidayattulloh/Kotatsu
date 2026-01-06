package org.koitharu.kotatsu.core.parser.keiyoshi

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import dalvik.system.PathClassLoader
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug

class KeiyoshiExtensionLoader(private val context: Context) {

	private val extensionCache = mutableMapOf<String, List<KeiyoshiSourceWrapper>>()

	fun loadExtensions(): List<KeiyoshiMangaSource> {
		val pkgManager = context.packageManager
		val installedPkgs = getInstalledPackages(pkgManager)

		Log.d(TAG, "Total installed packages: ${installedPkgs.size}")

		val extensionPkgs = installedPkgs.filter { isPackageKeiyoshiExtension(it) }
		Log.d(TAG, "Found ${extensionPkgs.size} Keiyoshi/Tachiyomi extensions")

		val sources = extensionPkgs.flatMap { pkgInfo ->
			Log.d(TAG, "Loading extension: ${pkgInfo.packageName}")
			loadExtensionSources(pkgInfo)
		}

		Log.d(TAG, "Total sources loaded: ${sources.size}")
		return sources
	}

	fun getSourceWrapper(source: KeiyoshiMangaSource): KeiyoshiSourceWrapper? {
		val wrappers = extensionCache[source.packageName]
		if (wrappers != null) {
			return wrappers.find { it.id == source.sourceId }
		}

		val pkgInfo = try {
			context.packageManager.getPackageInfo(source.packageName, PACKAGE_FLAGS)
		} catch (e: Exception) {
			return null
		}

		if (!isPackageKeiyoshiExtension(pkgInfo)) return null

		val loadedWrappers = loadSourceWrappers(pkgInfo)
		extensionCache[source.packageName] = loadedWrappers
		return loadedWrappers.find { it.id == source.sourceId }
	}

	private fun loadExtensionSources(pkgInfo: PackageInfo): List<KeiyoshiMangaSource> {
		val pkgManager = context.packageManager
		val appInfo = pkgInfo.applicationInfo
		if (appInfo == null) {
			Log.w(TAG, "No applicationInfo for ${pkgInfo.packageName}")
			return emptyList()
		}

		val pkgName = pkgInfo.packageName
		val versionName = pkgInfo.versionName ?: ""
		val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

		val extName = pkgManager.getApplicationLabel(appInfo).toString()
			.substringAfter("Tachiyomi: ")
			.substringAfter("Keiyoshi: ")

		Log.d(TAG, "Extension name: $extName, sourceDir: ${appInfo.sourceDir}")

		val isNsfw = appInfo.metaData?.getInt(METADATA_NSFW) == 1
		val icon = try {
			appInfo.loadIcon(pkgManager)
		} catch (e: Exception) {
			null
		}

		val sourceClassesString = appInfo.metaData?.getString(METADATA_SOURCE_CLASS)
		Log.d(TAG, "Source classes metadata: $sourceClassesString")

		val sourceClasses = sourceClassesString
			?.split(";")
			?.map { it.trim() }
			?.filter { it.isNotEmpty() }

		if (sourceClasses.isNullOrEmpty()) {
			Log.w(TAG, "No source classes found for $pkgName")
			return emptyList()
		}

		Log.d(TAG, "Found ${sourceClasses.size} source classes: $sourceClasses")

		val classLoader = try {
			PathClassLoader(appInfo.sourceDir, null, context.classLoader)
		} catch (e: Exception) {
			Log.e(TAG, "Failed to create classloader for $pkgName", e)
			e.printStackTraceDebug()
			return emptyList()
		}

		return sourceClasses.flatMap { className ->
			try {
				val fullClassName = if (className.startsWith(".")) {
					pkgName + className
				} else {
					className
				}
				Log.d(TAG, "Loading class: $fullClassName")

				val sourceClass = Class.forName(fullClassName, false, classLoader)
				val source = sourceClass.getDeclaredConstructor().newInstance()

				Log.d(TAG, "Successfully instantiated: $fullClassName")

				val sources = if (isSourceFactory(source)) {
					Log.d(TAG, "Source is a SourceFactory")
					@Suppress("UNCHECKED_CAST")
					source.javaClass.getMethod("createSources").invoke(source) as? List<Any> ?: emptyList()
				} else {
					listOf(source)
				}

				Log.d(TAG, "Created ${sources.size} sources from $fullClassName")

				sources.map { s ->
					val sourceId = getSourceId(s)
					val sourceName = getSourceName(s)
					val sourceLang = getSourceLang(s)

					Log.d(TAG, "Source: id=$sourceId, name=$sourceName, lang=$sourceLang")

					KeiyoshiMangaSource(
						packageName = pkgName,
						sourceId = sourceId,
						sourceName = sourceName.ifEmpty { extName },
						sourceLang = sourceLang,
						versionName = versionName,
						versionCode = versionCode,
						isNsfw = isNsfw,
						icon = icon,
					)
				}
			} catch (e: Exception) {
				Log.e(TAG, "Failed to load source class from $pkgName: ${e.message}", e)
				e.printStackTraceDebug()
				emptyList()
			}
		}
	}

	private fun loadSourceWrappers(pkgInfo: PackageInfo): List<KeiyoshiSourceWrapper> {
		val appInfo = pkgInfo.applicationInfo ?: return emptyList()
		val pkgName = pkgInfo.packageName

		val sourceClasses = appInfo.metaData?.getString(METADATA_SOURCE_CLASS)
			?.split(";")
			?.map { it.trim() }
			?.filter { it.isNotEmpty() }
			?: return emptyList()

		val classLoader = try {
			PathClassLoader(appInfo.sourceDir, null, context.classLoader)
		} catch (e: Exception) {
			e.printStackTraceDebug()
			return emptyList()
		}

		return sourceClasses.flatMap { className ->
			try {
				val fullClassName = if (className.startsWith(".")) {
					pkgName + className
				} else {
					className
				}
				val sourceClass = Class.forName(fullClassName, false, classLoader)
				val source = sourceClass.getDeclaredConstructor().newInstance()

				val sources = if (isSourceFactory(source)) {
					@Suppress("UNCHECKED_CAST")
					source.javaClass.getMethod("createSources").invoke(source) as? List<Any> ?: emptyList()
				} else {
					listOf(source)
				}

				val versionName = pkgInfo.versionName ?: ""
				val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
				val isNsfw = appInfo.metaData?.getInt(METADATA_NSFW) == 1
				val icon = try {
					appInfo.loadIcon(context.packageManager)
				} catch (e: Exception) {
					null
				}

				sources.map { s ->
					val sourceId = getSourceId(s)
					val sourceName = getSourceName(s)
					val sourceLang = getSourceLang(s)

					val keiyoshiSource = KeiyoshiMangaSource(
						packageName = pkgName,
						sourceId = sourceId,
						sourceName = sourceName,
						sourceLang = sourceLang,
						versionName = versionName,
						versionCode = versionCode,
						isNsfw = isNsfw,
						icon = icon,
					)
					KeiyoshiSourceWrapper(s, keiyoshiSource)
				}
			} catch (e: Exception) {
				e.printStackTraceDebug()
				emptyList()
			}
		}
	}

	private fun isSourceFactory(obj: Any): Boolean {
		return obj.javaClass.interfaces.any { it.name == "eu.kanade.tachiyomi.source.SourceFactory" }
	}

	private fun getSourceId(source: Any): Long {
		return try {
			source.javaClass.getMethod("getId").invoke(source) as? Long ?: 0L
		} catch (e: Exception) {
			0L
		}
	}

	private fun getSourceName(source: Any): String {
		return try {
			source.javaClass.getMethod("getName").invoke(source) as? String ?: ""
		} catch (e: Exception) {
			""
		}
	}

	private fun getSourceLang(source: Any): String {
		return try {
			source.javaClass.getMethod("getLang").invoke(source) as? String ?: ""
		} catch (e: Exception) {
			""
		}
	}

	private fun isPackageKeiyoshiExtension(pkgInfo: PackageInfo): Boolean {
		val hasFeature = pkgInfo.reqFeatures?.any { it.name == EXTENSION_FEATURE } == true
		val hasMetaData = pkgInfo.applicationInfo?.metaData?.containsKey(METADATA_SOURCE_CLASS) == true

		if (hasFeature || hasMetaData) {
			Log.d(TAG, "Extension candidate: ${pkgInfo.packageName} (feature=$hasFeature, metadata=$hasMetaData)")
		}

		return hasFeature || hasMetaData
	}

	@Suppress("DEPRECATION")
	private fun getInstalledPackages(pkgManager: PackageManager): List<PackageInfo> {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
		} else {
			pkgManager.getInstalledPackages(PACKAGE_FLAGS)
		}
	}

	companion object {
		private const val TAG = "KeiyoshiExtLoader"
		private const val EXTENSION_FEATURE = "tachiyomi.extension"
		private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
		private const val METADATA_NSFW = "tachiyomi.extension.nsfw"

		@Suppress("DEPRECATION")
		private val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
			PackageManager.GET_META_DATA or
			PackageManager.GET_SIGNATURES or
			(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0)
	}
}
