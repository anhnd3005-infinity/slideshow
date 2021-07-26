package com.ynsuper.slideshowver1.view.menu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.SeekBar
import androidx.annotation.Px
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.ynsuper.slideshowver1.R
import com.ynsuper.slideshowver1.base.BaseCustomConstraintLayout
import com.ynsuper.slideshowver1.callback.TopBarController
import com.ynsuper.slideshowver1.view.FontLoader
import com.ynsuper.slideshowver1.view.SlideShowActivity
import com.ynsuper.slideshowver1.view.adapter.FontFamilyAdapter
import com.ynsuper.slideshowver1.view.sticker.QuoteState
import com.ynsuper.slideshowver1.view.sticker.StickerView
import com.ynsuper.slideshowver1.viewmodel.SlideShowViewModel
import kotlinx.android.synthetic.main.fragment_duration_option.view.*
import kotlinx.android.synthetic.main.item_layout_edit_top_view.view.*
import kotlinx.android.synthetic.main.layout_text_quote.view.*
import java.lang.NullPointerException
import kotlin.math.roundToInt

class TextQuoteViewLayout : BaseCustomConstraintLayout {
    private lateinit var preview: StickerView
    private lateinit var topBarController: TopBarController
    private var listener: QuoteListener? = null

    private var state: QuoteState? = null


    constructor(context: Context?) : super(context) {
        init(context)
        setLayoutInflate(R.layout.layout_text_quote)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){
        init(context)
        setLayoutInflate(R.layout.layout_text_quote)
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        init(context)
        setLayoutInflate(R.layout.layout_text_quote)
    }


    fun setTopBarName(name: String){
        text_name_top_bar.text = name
    }
    private var bgColor = Color.WHITE

    private var currentFont: FontLoader.FontFamily? = null


    fun setState(state : QuoteState, preview : StickerView){
        this.state = state
        this.preview = preview
        initView()
    }

    private fun initView() {
        listener = context as SlideShowActivity
        setTopBarName(context.getString(R.string.text_quote))

        val fontLoader = FontLoader(context.assets)
        val fonts = fontLoader.getFonts()

        val adapter = FontFamilyAdapter(context, null, fonts)

        val editTextFilledExposedDropdown = findViewById<AutoCompleteTextView>(R.id.fontFamily)
        editTextFilledExposedDropdown.setAdapter(adapter)
        editTextFilledExposedDropdown.isEnabled = true
        editTextFilledExposedDropdown.setOnItemClickListener { adapterView, view, i, l ->
            currentFont = fonts[i]
            preview.setTypeface(currentFont!!.getTypeface(context.assets))
        }

        colorPicker.setOnClickListener {
            showColorPicker()
        }
        image_submit_menu.setOnClickListener {
            saveBitmap()
            topBarController.clickSubmitTopBar()
        }

        editText.addTextChangedListener {
            preview.setText(it.toString())
        }

        val defaultSize = 12f.dip()

//        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
//                val size = defaultSize + p1.dipF()
//                preview.setTextSize(size)
//            }
//
//            override fun onStartTrackingTouch(p0: SeekBar?) {}
//            override fun onStopTrackingTouch(p0: SeekBar?) {}
//        })


        state?.applyTo(context.assets, preview)

        state?.textColor?.let {
            colorPicker.setBackgroundColor(it)
        }

        state?.text.let {
            editText.setText(it)
        }

        state?.fontFamily?.let {
            currentFont = it
        }
    }

     fun saveBitmap() {
        listener?.onReceiveQuoteBitmap(preview.getBitmap())
        listener?.newQuoteState(QuoteState.from(preview, currentFont))
    }



    private fun toggleBg() {
        bgColor = if (bgColor == Color.WHITE) Color.DKGRAY
        else Color.WHITE
        preview.setBackgroundColor(bgColor)
    }


    private fun Float.px(): Float {
        return this / resources.displayMetrics.density
    }


    @Px
    private fun Int.dip(): Int {
        return dipF().roundToInt()
    }

    @Px
    private fun Int.dipF(): Float {
        return toFloat().dip()
    }

    @Px
    private fun Float.dip(): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            resources.displayMetrics
        )
    }

    private fun showColorPicker() {
        ColorPickerDialog.Builder(context)
            .setTitle("Color Picker")
            .setPreferenceName("pref-color")
            .setPositiveButton("Confirm", object : ColorEnvelopeListener {
                override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                    if (envelope == null) return
                    colorPicker.setBackgroundColor(envelope.color)
                    preview.setTextColor(envelope.color)
                }
            })
            .setNegativeButton("Cancel") { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            .attachAlphaSlideBar(true)
            .attachBrightnessSlideBar(true)
            .show()
    }

    fun setTopbarController(topBarController: TopBarController) {
        this.topBarController = topBarController

    }


    interface QuoteListener {
        fun onReceiveQuoteBitmap(bitmap: Bitmap)
        fun newQuoteState(quoteState: QuoteState)
    }
}