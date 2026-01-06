package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

@Suppress("unused")
abstract class HttpSource : CatalogueSource {

	protected val network: NetworkHelper by injectLazy()

	abstract val baseUrl: String

	open val versionId = 1

	override val id by lazy { generateId(name, lang, versionId) }

	val headers: Headers by lazy { headersBuilder().build() }

	open val client: OkHttpClient
		get() = network.client

	protected fun generateId(name: String, lang: String, versionId: Int): Long {
		val key = "${name.lowercase()}/$lang/$versionId"
		val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
		return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
	}

	protected open fun headersBuilder() = Headers.Builder().apply {
		add("User-Agent", network.defaultUserAgentProvider())
	}

	override fun toString() = "$name (${lang.uppercase()})"

	override suspend fun getPopularManga(page: Int): MangasPage {
		val response = client.newCall(popularMangaRequest(page)).execute()
		return popularMangaParse(response)
	}

	protected abstract fun popularMangaRequest(page: Int): Request

	protected abstract fun popularMangaParse(response: Response): MangasPage

	override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
		val response = client.newCall(searchMangaRequest(page, query, filters)).execute()
		return searchMangaParse(response)
	}

	protected abstract fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request

	protected abstract fun searchMangaParse(response: Response): MangasPage

	override suspend fun getLatestUpdates(page: Int): MangasPage {
		val response = client.newCall(latestUpdatesRequest(page)).execute()
		return latestUpdatesParse(response)
	}

	protected abstract fun latestUpdatesRequest(page: Int): Request

	protected abstract fun latestUpdatesParse(response: Response): MangasPage

	override suspend fun getMangaDetails(manga: SManga): SManga {
		val response = client.newCall(mangaDetailsRequest(manga)).execute()
		return mangaDetailsParse(response).apply { initialized = true }
	}

	open fun mangaDetailsRequest(manga: SManga): Request {
		return GET(baseUrl + manga.url, headers)
	}

	protected abstract fun mangaDetailsParse(response: Response): SManga

	override suspend fun getChapterList(manga: SManga): List<SChapter> {
		val response = client.newCall(chapterListRequest(manga)).execute()
		return chapterListParse(response)
	}

	protected open fun chapterListRequest(manga: SManga): Request {
		return GET(baseUrl + manga.url, headers)
	}

	protected abstract fun chapterListParse(response: Response): List<SChapter>

	override suspend fun getPageList(chapter: SChapter): List<Page> {
		val response = client.newCall(pageListRequest(chapter)).execute()
		return pageListParse(response)
	}

	protected open fun pageListRequest(chapter: SChapter): Request {
		return GET(baseUrl + chapter.url, headers)
	}

	protected abstract fun pageListParse(response: Response): List<Page>

	open suspend fun getImageUrl(page: Page): String {
		val response = client.newCall(imageUrlRequest(page)).execute()
		return imageUrlParse(response)
	}

	protected open fun imageUrlRequest(page: Page): Request {
		return GET(page.url, headers)
	}

	protected abstract fun imageUrlParse(response: Response): String

	protected open fun imageRequest(page: Page): Request {
		return GET(page.imageUrl!!, headers)
	}

	fun SChapter.setUrlWithoutDomain(url: String) {
		this.url = getUrlWithoutDomain(url)
	}

	fun SManga.setUrlWithoutDomain(url: String) {
		this.url = getUrlWithoutDomain(url)
	}

	private fun getUrlWithoutDomain(orig: String): String {
		return try {
			val uri = URI(orig.replace(" ", "%20"))
			var out = uri.path
			if (uri.query != null) {
				out += "?" + uri.query
			}
			if (uri.fragment != null) {
				out += "#" + uri.fragment
			}
			out
		} catch (e: URISyntaxException) {
			orig
		}
	}

	open fun getMangaUrl(manga: SManga): String {
		return mangaDetailsRequest(manga).url.toString()
	}

	open fun getChapterUrl(chapter: SChapter): String {
		return pageListRequest(chapter).url.toString()
	}

	open fun prepareNewChapter(chapter: SChapter, manga: SManga) {}

	override fun getFilterList() = FilterList()
}
