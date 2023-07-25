package com.divyanshu.draw.widget

import android.content.Context
import android.graphics.Bitmap
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


class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var mPaths = LinkedHashMap<MyPath, PaintOptions>()

    private var mLastPaths = LinkedHashMap<MyPath, PaintOptions>()
    private var mUndonePaths = LinkedHashMap<MyPath, PaintOptions>()

    private var mPaint = Paint()
    private var mPath = MyPath()
    private var mPaintOptions = PaintOptions()

    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mIsSaving = false
    private var mIsStrokeWidthBarEnabled = false
    private var mTransform = Matrix()

    private var mIsScrolling = false
    private var mScale = 1f
    private var mScrollOriginX = 0f
    private var mScrollOriginY = 0f
    private var mScrollX = 0f
    private var mScrollY = 0f
    private var mScaleGestureDetector: ScaleGestureDetector

    private var mBackground: Bitmap? = null
    private var mBackgroundRect = RectF()

    var tempRect = Rect()

    init {
        mPaint.apply {
            color = mPaintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = mPaintOptions.strokeWidth
            isAntiAlias = true
        }

        mScaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val oldScale = mScale
                    mScale *= detector.scaleFactor
                    mScale = Math.min(Math.max(mScale, 0.5f), 3.0f)
                    mScrollX += detector.focusX * (oldScale - mScale) / mScale
                    mScrollY += detector.focusY * (oldScale - mScale) / mScale
                    invalidate()
                    return true
                }
            })
    }

    fun reset() {
        mUndonePaths.clear()
        mPaths.clear()
        mLastPaths.clear()
        clearCanvas()
    }

    fun undo() {
        if (mPaths.isEmpty() && mLastPaths.isNotEmpty()) {
            mPaths = mLastPaths.clone() as LinkedHashMap<MyPath, PaintOptions>
            mLastPaths.clear()
            invalidate()
            return
        }
        if (mPaths.isEmpty()) return

        val lastPath = mPaths.values.lastOrNull()
        val lastKey = mPaths.keys.lastOrNull()

        mPaths.remove(lastKey)
        if (lastPath != null && lastKey != null) {
            mUndonePaths[lastKey] = lastPath
        }
        invalidate()
    }

    fun redo() {
        if (mUndonePaths.keys.isEmpty()) return
        val lastKey = mUndonePaths.keys.last()
        addPath(lastKey, mUndonePaths.values.last())
        mUndonePaths.remove(lastKey)
        invalidate()
    }

    fun setColor(newColor: Int) {
        Log.d("alpha", mPaintOptions.alpha.toString())
        @ColorInt
        val alphaColor = ColorUtils.setAlphaComponent(newColor, mPaintOptions.alpha)
        mPaintOptions.color = alphaColor
        if (mIsStrokeWidthBarEnabled) {
            invalidate()
        }
    }


    fun setAlpha(newAlpha: Int) {
        mPaintOptions.alpha = newAlpha
        setColor(mPaintOptions.color)
    }

    fun setStrokeWidth(newStrokeWidth: Float) {
        mPaintOptions.strokeWidth = newStrokeWidth
        if (mIsStrokeWidthBarEnabled) {
            invalidate()
        }
    }


    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        mIsSaving = true
        draw(canvas)
        mIsSaving = false
        return bitmap
    }


    private fun addPath(path: MyPath, options: PaintOptions) {
        mPaths[path] = options
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Store current clip bounds in tempRect.
        canvas.getClipBounds(tempRect)

        mTransform.setTranslate(mScrollX + tempRect.centerX(), mScrollY + tempRect.centerY())
        mTransform.postScale(mScale, mScale)

        val bg = mBackground
        if (bg != null) {
            mBackgroundRect.left = -bg.width.toFloat() / 2
            mBackgroundRect.right = bg.width.toFloat() / 2
            mBackgroundRect.top = -bg.height.toFloat() / 2
            mBackgroundRect.bottom = bg.height.toFloat() / 2
            if (bg.height == 1 && bg.width == 1) {
                mBackgroundRect.set(tempRect)
            } else {
                mTransform.mapRect(mBackgroundRect)
            }

            mBackground?.let {
                canvas.drawBitmap(it, null, mBackgroundRect, null)
            }
        }

        canvas.setMatrix(mTransform)
        mTransform.invert(mTransform)

        for ((key, value) in mPaths) {
            changePaint(value)
            canvas.drawPath(key, mPaint)
        }

        changePaint(mPaintOptions)
        canvas.drawPath(mPath, mPaint)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
    }

    fun clearCanvas() {
        mBackground = null
        mLastPaths = mPaths.clone() as LinkedHashMap<MyPath, PaintOptions>
        mPath.reset()
        mPaths.clear()
        invalidate()
    }

    private fun actionDown(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mCurX = x
        mCurY = y
    }

    private fun actionMove(x: Float, y: Float) {
        mPath.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2)
        mCurX = x
        mCurY = y
    }

    private fun actionUp() {
        mPath.lineTo(mCurX, mCurY)

        // draw a dot on click
        if (mStartX == mCurX && mStartY == mCurY) {
            mPath.lineTo(mCurX, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY)
        }

        mPaths[mPath] = mPaintOptions
        mPath = MyPath()
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth, mPaintOptions.alpha)
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
        if (shouldScroll != mIsScrolling) {
            mUndonePaths.clear()
            mPath.reset()
            if (shouldScroll) {
                mIsScrolling = true
                mScrollOriginX = center.x
                mScrollOriginY = center.y
            } else if (event.action == MotionEvent.ACTION_UP) {
                mIsScrolling = false
            }
            return true
        }

        if (shouldScroll) {
            mScrollX += (center.x - mScrollOriginX) / mScale
            mScrollY += (center.y - mScrollOriginY) / mScale
            mScrollOriginX = center.x
            mScrollOriginY = center.y
            invalidate()
        }
        return mIsScrolling
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleGestureDetector.onTouchEvent(event)
        if (handleScroll(event)) return true

        val points: FloatArray = floatArrayOf(event.x, event.y)
        mTransform.mapPoints(points)
        val x = points[0]
        val y = points[1]

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = x
                mStartY = y
                actionDown(x, y)
                mUndonePaths.clear()
            }

            MotionEvent.ACTION_MOVE -> actionMove(x, y)
            MotionEvent.ACTION_UP -> actionUp()
        }

        invalidate()
        return true
    }
}