package org.koitharu.kotatsu.reader.ui.pager

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.parsers.model.MangaPageText
import kotlin.math.max
import kotlin.math.min

class TextOverlayView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

	private var processedTextOverlays: List<ProcessedTextOverlay>? = null
	private var imageWidth: Int = 0
	private var imageHeight: Int = 0

	private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
		color = Color.BLACK
		textSize = 32f
		style = Paint.Style.FILL
	}
	
	private val shadowPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
		color = Color.WHITE
		textSize = 32f
		style = Paint.Style.STROKE
		strokeWidth = context.resources.resolveDp(2f)
	}
	
	private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.argb(245, 255, 255, 255)
		style = Paint.Style.FILL
	}
	
	private val shadowBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.argb(80, 0, 0, 0)
		style = Paint.Style.FILL
		maskFilter = BlurMaskFilter(context.resources.resolveDp(4f), BlurMaskFilter.Blur.NORMAL)
	}
	
	private val padding = context.resources.resolveDp(8f)
	private val cornerRadius = context.resources.resolveDp(8f)
	private val tempRect = RectF()
	private val shadowRect = RectF()
	private val textBounds = Rect()

	internal fun processTextOverlays(
		texts: List<MangaPageText>,
		imageWidth: Int,
		imageHeight: Int,
	): List<ProcessedTextOverlay> {
		this.imageWidth = imageWidth
		this.imageHeight = imageHeight

		val scaleX = width.toFloat() / imageWidth
		val scaleY = height.toFloat() / imageHeight

		return texts.map { overlay ->
			val left = overlay.rect.left * scaleX
			val top = overlay.rect.top * scaleY
			val rectWidth = overlay.rect.width * scaleX
			val rectHeight = overlay.rect.height * scaleY

			val maxWidth = (rectWidth - padding * 2).toInt()
			if (maxWidth <= 0) return@map null

			val fontSize = calculateOptimalFontSize(
				text = overlay.text,
				maxWidth = maxWidth,
				maxHeight = rectHeight - padding * 2,
				minSize = context.resources.resolveDp(8f),
				maxSize = context.resources.resolveDp(72f),
				targetSize = rectHeight * 0.6f,
			)

			val layout = createStaticLayout(overlay.text, textPaint.apply { this.textSize = fontSize }, maxWidth)

			ProcessedTextOverlay(
				staticLayout = layout,
				left = left,
				top = top,
				rectWidth = rectWidth,
				backgroundHeight = layout.height.toFloat() + padding * 2,
			)
		}.filterNotNull()
	}

	fun renderTextOverlays(processedData: Any) {
		@Suppress("UNCHECKED_CAST")
		this.processedTextOverlays = processedData as? List<ProcessedTextOverlay>
		post { invalidate() }
	}


	fun clear() {
		processedTextOverlays = null
		invalidate()
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		val overlays = processedTextOverlays ?: return
		if (imageWidth == 0 || imageHeight == 0 || width == 0 || height == 0) return

		overlays.forEach { overlay ->
			tempRect.set(
				overlay.left,
				overlay.top,
				overlay.left + overlay.rectWidth,
				overlay.top + overlay.backgroundHeight,
			)

			shadowRect.set(tempRect)
			shadowRect.offset(0f, context.resources.resolveDp(2f))
			canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowBackgroundPaint)
			canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, backgroundPaint)

			canvas.save()
			canvas.translate(overlay.left + padding, overlay.top + padding)
			overlay.staticLayout.draw(canvas)
			canvas.restore()
		}
	}

	private fun calculateOptimalFontSize(
		text: String,
		maxWidth: Int,
		maxHeight: Float,
		minSize: Float,
		maxSize: Float,
		targetSize: Float,
	): Float {
		var low = minSize
		var high = min(maxSize, targetSize)
		var bestSize = low

		while (high - low > 0.5f) {
			val mid = (low + high) / 2f
			val layout = createStaticLayout(text, textPaint.apply { textSize = mid }, maxWidth)

			if (layout.height <= maxHeight) {
				bestSize = mid
				low = mid
			} else {
				high = mid
			}
		}

		return max(bestSize, minSize)
	}

	private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
		return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
			.setAlignment(Layout.Alignment.ALIGN_CENTER)
			.setLineSpacing(0f, 1f)
			.setIncludePad(false)
			.build()
	}

	internal data class ProcessedTextOverlay(
		val staticLayout: StaticLayout,
		val left: Float,
		val top: Float,
		val rectWidth: Float,
		val backgroundHeight: Float,
	)
}
