package com.divyanshu.draw.widget

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
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.divyanshu.draw.R


class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var paths = LinkedHashMap<MyPath, PaintOptions>()
    private var lastPaths = LinkedHashMap<MyPath, PaintOptions>()
    private var undonePaths = LinkedHashMap<MyPath, PaintOptions>()

    private var paint = Paint()
    private var myPath = MyPath()
    private var paintOptions = PaintOptions()

    private var currentX = 0f
    private var currentY = 0f
    private var startX = 0f
    private var startY = 0f
    private var isSaving = false
    private var isStrokeWidthBarEnabled = false
    private var transform = Matrix()

    private var isScrolling = false
    private var scale = 1f
    private var scrollOriginX = 0f
    private var scrollOriginY = 0f
    private var scrollX = 0f
    private var scrollY = 0f
    private var scaleGestureDetector: ScaleGestureDetector

    private var backgroundBitmap: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.car)
    private var backgroundRect = RectF()

    var tempRect = Rect()

    init {
        paint.apply {
            color = paintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = paintOptions.strokeWidth
            isAntiAlias = true
        }

        scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val oldScale = scale
                    scale *= detector.scaleFactor
                    scale = Math.min(Math.max(scale, 0.5f), 3.0f)
                    scrollX += detector.focusX * (oldScale - scale) / scale
                    scrollY += detector.focusY * (oldScale - scale) / scale
                    invalidate()
                    return true
                }
            })
    }

    fun reset() {
        undonePaths.clear()
        paths.clear()
        lastPaths.clear()
        clearCanvas()
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
        addPath(lastKey, undonePaths.values.last())
        undonePaths.remove(lastKey)
        invalidate()
    }

    fun setColor(newColor: Int) {
        Log.d("alpha", paintOptions.alpha.toString())
        @ColorInt
        val alphaColor = ColorUtils.setAlphaComponent(newColor, paintOptions.alpha)
        paintOptions.color = alphaColor
        if (isStrokeWidthBarEnabled) {
            invalidate()
        }
    }


    fun setAlpha(newAlpha: Int) {
        paintOptions.alpha = newAlpha
        setColor(paintOptions.color)
    }

    fun setStrokeWidth(newStrokeWidth: Float) {
        paintOptions.strokeWidth = newStrokeWidth
        if (isStrokeWidthBarEnabled) {
            invalidate()
        }
    }


    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        isSaving = true
        draw(canvas)
        isSaving = false
        return bitmap
    }


    private fun addPath(path: MyPath, options: PaintOptions) {
        paths[path] = options
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Store current clip bounds in tempRect.
        canvas.getClipBounds(tempRect)

        transform.setTranslate(scrollX + tempRect.centerX(), scrollY + tempRect.centerY())
        transform.postScale(scale, scale)

        backgroundBitmap?.let {
            backgroundRect.left = -it.width.toFloat() / 2
            backgroundRect.right = it.width.toFloat() / 2
            backgroundRect.top = -it.height.toFloat() / 2
            backgroundRect.bottom = it.height.toFloat() / 2
            if (it.height == 1 && it.width == 1) {
                backgroundRect.set(tempRect)
            } else {
                transform.mapRect(backgroundRect)
            }

            backgroundBitmap?.let {
                canvas.drawBitmap(it, null, backgroundRect, null)
            }
        }

        canvas.setMatrix(transform)
        transform.invert(transform)

        for ((key, value) in paths) {
            changePaint(value)
            canvas.drawPath(key, paint)
        }

        changePaint(paintOptions)
        canvas.drawPath(myPath, paint)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        paint.color = paintOptions.color
        paint.strokeWidth = paintOptions.strokeWidth
    }

    fun clearCanvas() {
        backgroundBitmap = null
        lastPaths = paths.clone() as LinkedHashMap<MyPath, PaintOptions>
        myPath.reset()
        paths.clear()
        invalidate()
    }

    private fun actionDown(x: Float, y: Float) {
        myPath.reset()
        myPath.moveTo(x, y)
        currentX = x
        currentY = y
    }

    private fun actionMove(x: Float, y: Float) {
        myPath.quadTo(currentX, currentY, (x + currentX) / 2, (y + currentY) / 2)
        currentX = x
        currentY = y
    }

    private fun actionUp() {
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


    private fun getPointerCenter(event: MotionEvent): MotionEvent.PointerCoords {
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


    private fun handleScroll(event: MotionEvent): Boolean {
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
            scrollX += (center.x - scrollOriginX) / scale
            scrollY += (center.y - scrollOriginY) / scale
            scrollOriginX = center.x
            scrollOriginY = center.y
            invalidate()
        }
        return isScrolling
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (handleScroll(event)) return true

        val points: FloatArray = floatArrayOf(event.x, event.y)
        transform.mapPoints(points)
        val x = points[0]
        val y = points[1]

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y
                actionDown(x, y)
                undonePaths.clear()
            }

            MotionEvent.ACTION_MOVE -> actionMove(x, y)
            MotionEvent.ACTION_UP -> actionUp()
        }

        invalidate()
        return true
    }
}