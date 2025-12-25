package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EdgeEffect
import androidx.core.view.ViewCompat.TYPE_TOUCH
import androidx.core.view.forEach
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import androidx.core.view.iterator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections
import java.util.LinkedList
import java.util.WeakHashMap

class WebtoonRecyclerView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

	private var onPageScrollListeners = LinkedList<OnWebtoonScrollListener>()
	private val scrollDispatcher = WebtoonScrollDispatcher()
	private val detachedViews = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
	private var isFixingScroll = false

	private val pullGestureTracker = PullGestureTracker()

	var isPullGestureEnabled: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				setEdgeEffectFactory(
					if (value) NoOpEdgeEffectFactory() else EdgeEffectFactory()
				)
				if (!value) {
					pullGestureTracker.reset(true)
				}
			}
		}

	var pullThreshold: Float = 0.3f
	private var pullListener: OnPullGestureListener? = null

	fun setOnPullGestureListener(listener: OnPullGestureListener?) {
		pullListener = listener
	}

	override fun onChildDetachedFromWindow(child: View) {
		super.onChildDetachedFromWindow(child)
		detachedViews.add(child)
	}

	override fun onChildAttachedToWindow(child: View) {
		super.onChildAttachedToWindow(child)
		detachedViews.remove(child)
	}

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		if (isPullGestureEnabled && pullListener != null) {
			pullGestureTracker.onTouchEvent(ev)
		} else {
			pullGestureTracker.reset(false)
		}
		return super.dispatchTouchEvent(ev)
	}

	override fun onDetachedFromWindow() {
		pullGestureTracker.reset(true)
		super.onDetachedFromWindow()
	}

	override fun startNestedScroll(axes: Int) = startNestedScroll(axes, TYPE_TOUCH)

	override fun startNestedScroll(axes: Int, type: Int): Boolean = isNotEmpty()

	override fun dispatchNestedPreScroll(
		dx: Int,
		dy: Int,
		consumed: IntArray?,
		offsetInWindow: IntArray?
	) = dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, TYPE_TOUCH)

	override fun dispatchNestedPreScroll(
		dx: Int,
		dy: Int,
		consumed: IntArray?,
		offsetInWindow: IntArray?,
		type: Int
	): Boolean {
		val consumedY = consumeVerticalScroll(dy)
		if (consumed != null) {
			consumed[0] = 0
			consumed[1] = consumedY
		}
		notifyScrollChanged(dy)
		return consumedY != 0 || dy == 0
	}

	private fun consumeVerticalScroll(dy: Int): Int {
		if (isEmpty()) return 0
		return when {
			dy > 0 -> {
				val child = getChildAt(0) as WebtoonFrameLayout
				var consumed = child.dispatchVerticalScroll(dy)
				if (consumed < dy && childCount > 1) {
					val next = getChildAt(1) as WebtoonFrameLayout
					val unconsumed = dy - consumed - next.top
					if (unconsumed > 0) {
						consumed += next.dispatchVerticalScroll(unconsumed)
					}
				}
				consumed
			}
			dy < 0 -> {
				val child = getChildAt(childCount - 1) as WebtoonFrameLayout
				var consumed = child.dispatchVerticalScroll(dy)
				if (consumed > dy && childCount > 1) {
					val prev = getChildAt(childCount - 2) as WebtoonFrameLayout
					val unconsumed = dy - consumed + (height - prev.bottom)
					if (unconsumed < 0) {
						consumed += prev.dispatchVerticalScroll(unconsumed)
					}
				}
				consumed
			}
			else -> 0
		}
	}

	fun addOnPageScrollListener(listener: OnWebtoonScrollListener) {
		onPageScrollListeners.add(listener)
	}

	fun removeOnPageScrollListener(listener: OnWebtoonScrollListener) {
		onPageScrollListeners.remove(listener)
	}

	private fun notifyScrollChanged(dy: Int) {
		if (onPageScrollListeners.isEmpty()) return
		scrollDispatcher.dispatchScroll(this, dy)
	}

	fun relayoutChildren() {
		forEach { (it as WebtoonFrameLayout).target.requestLayout() }
		detachedViews.forEach { (it as WebtoonFrameLayout).target.requestLayout() }
	}

	fun updateChildrenScroll() {
		if (isFixingScroll) return
		isFixingScroll = true
		for (child in this) {
			val target = (child as WebtoonFrameLayout).target
			if (adjustScroll(child, target)) break
		}
		isFixingScroll = false
	}

	private fun adjustScroll(child: View, ssiv: WebtoonImageView): Boolean = when {
		child.bottom < height && ssiv.getScroll() < ssiv.getScrollRange() -> {
			val d = minOf(height - child.bottom, ssiv.getScrollRange() - ssiv.getScroll())
			ssiv.scrollBy(d)
			true
		}
		child.top > 0 && ssiv.getScroll() > 0 -> {
			val d = minOf(child.top, ssiv.getScroll())
			ssiv.scrollBy(-d)
			true
		}
		else -> false
	}

	private class WebtoonScrollDispatcher {
		private var firstPos = NO_POSITION
		private var lastPos = NO_POSITION

		fun dispatchScroll(rv: WebtoonRecyclerView, dy: Int) {
			val lm = rv.layoutManager as? LinearLayoutManager ?: run {
				firstPos = NO_POSITION
				lastPos = NO_POSITION
				return
			}
			val f = lm.findFirstVisibleItemPosition()
			val l = lm.findLastVisibleItemPosition()
			if (f != firstPos || l != lastPos) {
				firstPos = f
				lastPos = l
				if (f != NO_POSITION && l != NO_POSITION) {
					rv.onPageScrollListeners.forEach {
						it.onScrollChanged(rv, dy, f, l)
					}
				}
			}
		}
	}

	private class NoOpEdgeEffectFactory : EdgeEffectFactory() {
		override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect =
			object : EdgeEffect(view.context) {
				override fun draw(canvas: Canvas): Boolean = false
			}
	}

	private inner class PullGestureTracker {

		private val edgeTolerancePx = 2

		private var edge = PullEdge.NONE
		private var lastY = 0f
		private var distancePx = 0f
		private var isTracking = false

		fun onTouchEvent(ev: MotionEvent) {
			val listener = pullListener ?: return
			if (!isPullGestureEnabled) return
			when (ev.actionMasked) {
				MotionEvent.ACTION_DOWN -> {
					reset(false)
					isTracking = true
					lastY = ev.y
				}
				MotionEvent.ACTION_MOVE -> {
					if (!isTracking) return
					val dy = ev.y - lastY
					lastY = ev.y
					if (dy != 0f) handleMove(dy, listener)
				}
				MotionEvent.ACTION_UP -> if (isTracking) finish(listener, false)
				MotionEvent.ACTION_CANCEL -> if (isTracking) finish(listener, true)
			}
		}

		fun reset(notify: Boolean) {
			val listener = pullListener
			edge = PullEdge.NONE
			distancePx = 0f
			lastY = 0f
			isTracking = false
			if (notify && listener != null) {
				listener.onPullProgressTop(0f)
				listener.onPullProgressBottom(0f)
				listener.onPullCancelled()
			}
		}

		private fun handleMove(dy: Float, listener: OnPullGestureListener) {
			if (height <= 0) return
			if (edge == PullEdge.NONE) {
				edge = when {
					dy > 0f && isAtAbsoluteTop() -> PullEdge.TOP
					dy < 0f && isAtAbsoluteBottom() -> PullEdge.BOTTOM
					else -> PullEdge.NONE
				}
				if (edge == PullEdge.NONE) return
			}
			val delta = if (edge == PullEdge.TOP) dy else -dy
			distancePx = (distancePx + delta).coerceAtLeast(0f)
			val progress = distancePx / (height * pullThreshold).coerceAtLeast(1f)
			if (edge == PullEdge.TOP) {
				listener.onPullProgressTop(progress)
			} else {
				listener.onPullProgressBottom(progress)
			}
			if (distancePx <= 0f) edge = PullEdge.NONE
		}

		private fun finish(listener: OnPullGestureListener, cancelled: Boolean) {
			val progress = if (height > 0) distancePx / (height * pullThreshold).coerceAtLeast(1f) else 0f
			val e = edge
			edge = PullEdge.NONE
			distancePx = 0f
			lastY = 0f
			isTracking = false
			listener.onPullProgressTop(0f)
			listener.onPullProgressBottom(0f)
			when {
				cancelled -> listener.onPullCancelled()
				e == PullEdge.TOP && progress >= 1f -> listener.onPullTriggeredTop()
				e == PullEdge.BOTTOM && progress >= 1f -> listener.onPullTriggeredBottom()
				else -> listener.onPullCancelled()
			}
		}

		private fun isAtAbsoluteTop(): Boolean {
			val lm = layoutManager as? LinearLayoutManager
			if (lm != null && lm.findFirstVisibleItemPosition() != 0) return false
			if (!canScrollVertically(-1)) return true
			if (childCount <= 0) return true
			val child = getChildAt(0) as? WebtoonFrameLayout ?: return true
			return child.target.getScroll() <= edgeTolerancePx
		}

		private fun isAtAbsoluteBottom(): Boolean {
			val adapter = adapter ?: return false
			val lm = layoutManager as? LinearLayoutManager
			if (lm != null && lm.findLastVisibleItemPosition() != adapter.itemCount - 1) return false
			if (!canScrollVertically(1)) return true
			if (childCount <= 0) return true
			val child = getChildAt(childCount - 1) as? WebtoonFrameLayout ?: return true
			val ssiv = child.target
			return ssiv.getScrollRange() - ssiv.getScroll() <= edgeTolerancePx
		}
	}

	private enum class PullEdge {
		NONE,
		TOP,
		BOTTOM
	}

	interface OnWebtoonScrollListener {
		fun onScrollChanged(
			recyclerView: WebtoonRecyclerView,
			dy: Int,
			firstVisiblePosition: Int,
			lastVisiblePosition: Int
		)
	}

	interface OnPullGestureListener {
		fun onPullProgressTop(progress: Float)
		fun onPullProgressBottom(progress: Float)
		fun onPullTriggeredTop()
		fun onPullTriggeredBottom()
		fun onPullCancelled()
	}
}
