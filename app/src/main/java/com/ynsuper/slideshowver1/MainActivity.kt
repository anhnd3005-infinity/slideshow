package com.ynsuper.slideshowver1

import android.os.Bundle
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProviders
import com.ynsuper.slideshowver1.base.BaseActivity
import com.ynsuper.slideshowver1.databinding.ActivityMainBinding
import com.ynsuper.slideshowver1.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {

    private lateinit var buttonStart: LinearLayout
    private lateinit var buttonMyVideo: LinearLayout
    private val binding by binding<ActivityMainBinding>(R.layout.activity_main)
    private var viewModel : MainViewModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.apply {
            lifecycleOwner = this@MainActivity
            viewModel = ViewModelProviders.of(this@MainActivity).get(MainViewModel::class.java)
            binding.mainViewModel = viewModel
            viewModel?.setBinding(binding)
        }
        viewModel?.setMainActivity(this)
        viewModel!!.checkRuntimePermission()
        buttonStart = findViewById(R.id.linearCreateVideo)
        buttonMyVideo = findViewById(R.id.linear_my_video)
        buttonStart.setOnClickListener {
            viewModel?.startImagePicker()
        }
        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> viewModel?.selectNavigatorHome()!!
                R.id.navigation_my_video -> viewModel?.selectNavigatorMyVideo()!!
                R.id.navigation_pro -> viewModel?.selectNavigatorPro()!!
                else -> { false}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel?.loadDataVideoDraft()
    }

}