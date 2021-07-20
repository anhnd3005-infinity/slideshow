package com.ynsuper.slideshowver1.view

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProviders
import com.ynsuper.slideshowver1.R
import com.ynsuper.slideshowver1.adapter.MusicAdapter
import com.ynsuper.slideshowver1.adapter.SoundManager
import com.ynsuper.slideshowver1.base.BaseActivity
import com.ynsuper.slideshowver1.callback.SaveStateListener
import com.ynsuper.slideshowver1.callback.SceneOptionStateListener
import com.ynsuper.slideshowver1.databinding.ActivitySlideshowBinding
import com.ynsuper.slideshowver1.model.ImageModel
import com.ynsuper.slideshowver1.util.Constant
import com.ynsuper.slideshowver1.util.entity.AudioEntity
import com.ynsuper.slideshowver1.view.menu.BackgroundOptionsViewLayout
import com.ynsuper.slideshowver1.view.menu.DurationViewLayout
import com.ynsuper.slideshowver1.view.menu.MusicViewLayout
import com.ynsuper.slideshowver1.viewmodel.SlideShowViewModel
import kotlinx.android.synthetic.main.activity_slideshow.*
import kotlinx.android.synthetic.main.layout_menu_bar.*
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*

class SlideShowActivity : BaseActivity(), SceneOptionStateListener,
    MusicAdapter.OnSongClickListener, MusicViewLayout.OnSelectedSongListener, SaveStateListener {
    private val binding by binding<ActivitySlideshowBinding>(R.layout.activity_slideshow)
    private lateinit var viewModel: SlideShowViewModel
    private var mViews: ArrayList<View>? = null
    private var mCurrentView: StickerView? = null
    private var mContentRootView: ConstraintLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initEvent()
    }

    private fun initView() {
        binding.apply {
            lifecycleOwner = this@SlideShowActivity
            viewModel =
                ViewModelProviders.of(this@SlideShowActivity).get(SlideShowViewModel::class.java)
            binding.slideShowViewModel = viewModel
            viewModel.setBinding(binding)
            viewModel.initDataBase(this@SlideShowActivity)
            viewModel.loadDataImage(intent.getParcelableArrayListExtra<ImageModel>(Constant.EXTRA_ARRAY_IMAGE))
            viewModel.loadDataMusic()

            mContentRootView = binding.rlContentRoot
            mViews = ArrayList()

        }
    }

    override fun onSelectedSong(songName: String) {
        super.onSelectedSong(songName)
        Log.e(Constant.YNSUPER_TAG, "onSelectedSong: $songName")

        viewModel.selectMusicSong(songName)
    }

    private fun initEvent() {
        layout_menu_ratio.setOnClickListener { viewModel.selectMenuRatio() }
        layout_menu_background.setOnClickListener { viewModel.selectMenuBackground() }
        layout__menu_transition.setOnClickListener { viewModel.selectMenuTransition() }
        layout_menu_music.setOnClickListener { viewModel.selectMenuMusic() }
        layout_menu_duration.setOnClickListener { viewModel.selectMenuSpeed() }
        layout_menu_sticker.setOnClickListener { viewModel.selectMenuSticker() }
        layout_menu_text.setOnClickListener { viewModel.selectMenuText() }
        image_add_image.setOnClickListener { viewModel.showAddImageSheet() }
        image_save_draft.setOnClickListener { viewModel.saveDraft() }
        image_save_video.setOnClickListener { viewModel.saveVideo() }
        image_back.setOnClickListener { finish() }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val audioFile = data?.data ?: return

            val inputStream = contentResolver.openInputStream(audioFile) ?: return
            val cursor = contentResolver.query(audioFile, null, null, null, null)
            val nameColumn = cursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name = cursor.getString(nameColumn)

            val outputFile = File(externalCacheDir, "audio-${UUID.randomUUID()}" + name)
            val fileOutputStream = FileOutputStream(outputFile)
            IOUtils.copy(inputStream, fileOutputStream)
            inputStream.close()
            fileOutputStream.flush()
            fileOutputStream.close()
            cursor.close()

            val audio = AudioEntity(path = outputFile.path)
            viewModel.setAudio(audio)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.liveDataCategory.observe(this, viewModel.changeCategoryMusicList)
    }


    override fun onDurationChange(state: DurationViewLayout.OptionState) {
        viewModel.onDurationChange(state)
    }

    fun reloadSlideBar() {
        viewModel.reloadSlideBar()
    }

    fun addMoreScene() {
        viewModel.addMoreScene()
    }

    override fun onBackGroundConfigChange(state: BackgroundOptionsViewLayout.OptionState) {
        viewModel.onBackgroundConfigChage(state)
    }

    fun changeCloseImageToBack() {
        viewModel.changeImageCloseToBack()
    }

    fun changeBackToCloseImage() {
        viewModel.changeBackToCloseImage()

    }

    override fun onPause() {
        super.onPause()
        SoundManager.getInstance(this).stopSound()
    }

    fun addSticker(bitmap: Bitmap?) {
        viewModel?.addStickerInVideo(bitmap)
    }

    override fun onExportVideo(width: Int, height: Int) {
        viewModel?.exportVideo(width, height)
    }

    fun changeRatioView(width: Int, height: Int) {
        viewModel?.changeRatioPreview(width, height)
    }


//    override fun onSongDownloadClick(
//        url: String?,
//        name: String?,
//        progressBar: ProgressBar?,
//        imgDownUse: ImageView?,
//        isInternetMusic: Boolean
//    ) {
//        Log.e(Constant.NDPHH_TAG, "onDownloadClick Slikeshow")
//    }
//
//    override fun onSongClick(musicModel: MusicModel?) {
//        super.onSongClick(musicModel)
//        viewModel.songClick(musicModel)
//    }
}