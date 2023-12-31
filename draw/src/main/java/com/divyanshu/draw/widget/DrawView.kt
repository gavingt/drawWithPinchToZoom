package com.divyanshu.draw.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.divyanshu.draw.R
import java.io.Serializable

data class PaintOptions(var color: Int = Color.parseColor("#660000FF"), var strokeWidth: Float = 19f, var alpha: Int = 50) : Serializable


class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // TODO: Maybe we just need these properties.
    private var paths = LinkedHashMap<MyPath, PaintOptions>()
    private var lastPaths = LinkedHashMap<MyPath, PaintOptions>()
    private var undonePaths = LinkedHashMap<MyPath, PaintOptions>()
    private var paint = Paint()
    private var myPath = MyPath()
    private var paintOptions = PaintOptions()
    private var currentX = 0f
    private var currentY = 0f
    private var startX = 0f
    private var startY = 0f


    // TODO: We probably don't need these.
    private var matrix = Matrix()
    private var isScrolling = false
    private var currentScaleFactor = 1f
    private var scrollOriginX = 0f
    private var scrollOriginY = 0f
    private var scrollX = 0f
    private var scrollY = 0f
    private var scaleGestureDetector: ScaleGestureDetector

    private var backgroundBitmap: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.car)
    private var backgroundRect = RectF()
    private var visibleRect = Rect()


    init {
        paint.apply {
            color = paintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = paintOptions.strokeWidth
            isAntiAlias = true
        }

        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val oldScale = currentScaleFactor
                currentScaleFactor *= detector.scaleFactor
                // Set min and max zoom levels.
                currentScaleFactor = currentScaleFactor.coerceAtLeast(0.2f).coerceAtMost(3.0f)
                scrollX += detector.focusX * (oldScale - currentScaleFactor) / currentScaleFactor
                scrollY += detector.focusY * (oldScale - currentScaleFactor) / currentScaleFactor
                invalidate()
                return true
            }
        })
    }


    fun undo() {
        if (paths.isEmpty() && lastPaths.isNotEmpty()) {
            paths = lastPaths.clone() as LinkedHashMap<MyPath, PaintOptions>
            lastPaths.clear()
            invalidate()
            return
        }
        if (paths.isEmpty()) return

        val lastPath = paths.values.lastOrNull()
        val lastKey = paths.keys.lastOrNull()

        paths.remove(lastKey)
        if (lastPath != null && lastKey != null) {
            undonePaths[lastKey] = lastPath
        }
        invalidate()
    }


    fun redo() {
        if (undonePaths.keys.isEmpty()) return
        val lastKey = undonePaths.keys.last()
        paths[lastKey] = undonePaths.values.last()
        undonePaths.remove(lastKey)
        invalidate()
    }


    fun reset() {
        undonePaths.clear()
        paths.clear()
        lastPaths.clear()

        backgroundBitmap = null
        lastPaths = paths.clone() as LinkedHashMap<MyPath, PaintOptions>
        myPath.reset()
        paths.clear()
        invalidate()
    }


    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        return bitmap
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Store current clip bounds in visibleRect.
        canvas.getClipBounds(visibleRect)

        matrix.setTranslate(scrollX + visibleRect.centerX(), scrollY + visibleRect.centerY())
        matrix.postScale(currentScaleFactor, currentScaleFactor)

        backgroundBitmap?.let {
            backgroundRect.left = -it.width.toFloat() / 2
            backgroundRect.right = it.width.toFloat() / 2
            backgroundRect.top = -it.height.toFloat() / 2
            backgroundRect.bottom = it.height.toFloat() / 2
            if (it.height == 1 && it.width == 1) {
                backgroundRect.set(visibleRect)
            } else {
                matrix.mapRect(backgroundRect)
            }

            canvas.drawBitmap(it, null, backgroundRect, null)
        }

        canvas.setMatrix(matrix)
        matrix.invert(matrix)

        for ((key, _) in paths) {
            canvas.drawPath(key, paint)
        }

        canvas.drawPath(myPath, paint)
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        fun getPointerCenter(event: MotionEvent): MotionEvent.PointerCoords {
            val result = MotionEvent.PointerCoords()
            val temp = MotionEvent.PointerCoords()
            for (i in 0 until event.pointerCount) {
                event.getPointerCoords(i, temp)
                result.x += temp.x
                result.y += temp.y
            }

            if (event.pointerCount > 0) {
                result.x /= event.pointerCount.toFloat()
                result.y /= event.pointerCount.toFloat()
            }

            return result
        }


        // TODO: We probably need to add this to solve problem with extra strokes when zooming.
        fun handleScroll(event: MotionEvent): Boolean {
            if (event.action != MotionEvent.ACTION_UP && event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_POINTER_DOWN
                && event.action != MotionEvent.ACTION_POINTER_UP && event.action != MotionEvent.ACTION_MOVE
            ) {
                return false
            }

            val shouldScroll = event.pointerCount > 1
            val center = getPointerCenter(event)
            if (shouldScroll != isScrolling) {
                undonePaths.clear()
                myPath.reset()
                if (shouldScroll) {
                    isScrolling = true
                    scrollOriginX = center.x
                    scrollOriginY = center.y
                } else if (event.action == MotionEvent.ACTION_UP) {
                    isScrolling = false
                }
                return true
            }

            if (shouldScroll) {
                scrollX += (center.x - scrollOriginX) / currentScaleFactor
                scrollY += (center.y - scrollOriginY) / currentScaleFactor
                scrollOriginX = center.x
                scrollOriginY = center.y
                invalidate()
            }
            return isScrolling
        }


        scaleGestureDetector.onTouchEvent(event)
        if (handleScroll(event)) return true

        val points: FloatArray = floatArrayOf(event.x, event.y)
        matrix.mapPoints(points)
        val x = points[0]
        val y = points[1]

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y
                myPath.reset()
                myPath.moveTo(x, y)
                currentX = x
                currentY = y
                undonePaths.clear()
            }

            MotionEvent.ACTION_MOVE -> {
                myPath.quadTo(currentX, currentY, (x + currentX) / 2, (y + currentY) / 2)
                currentX = x
                currentY = y
            }

            MotionEvent.ACTION_UP -> {
                myPath.lineTo(currentX, currentY)

                // draw a dot on click
                if (startX == currentX && startY == currentY) {
                    myPath.lineTo(currentX, currentY + 2)
                    myPath.lineTo(currentX + 1, currentY + 2)
                    myPath.lineTo(currentX + 1, currentY)
                }

                paths[myPath] = paintOptions
                myPath = MyPath()
                paintOptions = PaintOptions(paintOptions.color, paintOptions.strokeWidth, paintOptions.alpha)
            }
        }

        invalidate()
        return true
    }
}