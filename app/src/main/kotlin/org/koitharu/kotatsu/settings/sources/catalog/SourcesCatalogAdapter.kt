package org.koitharu.kotatsu.settings.sources.catalog

import android.content.Context
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel

class SourcesCatalogAdapter(
	listener: OnListItemClickListener<SourceCatalogItem.Source>,
	keiyoshiListener: OnListItemClickListener<SourceCatalogItem.KeiyoshiSource>,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.CHAPTER_LIST, sourceCatalogItemSourceAD(listener))
		addDelegate(ListItemType.NAV_ITEM, sourceCatalogItemKeiyoshiAD(keiyoshiListener))
		addDelegate(ListItemType.HINT_EMPTY, sourceCatalogItemHintAD())
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		val item = items.getOrNull(position)
		return when (item) {
			is SourceCatalogItem.Source -> item.source.getTitle(context).take(1)
			is SourceCatalogItem.KeiyoshiSource -> item.source.getTitle(context).take(1)
			else -> null
		}
	}
}
