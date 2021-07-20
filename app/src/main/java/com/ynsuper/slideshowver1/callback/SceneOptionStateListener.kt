package com.ynsuper.slideshowver1.callback

import com.ynsuper.slideshowver1.view.menu.BackgroundOptionsViewLayout
import com.ynsuper.slideshowver1.view.menu.DurationViewLayout

interface SceneOptionStateListener {
    fun onDurationChange(state: DurationViewLayout.OptionState)

    fun onBackGroundConfigChange(state: BackgroundOptionsViewLayout.OptionState)
}