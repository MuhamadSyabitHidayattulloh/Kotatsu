package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage

interface CatalogueSource : Source {

	override val lang: String

	val supportsLatest: Boolean

	suspend fun getPopularManga(page: Int): MangasPage = throw NotImplementedError()

	suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage = throw NotImplementedError()

	suspend fun getLatestUpdates(page: Int): MangasPage = throw NotImplementedError()

	fun getFilterList(): FilterList = FilterList()
}
