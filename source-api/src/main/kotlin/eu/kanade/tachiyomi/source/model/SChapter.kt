@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.source.model

import java.io.Serializable

interface SChapter : Serializable {

	var url: String

	var name: String

	var date_upload: Long

	var chapter_number: Float

	var scanlator: String?

	fun copy() = create().also {
		it.url = url
		it.name = name
		it.date_upload = date_upload
		it.chapter_number = chapter_number
		it.scanlator = scanlator
	}

	companion object {
		const val UNKNOWN = -2f

		fun create(): SChapter {
			return SChapterImpl()
		}
	}
}
