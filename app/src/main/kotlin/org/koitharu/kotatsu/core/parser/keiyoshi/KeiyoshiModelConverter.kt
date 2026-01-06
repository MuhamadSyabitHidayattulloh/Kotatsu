package org.koitharu.kotatsu.core.parser.keiyoshi

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag

object KeiyoshiModelConverter {

	fun convertManga(sManga: SManga, source: MangaSource, isNsfw: Boolean): Manga {
		val url = sManga.url
		val title = sManga.title
		val artist = sManga.artist
		val author = sManga.author
		val description = sManga.description
		val genre = sManga.genre
		val status = sManga.status
		val thumbnailUrl = sManga.thumbnail_url

		val tags = genre?.split(",")
			?.map { it.trim() }
			?.filter { it.isNotEmpty() }
			?.mapTo(HashSet()) { tagName ->
				MangaTag(
					key = tagName.lowercase().replace(" ", "_"),
					title = tagName,
					source = source,
				)
			}.orEmpty()

		val authors = buildSet {
			author?.let { add(it) }
			artist?.let { add(it) }
		}

		return Manga(
			id = generateId(url, source),
			title = title,
			altTitles = emptySet(),
			url = url,
			publicUrl = url,
			rating = RATING_UNKNOWN,
			contentRating = if (isNsfw) ContentRating.ADULT else null,
			coverUrl = thumbnailUrl,
			tags = tags,
			state = convertStatus(status),
			authors = authors,
			largeCoverUrl = null,
			description = description,
			chapters = emptyList(),
			source = source,
		)
	}

	fun convertChapter(sChapter: SChapter, source: MangaSource): MangaChapter {
		val url = sChapter.url
		val name = sChapter.name
		val dateUpload = sChapter.date_upload
		val chapterNumber = sChapter.chapter_number
		val scanlator = sChapter.scanlator

		return MangaChapter(
			id = generateId(url, source),
			title = name.ifEmpty { null },
			number = chapterNumber,
			volume = 0,
			url = url,
			scanlator = scanlator,
			uploadDate = dateUpload,
			branch = null,
			source = source,
		)
	}

	fun convertPage(page: Page, index: Int, source: MangaSource): MangaPage {
		val url = page.url
		val imageUrl = page.imageUrl

		return MangaPage(
			id = generateId("$url$index", source),
			url = imageUrl ?: url,
			preview = null,
			source = source,
		)
	}

	private fun convertStatus(status: Int): MangaState? {
		return when (status) {
			SMANGA_ONGOING -> MangaState.ONGOING
			SMANGA_COMPLETED -> MangaState.FINISHED
			SMANGA_LICENSED -> MangaState.PAUSED
			SMANGA_PUBLISHING_FINISHED -> MangaState.FINISHED
			SMANGA_CANCELLED -> MangaState.ABANDONED
			SMANGA_ON_HIATUS -> MangaState.PAUSED
			else -> null
		}
	}

	private fun generateId(key: String, source: MangaSource): Long {
		val fullKey = "${source.name}:$key"
		return fullKey.hashCode().toLong() and Long.MAX_VALUE
	}

	private const val RATING_UNKNOWN = -1f

	private const val SMANGA_ONGOING = 1
	private const val SMANGA_COMPLETED = 2
	private const val SMANGA_LICENSED = 3
	private const val SMANGA_PUBLISHING_FINISHED = 4
	private const val SMANGA_CANCELLED = 5
	private const val SMANGA_ON_HIATUS = 6
}
