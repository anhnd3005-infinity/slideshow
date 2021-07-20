package com.ynsuper.slideshowver1.base

import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.ynsuper.slideshowver1.view.menu.DurationViewLayout

open class BaseActivity : AppCompatActivity() {
    protected  inline  fun <reified T: ViewDataBinding> binding(@LayoutRes
                                                                resID:Int) : Lazy<T> = lazy {
        DataBindingUtil.setContentView<T>(this,resID)
    }

    open fun onStateChange(state: DurationViewLayout.OptionState) {}
}
