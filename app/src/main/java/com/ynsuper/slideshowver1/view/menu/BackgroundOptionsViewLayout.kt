package com.ynsuper.slideshowver1.view.menu

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.SeekBar

import androidx.annotation.IdRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.ynsuper.slideshowver1.R
import com.ynsuper.slideshowver1.adapter.ColorTextAdapter
import com.ynsuper.slideshowver1.base.BaseCustomConstraintLayout
import com.ynsuper.slideshowver1.callback.SceneOptionStateListener
import com.ynsuper.slideshowver1.callback.TopBarController
import com.ynsuper.slideshowver1.util.Constants
import com.ynsuper.slideshowver1.view.SlideShowActivity
import kotlinx.android.synthetic.main.fragment_background_option.view.*
import kotlinx.android.synthetic.main.fragment_duration_option.view.*
import kotlinx.android.synthetic.main.item_layout_edit_top_view.view.*

class BackgroundOptionsViewLayout : BaseCustomConstraintLayout {
    private lateinit var colorTextAdapter: ColorTextAdapter
    private lateinit var topBarController: TopBarController
    private var state: OptionState? = null
    private var listener: SceneOptionStateListener? = null

    constructor(context: Context?) : super(context) {
        init(context)
        setLayoutInflate(R.layout.fragment_background_option)
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        init(context)
        setLayoutInflate(R.layout.fragment_background_option)
        initView()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context)
        setLayoutInflate(R.layout.fragment_background_option)
        initView()
    }


    private fun initView() {
        listener = context as SlideShowActivity
        setTopBarName(context.getString(R.string.text_background))
        setState()
        setColorTextAdapter()

        checkboxBlur.setOnCheckedChangeListener { buttonView, isChecked ->
            saveState()
        }
        seekBarBlur.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                state?.progressBlur = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
        groupfill.setOnCheckedChangeListener { group, checkedId ->
            saveState()
        }
        image_submit_menu.setOnClickListener {
//            saveState()
            topBarController.clickSubmitTopBar()

        }

    }

    private fun setColorTextAdapter() {
        colorTextAdapter = ColorTextAdapter(
            Constants.getColorText(),
            context
        ) { view, position ->
            state?.color =  resources.getColor(Constants.getColorText().get(position).getIdColor())
            saveState()
        }
        recycleBackgroundColor.setAdapter(colorTextAdapter)
        recycleBackgroundColor.setHasFixedSize(true)
        val linearLayoutManagerColor = LinearLayoutManager(context)
        linearLayoutManagerColor.orientation = LinearLayoutManager.HORIZONTAL
        recycleBackgroundColor.layoutManager = linearLayoutManagerColor
    }


    private fun saveState() {
        this.state?.let {
            listener?.onBackGroundConfigChange(
                it.copy(
                    blur = checkboxBlur.isChecked,
                    crop = currentCrop()
                )
            )
        }
    }

    private fun currentCrop(): String {
        if (fitCenter.isChecked) return "fit-center"
        if (fitEnd.isChecked) return "fit-end"
        if (fitStart.isChecked) return "fit-start"
        if (fillCenter.isChecked) return "fill-center"
        if (fillEnd.isChecked) return "fill-end"
        if (fillStart.isChecked) return "fill-start"
        return "fit-center"
    }

    private fun setState() {
        state?.let { s ->
            groupfill.check(getCheckedId(s.crop!!))

        }
    }

    @IdRes
    private fun getCheckedId(key: String): Int {
        return when (key) {
            "fit-center" -> R.id.fitCenter
            "fit-end" -> R.id.fitEnd
            "fit-start" -> R.id.fitStart
            "fill-center" -> R.id.fillCenter
            "fill-end" -> R.id.fillEnd
            "fill-start" -> R.id.fillStart
            else -> R.id.fitCenter
        }
    }

    fun setTopbarController(topbarController: TopBarController) {
        this.topBarController = topbarController
    }

    fun setState(state: OptionState) {
        this.state = state
        setState()
    }
    fun setTopBarName(name: String){
        text_name_top_bar.text = name
    }


    data class OptionState(
        var id: String,
        var progressBlur: Int,
        var crop: String? = "fit-center",
        var blur: Boolean = true,
        var delete: Boolean = false,
        var color: Int = 0
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readInt(),
            parcel.readString(),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte(),
            parcel.readInt()
        ) {
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(id)
            parcel.writeInt(progressBlur)
            parcel.writeString(crop)
            parcel.writeByte(if (blur) 1 else 0)
            parcel.writeByte(if (delete) 1 else 0)
            parcel.writeInt(color)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<OptionState> {
            override fun createFromParcel(parcel: Parcel): OptionState {
                return OptionState(parcel)
            }

            override fun newArray(size: Int): Array<OptionState?> {
                return arrayOfNulls(size)
            }
        }


    }
}