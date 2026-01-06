package org.koitharu.kotatsu.core.parser.keiyoshi

import android.util.Log
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage

class KeiyoshiSourceWrapper(
	private val source: Any,
	private val keiyoshiSource: KeiyoshiMangaSource,
) {

	private val catalogueSource: CatalogueSource? = source as? CatalogueSource

	val id: Long
		get() = keiyoshiSource.sourceId

	val name: String
		get() = keiyoshiSource.sourceName

	val lang: String
		get() = keiyoshiSource.sourceLang

	val supportsLatest: Boolean
		get() = catalogueSource?.supportsLatest ?: false

	suspend fun getPopularManga(page: Int): Pair<List<Manga>, Boolean> = withContext(Dispatchers.IO) {
		try {
			val mangasPage = catalogueSource?.getPopularManga(page)
				?: return@withContext emptyList<Manga>() to false
			val mangas = mangasPage.mangas.map { sManga ->
				KeiyoshiModelConverter.convertManga(sManga, keiyoshiSource, keiyoshiSource.isNsfw)
			}
			mangas to mangasPage.hasNextPage
		} catch (e: Exception) {
			Log.e(TAG, "getPopularManga failed", e)
			emptyList<Manga>() to false
		}
	}

	suspend fun getSearchManga(page: Int, query: String): Pair<List<Manga>, Boolean> = withContext(Dispatchers.IO) {
		try {
			val mangasPage = catalogueSource?.getSearchManga(page, query, FilterList())
				?: return@withContext emptyList<Manga>() to false
			val mangas = mangasPage.mangas.map { sManga ->
				KeiyoshiModelConverter.convertManga(sManga, keiyoshiSource, keiyoshiSource.isNsfw)
			}
			mangas to mangasPage.hasNextPage
		} catch (e: Exception) {
			Log.e(TAG, "getSearchManga failed", e)
			emptyList<Manga>() to false
		}
	}

	suspend fun getLatestUpdates(page: Int): Pair<List<Manga>, Boolean> = withContext(Dispatchers.IO) {
		try {
			val mangasPage = catalogueSource?.getLatestUpdates(page)
				?: return@withContext emptyList<Manga>() to false
			val mangas = mangasPage.mangas.map { sManga ->
				KeiyoshiModelConverter.convertManga(sManga, keiyoshiSource, keiyoshiSource.isNsfw)
			}
			mangas to mangasPage.hasNextPage
		} catch (e: Exception) {
			Log.e(TAG, "getLatestUpdates failed", e)
			emptyList<Manga>() to false
		}
	}

	suspend fun getMangaDetails(manga: Manga): Manga = withContext(Dispatchers.IO) {
		try {
			val sManga = createSMangaFromManga(manga)
			val resultSManga = catalogueSource?.getMangaDetails(sManga)
				?: return@withContext manga
			KeiyoshiModelConverter.convertManga(resultSManga, keiyoshiSource, keiyoshiSource.isNsfw).copy(
				chapters = manga.chapters,
			)
		} catch (e: Exception) {
			Log.e(TAG, "getMangaDetails failed", e)
			manga
		}
	}

	suspend fun getChapterList(manga: Manga): List<MangaChapter> = withContext(Dispatchers.IO) {
		try {
			val sManga = createSMangaFromManga(manga)
			val chapters = catalogueSource?.getChapterList(sManga) ?: emptyList()
			chapters.map { sChapter ->
				KeiyoshiModelConverter.convertChapter(sChapter, keiyoshiSource)
			}.reversed()
		} catch (e: Exception) {
			Log.e(TAG, "getChapterList failed", e)
			emptyList()
		}
	}

	suspend fun getPageList(chapter: MangaChapter): List<MangaPage> = withContext(Dispatchers.IO) {
		try {
			val sChapter = createSChapterFromChapter(chapter)
			val pages = catalogueSource?.getPageList(sChapter) ?: emptyList()
			pages.mapIndexed { index, page ->
				KeiyoshiModelConverter.convertPage(page, index, keiyoshiSource)
			}
		} catch (e: Exception) {
			Log.e(TAG, "getPageList failed", e)
			emptyList()
		}
	}

	suspend fun getImageUrl(page: MangaPage): String = withContext(Dispatchers.IO) {
		page.url
	}

	private fun createSMangaFromManga(manga: Manga): SManga {
		return SManga.create().apply {
			url = manga.url
			title = manga.title
			thumbnail_url = manga.coverUrl
			description = manga.description
			author = manga.authors.firstOrNull()
		}
	}

	private fun createSChapterFromChapter(chapter: MangaChapter): SChapter {
		return SChapter.create().apply {
			url = chapter.url
			name = chapter.title ?: ""
			chapter_number = chapter.number
			date_upload = chapter.uploadDate
		}
	}

	companion object {
		private const val TAG = "KeiyoshiWrapper"
	}
}
