package com.ynsuper.slideshowver1.view.menu

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.annotation.FloatRange
import com.ynsuper.slideshowver1.R
import com.ynsuper.slideshowver1.base.BaseCustomConstraintLayout
import com.ynsuper.slideshowver1.callback.SceneOptionStateListener
import com.ynsuper.slideshowver1.callback.TopBarController
import com.ynsuper.slideshowver1.view.SlideShowActivity
import kotlinx.android.synthetic.main.fragment_duration_option.view.*
import kotlinx.android.synthetic.main.item_layout_edit_top_view.view.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class DurationViewLayout : BaseCustomConstraintLayout {
    private lateinit var topBarController: TopBarController
    private var state: OptionState? = null
    private var listener: SceneOptionStateListener? = null
    private val MAX_DURATION = 20 * 1000L // 10 seconds
    private val MIN_DURATION = 2 * 1000L // 2 seconds

    constructor(context: Context?) : super(context) {
        setLayoutInflate(R.layout.fragment_duration_option)
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setLayoutInflate(R.layout.fragment_duration_option)
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setLayoutInflate(R.layout.fragment_duration_option)
        initView()
    }

    private fun initView() {
        updateDuration()
        setTopBarName(context.getString(R.string.text_duration))
        seekBarDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateDuration()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        image_submit_menu.setOnClickListener {
            topBarController.clickSubmitTopBar()
        }
    }


    fun setTopbarController(topbarController: TopBarController) {
        this.topBarController = topbarController
    }


    private fun saveState() {
        this.state?.let {
            listener?.onDurationChange(this.state!!)
        }

    }

    private fun setState() {
        listener = context as SlideShowActivity

        state?.let { s ->
            seekBarDuration.progress = calcProgress(s.duration)

        }
    }


    private fun updateDuration() {
        val d =
            calculateDuration(seekBarDuration.progress.toFloat() / seekBarDuration.max.toFloat())
        duration.text = formatDuration(d)
        this.state?.duration = d
        saveState()

    }


    private fun formatDuration(millis: Long): String {
        return String.format(
            "%02d sec",

            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(
                    millis
                )
            )
        )
    }

    fun calculateDuration(@FloatRange(from = 0.0, to = 1.0) progress: Float): Long {
        return max((MAX_DURATION * progress).toLong(), MIN_DURATION)
    }

    fun calcProgress(duration: Long): Int {
        val delta = MAX_DURATION - MIN_DURATION
        return ((duration.toFloat() / delta.toFloat()) * 100f).toInt()
    }

    fun setState(state: OptionState) {
        this.state = state
        setState()
        initView()
    }

    fun setTopBarName(name: String) {
        text_name_top_bar.text = name
    }


    data class OptionState(
        var id: String,
        var duration: Long,
        var crop: String? = "fit-center",
        var blur: Boolean = true,
        var delete: Boolean = false
    ) : Parcelable {

        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readLong(),
            parcel.readString(),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(id)
            parcel.writeLong(duration)
            parcel.writeString(crop)
            parcel.writeByte(if (blur) 1 else 0)
            parcel.writeByte(if (delete) 1 else 0)
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