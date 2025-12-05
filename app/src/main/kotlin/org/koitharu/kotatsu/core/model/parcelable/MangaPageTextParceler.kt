package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import kotlinx.parcelize.Parceler
import org.koitharu.kotatsu.parsers.bitmap.Rect
import org.koitharu.kotatsu.parsers.model.MangaPageText

object MangaPageTextParceler : Parceler<MangaPageText> {
	override fun create(parcel: Parcel): MangaPageText {
		val rect = RectParceler.create(parcel)
		val text = parcel.readString() ?: ""
		return MangaPageText(rect, text)
	}

	override fun MangaPageText.write(parcel: Parcel, flags: Int) {
		with(RectParceler) {
			rect.write(parcel, flags)
		}
		parcel.writeString(text)
	}
}
