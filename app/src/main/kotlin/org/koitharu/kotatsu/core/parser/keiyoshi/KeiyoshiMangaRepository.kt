package org.koitharu.kotatsu.core.parser.keiyoshi

import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.parser.CachingMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.EnumSet

class KeiyoshiMangaRepository(
	override val source: KeiyoshiMangaSource,
	private val sourceWrapper: KeiyoshiSourceWrapper,
	cache: MemoryContentCache,
) : CachingMangaRepository(cache) {

	override val sortOrders: Set<SortOrder>
		get() = if (sourceWrapper.supportsLatest) {
			EnumSet.of(SortOrder.POPULARITY, SortOrder.NEWEST)
		} else {
			EnumSet.of(SortOrder.POPULARITY)
		}

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isMultipleTagsSupported = false,
			isTagsExclusionSupported = false,
			isSearchWithFiltersSupported = false,
		)

	override var defaultSortOrder: SortOrder
		get() = SortOrder.POPULARITY
		set(value) = Unit

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga> {
		val page = (offset / PAGE_SIZE) + 1

		if (!filter?.query.isNullOrEmpty()) {
			val (mangas, _) = sourceWrapper.getSearchManga(page, filter!!.query!!)
			return mangas
		}

		return when (order ?: defaultSortOrder) {
			SortOrder.NEWEST -> {
				if (sourceWrapper.supportsLatest) {
					val (mangas, _) = sourceWrapper.getLatestUpdates(page)
					mangas
				} else {
					val (mangas, _) = sourceWrapper.getPopularManga(page)
					mangas
				}
			}
			else -> {
				val (mangas, _) = sourceWrapper.getPopularManga(page)
				mangas
			}
		}
	}

	override suspend fun getDetailsImpl(manga: Manga): Manga {
		val details = sourceWrapper.getMangaDetails(manga)
		val chapters = sourceWrapper.getChapterList(manga)
		return details.copy(chapters = chapters)
	}

	override suspend fun getPagesImpl(chapter: MangaChapter): List<MangaPage> {
		return sourceWrapper.getPageList(chapter)
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		return sourceWrapper.getImageUrl(page)
	}

	override suspend fun getRelatedMangaImpl(seed: Manga): List<Manga> = emptyList()

	companion object {
		private const val PAGE_SIZE = 20
	}
}
