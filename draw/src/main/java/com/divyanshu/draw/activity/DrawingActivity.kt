package com.divyanshu.draw.activity

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.divyanshu.draw.databinding.ActivityDrawingBinding
import java.io.ByteArrayOutputStream

class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageCloseDrawing.setOnClickListener {
            finish()
        }
        binding.imageSendDrawing.setOnClickListener {
            val bStream = ByteArrayOutputStream()
            val bitmap = binding.drawView.getBitmap()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream)
            val byteArray = bStream.toByteArray()
            val returnIntent = Intent()
            returnIntent.putExtra("bitmap", byteArray)
            setResult(Activity.RESULT_OK,returnIntent)
            finish()
        }

        setUpDrawTools()

        //colorSelector()

        setPaintAlpha()

        setPaintWidth()
    }

    private fun setUpDrawTools() {
        binding.circleViewOpacity.setCircleRadius(100f)
        binding.imageDrawEraser.setOnClickListener {
            binding.drawView.clearCanvas()
            toggleDrawTools(binding.drawTools,false)
        }
        binding.imageDrawWidth.setOnClickListener {
            if (binding.drawTools.translationY == (56).toPx){
                toggleDrawTools(binding.drawTools,true)
            }else if (binding.drawTools.translationY == (0).toPx && binding.seekBarWidth.visibility == View.VISIBLE){
                toggleDrawTools(binding.drawTools,false)
            }
            binding.circleViewWidth.visibility = View.VISIBLE
            binding.circleViewOpacity.visibility = View.GONE
            binding.seekBarWidth.visibility = View.VISIBLE
            binding.seekBarOpacity.visibility = View.GONE
            binding.drawColorPalette.root.visibility = View.GONE
        }
        binding.imageDrawOpacity.setOnClickListener {
            if (binding.drawTools.translationY == (56).toPx){
                toggleDrawTools(binding.drawTools,true)
            }else if (binding.drawTools.translationY == (0).toPx && binding.seekBarOpacity.visibility == View.VISIBLE){
                toggleDrawTools(binding.drawTools,false)
            }
            binding.circleViewWidth.visibility = View.GONE
            binding.circleViewOpacity.visibility = View.VISIBLE
            binding.seekBarWidth.visibility = View.GONE
            binding.seekBarOpacity.visibility = View.VISIBLE
            binding.drawColorPalette.root.visibility = View.GONE
        }
        binding.imageDrawColor.setOnClickListener {
            if (binding.drawTools.translationY == (56).toPx){
                toggleDrawTools(binding.drawTools,true)
            }else if (binding.drawTools.translationY == (0).toPx && binding.drawColorPalette.root.visibility == View.VISIBLE){
                toggleDrawTools(binding.drawTools,false)
            }
            binding.circleViewWidth.visibility = View.GONE
            binding.circleViewOpacity.visibility = View.GONE
            binding.seekBarWidth.visibility = View.GONE
            binding.seekBarOpacity.visibility = View.GONE
            binding.drawColorPalette.root.visibility = View.VISIBLE
        }
        binding.imageDrawUndo.setOnClickListener {
            binding.drawView.undo()
            toggleDrawTools(binding.drawTools,false)
        }
        binding.imageDrawRedo.setOnClickListener {
            binding.drawView.redo()
            toggleDrawTools(binding.drawTools,false)
        }
    }

    private fun toggleDrawTools(view: View, showView: Boolean = true) {
        if (showView){
            view.animate().translationY((0).toPx)
        }else{
            view.animate().translationY((56).toPx)
        }
    }

/*
    private fun colorSelector() {
        image_color_black.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_black,null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_black)
        }
        image_color_red.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_red,null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_red)
        }
        image_color_yellow.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_yellow,null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_yellow)
        }
        image_color_green.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_green,null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_green)
        }
        image_color_blue.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_blue,null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_blue)
        }
        image_color_pink.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_pink,null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_pink)
        }
        image_color_brown.setOnClickListener {
            val color =  ResourcesCompat.getColor(resources, R.color.color_brown,null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_brown)
        }
    }
*/

/*    private fun scaleColorView(view: View) {
        //reset scale of all views
        image_color_black.scaleX = 1f
        image_color_black.scaleY = 1f

        image_color_red.scaleX = 1f
        image_color_red.scaleY = 1f

        image_color_yellow.scaleX = 1f
        image_color_yellow.scaleY = 1f

        image_color_green.scaleX = 1f
        image_color_green.scaleY = 1f

        image_color_blue.scaleX = 1f
        image_color_blue.scaleY = 1f

        image_color_pink.scaleX = 1f
        image_color_pink.scaleY = 1f

        image_color_brown.scaleX = 1f
        image_color_brown.scaleY = 1f

        //set scale of selected view
        view.scaleX = 1.5f
        view.scaleY = 1.5f
    }*/

    private fun setPaintWidth() {
        binding.seekBarWidth.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.drawView.setStrokeWidth(progress.toFloat())
                binding.circleViewWidth.setCircleRadius(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setPaintAlpha() {
        binding.seekBarOpacity.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.drawView.setAlpha(progress)
                binding.circleViewOpacity.setAlpha(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private val Int.toPx: Float
        get() = (this * Resources.getSystem().displayMetrics.density)
}
