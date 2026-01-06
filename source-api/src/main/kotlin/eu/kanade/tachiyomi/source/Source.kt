package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga

interface Source {

	val id: Long

	val name: String

	val lang: String
		get() = ""

	suspend fun getMangaDetails(manga: SManga): SManga = throw NotImplementedError()

	suspend fun getChapterList(manga: SManga): List<SChapter> = throw NotImplementedError()

	suspend fun getPageList(chapter: SChapter): List<Page> = throw NotImplementedError()
}
