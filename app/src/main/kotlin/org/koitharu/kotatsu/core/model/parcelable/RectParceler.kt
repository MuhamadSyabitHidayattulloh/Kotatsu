package org.koitharu.kotatsu.core.model.parcelable

import android.os.Parcel
import kotlinx.parcelize.Parceler
import org.koitharu.kotatsu.parsers.bitmap.Rect

object RectParceler : Parceler<Rect> {
	override fun create(parcel: Parcel): Rect {
		return Rect(
			left = parcel.readInt(),
			top = parcel.readInt(),
			right = parcel.readInt(),
			bottom = parcel.readInt(),
		)
	}

	override fun Rect.write(parcel: Parcel, flags: Int) {
		parcel.writeInt(left)
		parcel.writeInt(top)
		parcel.writeInt(right)
		parcel.writeInt(bottom)
	}
}
