package com.ynsuper.slideshowver1.view.menu

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.ynsuper.slideshowver1.R
import com.ynsuper.slideshowver1.base.BaseCustomConstraintLayout
import com.ynsuper.slideshowver1.callback.TopBarController
import kotlinx.android.synthetic.main.item_layout_edit_top_view.view.*
import kotlinx.android.synthetic.main.layout_template_view.view.*

class TransitionViewLayout : BaseCustomConstraintLayout {

    private lateinit var topBarController: TopBarController

    constructor(context: Context?) : super(context) {
        init(context)
        setLayoutInflate(R.layout.layout_template_view)

        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!,
        attrs
    ) {
        init(context)
        setLayoutInflate(R.layout.layout_template_view)
        initView()

    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context!!, attrs, defStyleAttr) {
        init(context)
        setLayoutInflate(R.layout.layout_template_view)
        initView()

    }

    fun setTopBarName(name: String){
        text_name_top_bar.text = name
    }
    private fun initView() {
        setTopBarName(context.getString(R.string.text_transition))
        image_submit_menu.setOnClickListener {
            topBarController.clickSubmitTopBar()
        }
    }

    fun setTopbarController(topbarController: TopBarController) {
        this.topBarController = topbarController
    }

    fun getRecycleViewTransitions(): RecyclerView {
        return recyclerViewTransitions
    }
}