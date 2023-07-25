package com.example.painter

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.divyanshu.draw.widget.CircleView
import com.divyanshu.draw.widget.DrawView
import com.example.painter.databinding.ActivityMainBinding
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import java.io.File
import java.util.UUID

class MainActivity : AppCompatActivity(), View.OnClickListener, ColorPickerDialogListener {

    private lateinit var binding: ActivityMainBinding

    private var drawView: DrawView? = null
    private var colorSelect: ImageButton? = null
    private var drawBtn: ImageButton? = null
    private var eraseBtn: ImageButton? = null
    private var newBtn: ImageButton? = null
    private var saveBtn: ImageButton? = null
    private var smallBrush = 0f
    private var mediumBrush = 0f
    private var largeBrush = 0f
    var currentColor = 0
    private var toolbar: View? = null
    private var preview: CircleView? = null
    private var seekBarWidth: SeekBar? = null
    var eraser = false
    var toolbarOpen = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageDrawColor.setOnClickListener(this)
        currentColor = Color.BLACK
        smallBrush = resources.getInteger(R.integer.smallSize).toFloat()
        mediumBrush = resources.getInteger(R.integer.mediumSize).toFloat()
        largeBrush = resources.getInteger(R.integer.largeSize).toFloat()
        binding.imageDrawWidth.setOnClickListener(this)
        binding.drawing.setStrokeWidth(15f)
        binding.imageDrawEraser.setOnClickListener(this)
        eraser = false
        binding.newBtn.setOnClickListener(this)
        binding.saveBtn.setOnClickListener(this)
        binding.imageDrawRedo.setOnClickListener { binding.drawing.redo() }
        binding.imageDrawUndo.setOnClickListener { binding.drawing.undo() }
        binding.seekBarWidth.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.drawing.setStrokeWidth(progress.toFloat())
                binding.circleViewPreview.setCircleRadius(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 112)
        }
    }

    override fun onClick(view: View) {
        if (view.id == R.id.image_draw_width) {
            toggleToolbar()
        } else if (view.id == R.id.image_draw_brush) {
            drawView!!.setColor(currentColor)
            preview!!.setColor(currentColor)
        } else if (view.id == R.id.image_draw_eraser) {
            drawView!!.setColor(Color.WHITE)
            preview!!.setColor(Color.WHITE)
        } else if (view.id == R.id.image_draw_color) {
            ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setAllowPresets(false)
                .setDialogId(DIALOG_ID)
                .setColor(currentColor)
                .setShowAlphaSlider(true)
                .show(this)
        } else if (view.id == R.id.newBtn) {
            val newDialog = AlertDialog.Builder(this)
            newDialog.setTitle("New Drawing")
            newDialog.setMessage("Start a new drawing? (This will erase your current drawing.)")
            newDialog.setPositiveButton("Yes") { dialog, which ->
                drawView!!.reset()
                dialog.dismiss()
            }
            newDialog.setNegativeButton("No") { dialog, which -> dialog.cancel() }
            newDialog.show()
        } else if (view.id == R.id.saveBtn) {
            val saveDialog = AlertDialog.Builder(this)
            saveDialog.setTitle("Save Drawing")
            saveDialog.setMessage("Save drawing to device gallery?")
            saveDialog.setPositiveButton("Yes") { dialog, which -> saveDrawing() }
            saveDialog.setNegativeButton("No") { dialog, which -> dialog.cancel() }
            saveDialog.show()
        }
    }

    private fun toggleToolbar() {
        toolbar!!.animate().translationY((if (toolbarOpen) 56 else 0) * resources.displayMetrics.density)
        toolbarOpen = !toolbarOpen
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        Log.d("Color", "onColorSelected() called with: dialogId = [$dialogId], color = [$color]")
        when (dialogId) {
            DIALOG_ID -> {
                // We got result from the dialog that is shown when clicking on the icon in the action bar.
                currentColor = color
                drawView!!.setColor(color)
                val alpha = color shr 24 and 0x000000FF
                Log.d("Alpha", "$color $alpha")
                drawView!!.setAlpha(alpha)
            }
        }
    }

    override fun onDialogDismissed(dialogId: Int) {}


    private fun saveDrawing() {
        fixMediaDir()
        if (checkPermissionREAD_EXTERNAL_STORAGE(applicationContext)) {
            val bmp = drawView!!.getBitmap()
            val imgSaved = MediaStore.Images.Media.insertImage(contentResolver, bmp, UUID.randomUUID().toString() + ".png", "drawing")
            if (imgSaved != null) {
                Toast.makeText(applicationContext.applicationContext, "Drawing saved to Gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext.applicationContext, "Image could not saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkPermissionREAD_EXTERNAL_STORAGE(context: Context?): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        return if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context!!,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (context as Activity?)!!,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                ) {
                    showDialog("External storage", context,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    ActivityCompat
                        .requestPermissions(
                            (context as Activity?)!!, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
                }
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    fun showDialog(msg: String, context: Context?,
                   permission: String) {
        val alertBuilder = AlertDialog.Builder(context)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission necessary")
        alertBuilder.setMessage("$msg permission is necessary")
        alertBuilder.setPositiveButton(android.R.string.yes
        ) { dialog, which ->
            ActivityCompat.requestPermissions((context as Activity?)!!, arrayOf(permission),
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        }
        val alert = alertBuilder.create()
        alert.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do your stuff
            }

            else -> {}
        }
    }

    fun fixMediaDir() {
        val sdcard = Environment.getExternalStorageDirectory()
        if (sdcard != null) {
            val mediaDir = File(sdcard, "DCIM/Camera")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
        }
    }

    companion object {
        const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123
        private const val DIALOG_ID = 0
    }
}