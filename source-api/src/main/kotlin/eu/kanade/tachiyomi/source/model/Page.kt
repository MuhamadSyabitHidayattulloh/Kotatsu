package eu.kanade.tachiyomi.source.model

import android.net.Uri
import java.io.Serializable

open class Page @JvmOverloads constructor(
	val index: Int,
	val url: String = "",
	var imageUrl: String? = null,
	@Transient var uri: Uri? = null,
) : Serializable {

	val number: Int
		get() = index + 1

	@Transient
	@Volatile
	var status: State = State.QUEUE
		set(value) {
			field = value
		}

	@Transient
	@Volatile
	var progress: Int = 0

	enum class State {
		QUEUE,
		LOAD_PAGE,
		DOWNLOAD_IMAGE,
		READY,
		ERROR,
	}
}
