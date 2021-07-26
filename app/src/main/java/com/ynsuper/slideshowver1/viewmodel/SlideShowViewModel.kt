package com.ynsuper.slideshowver1.viewmodel

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.util.Size
import android.util.SparseArray
import android.view.TextureView
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.set
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.room.Room
import com.seanghay.studio.gles.transition.Transition
import com.seanghay.studio.gles.transition.TransitionStore
import com.seanghay.studio.utils.BitmapProcessor
import com.ynsuper.slideshowver1.R
import com.ynsuper.slideshowver1.adapter.SoundManager
import com.ynsuper.slideshowver1.base.BaseViewModel
import com.ynsuper.slideshowver1.bottomsheet.AddImageGroupBottomSheet
import com.ynsuper.slideshowver1.bottomsheet.SaveVideoBottomSheet
import com.ynsuper.slideshowver1.bottomsheet.StickerBottomSheet
import com.ynsuper.slideshowver1.callback.*
import com.ynsuper.slideshowver1.database.AppDatabase
import com.ynsuper.slideshowver1.databinding.ActivitySlideshowBinding
import com.ynsuper.slideshowver1.model.AlbumMusicModel
import com.ynsuper.slideshowver1.model.ImageModel
import com.ynsuper.slideshowver1.model.ListSticker
import com.ynsuper.slideshowver1.util.*
import com.ynsuper.slideshowver1.util.editvideo.BitmapUtils
import com.ynsuper.slideshowver1.util.entity.AudioEntity
import com.ynsuper.slideshowver1.util.entity.SlideEntity
import com.ynsuper.slideshowver1.util.entity.StoryEntity
import com.ynsuper.slideshowver1.view.LittleBox
import com.ynsuper.slideshowver1.view.SlideShowActivity
import com.ynsuper.slideshowver1.view.StickerView
import com.ynsuper.slideshowver1.view.adapter.SlideAdapter
import com.ynsuper.slideshowver1.view.adapter.TransitionsAdapter
import com.ynsuper.slideshowver1.view.custom_view.HorizontalThumbnailListView
import com.ynsuper.slideshowver1.view.menu.*
import com.ynsuper.slideshowver1.view.menu.BackgroundOptionsViewLayout.OptionState
import com.ynsuper.slideshowver1.view.sticker.QuoteState
import gun0912.tedimagepicker.builder.TedImagePicker
import id.zelory.compressor.Compressor
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class SlideShowViewModel : BaseViewModel(), TopBarController, IHorizontalListChange {


    private var lastIndex: Int = 0
    private lateinit var onSelectedSongListener: MusicViewLayout.OnSelectedSongListener
    private var arrListSticker: ListSticker? = null
    private var totalDuration: Long = 0
    private var isPlay = false
    private var startScroll: Int = 0
    private var timer: Timer? = null
    private lateinit var textQuoteViewLayout: TextQuoteViewLayout
    private lateinit var durationViewLayout: DurationViewLayout
    private lateinit var musicViewLayout: MusicViewLayout
    private lateinit var ratioViewLayout: RatioViewLayout
    private lateinit var transitionViewLayout: TransitionViewLayout
    private lateinit var backgroundViewLayout: BackgroundOptionsViewLayout
    private var audio: AudioEntity? = null
    private lateinit var allTransition: List<Transition>
    private lateinit var binding: ActivitySlideshowBinding
    private lateinit var appDatabase: AppDatabase
    private lateinit var compressor: Compressor
    private lateinit var videoComposer: VideoComposer
    private lateinit var context: SlideShowActivity
    private val isLoading = MutableLiveData<Boolean>()
    private val isExport = MutableLiveData<Boolean>()
    private val transitionAdapter: TransitionsAdapter = TransitionsAdapter(arrayListOf())
    private val slides = arrayListOf<SlideEntity>()
    private val slideAdapter: SlideAdapter = SlideAdapter(slides)
    private var littleBox: LittleBox? = null
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var mCurClipIndex = 0
    private val quoteStatePool = SparseArray<QuoteState>()
    private lateinit var quoteState: QuoteState

    var liveDataCategory = MutableLiveData<AlbumMusicModel>()
    val changeCategoryMusicList = Observer<AlbumMusicModel> { values ->
        values?.let {
            musicViewLayout.setCategoryListModel(it)
        }
    }

    fun initDataBase(context: SlideShowActivity) {
        this.context = context
        videoComposer = VideoComposer(context)
        // update next version 3.0
        compressor = Compressor(context)

        appDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "slideshow-v1")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
//        setupStatusBar(Color.parseColor("#80FFFFFF"))
        isLoading.value = true
        binding.lifecycleOwner?.let {
            isLoading.observe(it, Observer {
                binding.loadingLayout.visibility = if (it) View.VISIBLE else View.GONE
            })
        }
        binding.lifecycleOwner?.let {
            isExport.observe(it, Observer {
                binding.loadingRender.visibility = if (it) View.VISIBLE else View.GONE
            })
        }
        // initMenuBar first
        initViewMenuBar()
        initTextQuote()
        initToolbar()
        initTransitions()
        initPhotos()
        launch()
        initDurations()
        if (Util.isInternetAvailable(context)) {
            loadDataSticker()
        }
        setEvents()
    }

    private fun initViewMenuBar() {
        backgroundViewLayout = context.findViewById(R.id.background_view_layout)
        backgroundViewLayout.setTopbarController(this)

        ratioViewLayout = context.findViewById(R.id.ratio_view_layout)
        ratioViewLayout.setTopbarController(this)

        transitionViewLayout = context.findViewById(R.id.transition_view_layout)
        transitionViewLayout.setTopbarController(this)

        musicViewLayout = context.findViewById(R.id.music_view_layout)
        musicViewLayout.setTopbarController(this)

        durationViewLayout = context.findViewById(R.id.duration_view_layout)
        durationViewLayout.setTopbarController(this)

        textQuoteViewLayout = context.findViewById(R.id.text_quote_view_layout)
        textQuoteViewLayout.setTopbarController(this)
    }


    private fun setEvents() {
        binding.horizontalImageSlide.setHorizontalListChange(this)
        binding.imageButtonControl.setOnClickListener {
            isPlay = !isPlay
            if (!isPlay) {
                binding.imageButtonControl.setImageResource(R.drawable.ic_icon_awesome_play)
                SoundManager.getInstance(context).getmMediaPlayer().pause()
//                littleBox?.pause()
            } else {
//                littleBox?.play()
                binding.imageButtonControl.setImageResource(R.drawable.ic_pause)
                SoundManager.getInstance(context).getmMediaPlayer().start()
                runThread()

            }

        }
    }

    fun runThread() {
        if (timer != null) {
            timer!!.cancel()
        }
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                context.runOnUiThread {
                    if (!binding.horizontalImageSlide!!.mIsScrollFromUser) {
                        startScroll = binding.horizontalImageSlide?.getmLastPosX()!!
                    }
                    Log.d(
                        "Thread",
                        binding.horizontalImageSlide!!.isLastImage()
                            .toString() + "  last image " + startScroll + " is play" + binding.horizontalImageSlide.isLastImage()
                    )
                    if (!isPlay) {
                        timer!!.cancel()
                    } else if (isPlay && !binding.horizontalImageSlide.isLastImage()) {
                        binding.horizontalImageSlide.mIsScrollFromUser = true
                        binding.horizontalImageSlide.onScrollChanged(
                            startScroll,
                            startScroll,
                            0,
                            0
                        )
                        startScroll += 1
                    } else {
                        startScroll = 0
                        Log.d("Thread", "start scroll init$startScroll")
                        startScroll = 0
                        binding.horizontalImageSlide.onScrollChanged(
                            startScroll + 0,
                            startScroll,
                            0,
                            0
                        )
                        timer?.cancel()
                    }
                }
            }
        }, 0, 10)
    }

    private fun initPhotos() {
//        if (slides.isEmpty()) {
//            val fromDb = appDatabase.slideDao().getAll()
//            fromDb.filter { !File(it.path).exists() }.forEach(appDatabase.slideDao()::delete)
//            slides.addAll(fromDb.filter { File(it.path).exists() })
//        }

//        binding.recyclerView.let {
//            it.adapter = slideAdapter
//            it.setHasFixedSize(true)
//            (it.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
//        }

//        dispatchUpdates()

        slideAdapter.selectionChange = {
            selectionScene()
        }

        slideAdapter.onLongPress = {
            changeDurationScene()
        }

//        buttonDeselect.setOnClickListener {
//            slideAdapter.deselectAll()
//        }
    }

    private fun changeDurationScene() {
        val sceneIndex = slideAdapter.selectedAt
        if (sceneIndex >= 0) {
            val scene = videoComposer.getScenes()[sceneIndex]

            val state = DurationViewLayout.OptionState(
                id = scene.id,
                duration = scene.duration,
                crop = scene.cropType.key()
            )

            durationViewLayout.visibility = View.VISIBLE
            durationViewLayout.setState(state)
            hideMenuBar()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.text_move_image_to_edit),
                Toast.LENGTH_SHORT
            ).show()
        }


    }

    private fun selectionScene() {
        val sceneIndex = slideAdapter.selectedAt
        quoteState = quoteStatePool[sceneIndex] ?: quoteState

        if (sceneIndex >= 0) {
            val transition = videoComposer.getScenes().get(sceneIndex).transition
            val selectedTransition =
                videoComposer.getTransitions().firstOrNull { it.name == transition.name }

            if (selectedTransition != null) {
                val indexOf = videoComposer.getTransitions().indexOf(selectedTransition)
                transitionAdapter.select(indexOf)
                transitionViewLayout.getRecycleViewTransitions().smoothScrollToPosition(indexOf)
            }
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.text_move_image_to_edit),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun launch() {

        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                littleBox?.resize(width, height)
                Log.d("Ynsuper", "Ynsuper resize: " + width + " height: " + height)

            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                // littleBox?.release()
                return false
            }

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (surface == null) return
                Log.d(
                    "Ynsuper",
                    "Ynsuper onSurfaceTextureAvailable: " + width + " height: " + height
                )
                littleBox = LittleBox(context, surface, width, height)
                littleBox?.setComposer(videoComposer)
                littleBox?.resize(width, height)


//                littleBox?.playProgress = {
//                    binding.seekBarProgress.progress = (it * 100f).toInt()
//                }
                binding.horizontalImageSlide.setLittleBox(littleBox, videoComposer)
//                binding.seekBarProgress.setOnSeekBarChangeListener(object :
//                    SeekBar.OnSeekBarChangeListener {
//                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
////                        textViewProgress.text = HtmlCompat.fromHtml(
////                            "Progress: <strong>$p1%</strong>",
////                            HtmlCompat.FROM_HTML_MODE_LEGACY
////                        )
//
//                        val progress = p1.toFloat() / p0!!.max.toFloat()
//                        if (p2) videoComposer.renderAtProgress(progress)
//                        dispatchDraw()
//                    }
//
//                    override fun onStartTrackingTouch(p0: SeekBar?) {
//                        littleBox?.pause()
//                        binding.imageButtonControl.setImageResource(R.drawable.ic_icon_awesome_play)
//                    }
//
//                    override fun onStopTrackingTouch(p0: SeekBar?) {
//                    }
//                })
            }
        }
    }

    private fun initDurations() {
        videoComposer.duration.observe(binding.lifecycleOwner!!, Observer {
            binding.textViewDuration.setText("" + formatDuration(it))
            totalDuration = it

        })
    }

    private fun formatDuration(millis: Long): String {
        return String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                TimeUnit.MILLISECONDS.toHours(
                    millis
                )
            ),
            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(
                    millis
                )
            )
        )
    }

    private fun initTextQuote() {
        quoteState = QuoteState(
            text = "Hello, World!",
            textColor = Color.BLACK,
            textSize = 18f.dip(context.resources),
            scaleFactor = 1f,
            fontFamily = null
        )
    }

    private fun initTransitions() {
        allTransition = TransitionStore.getAllTransitions()
        val transitions = videoComposer.getTransitions()
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        transitionAdapter.items = transitions

        transitionViewLayout.getRecycleViewTransitions().let {
            it.adapter = transitionAdapter
            it.layoutManager = layoutManager
            it.setHasFixedSize(true)
            (it.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        transitionAdapter.selectionChange = {
            val transition = transitions[transitionAdapter.selectedAt]
            val scene = videoComposer.getScenes().getOrNull(slideAdapter.selectedAt)

            if (scene != null) {
                scene.transition = transition
                transitionViewLayout.getRecycleViewTransitions()
                    .smoothScrollToPosition(transitionAdapter.selectedAt)
            }
            binding.horizontalImageSlide.replaceImageTransition(
                slideAdapter.selectedAt,
                "image_transition/" + transition.imagePreview + ".png"
            )
//            loadHorizonSlideImage()
            dispatchDraw()
        }

        transitionAdapter.onLongPressed = {
            val transition = transitions[transitionAdapter.selectedAt]
            var index = 0
            videoComposer.getScenes().forEach {
                it.transition = transition
                binding.horizontalImageSlide.replaceImageTransition(
                    index,
                    "image_transition/" + transition.imagePreview + ".png"
                )
                index++

            }
            Toast.makeText(context, "Applied transitions for all slides", Toast.LENGTH_SHORT).show()
            dispatchDraw()
        }
    }

    private fun initToolbar() {

    }

    fun setBinding(binding: ActivitySlideshowBinding) {
        this.binding = binding
    }

    private fun dispatchDraw() {
        littleBox?.draw()
    }

    private fun Disposable.willBeDisposed() {
        addTo(compositeDisposable)
    }

    private fun dispatchUpdates() {
        val paths = slides.filter { File(it.path).exists() }

        Single.just(paths)
            .map {
                it.map { item ->
                    item.path to BitmapProcessor.loadSync(
                        item.path
                    )
                }
            }
            .toObservable().switchMap { videoComposer.setScenes(it).toObservable() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                isLoading.postValue(false)
                loadHorizonSlide()
            }
            .doOnSubscribe {
                isLoading.postValue(true)
            }
            .subscribeBy(onError = {
                Toast.makeText(context, "Error while loading slides", Toast.LENGTH_SHORT).show()
            }) {
                Toast.makeText(context, "Slides loaded", Toast.LENGTH_SHORT).show()

                dispatchDraw()
            }.willBeDisposed()
    }

    private fun applyData(items: List<ImageModel>) {
        Single.just(items)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { items ->
                items.map {
                    val inputStream = context.contentResolver.openInputStream(it.uriImage)
                    val file = File.createTempFile("filename", null, context.cacheDir)
                    val fileOutputStream = FileOutputStream(file)
                    IOUtils.copy(inputStream, fileOutputStream)
                    // set chat luong anh
                    val compressedFile = compressor.setQuality(70)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .compressToFile(file, "photos-${UUID.randomUUID()}.jpg")
                    SlideEntity(
                        createdAt = System.currentTimeMillis(),
                        path = compressedFile.path
                    )
                }
            }.subscribeBy {
                slides.addAll(it)
                slideAdapter.notifyDataSetChanged()
                dispatchUpdates()
                dispatchDraw()
            }.willBeDisposed()

    }

    fun setAudio(audioEntity: AudioEntity) {
        this.audio = audioEntity
//        binding.horizontalImageSlide.textMusicSong = SoundManager.getInstance(context).nameAudio
    }

    fun loadDataImage(listImage: ArrayList<ImageModel>?) {
        applyData(listImage!!)
    }

    @SuppressLint("CheckResult")
    fun loadDataSticker() {
        Log.d("Ynsuper", "Ynsuper loadDataSticker:")

        val subscribe = APIService.service.getListStickerModel()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                Toast.makeText(context, "Cannot connect to server", Toast.LENGTH_SHORT).show()

            }
            .doFinally {

            }
            .subscribe(
                { listSticker ->
                    arrListSticker = listSticker
                    Log.d("Ynsuper", "Ynsuper arrListSticker:" + arrListSticker?.arraySticker?.size)
                },
                { throwable ->
                    Log.d("Ynsuper", "Ynsuper error:" + throwable.message)
                }


            )


    }

    fun selectMenuRatio() {
        ratioViewLayout.visibility = View.VISIBLE
        hideMenuBar()
    }

    fun selectMenuBackground() {
        val sceneIndex = slideAdapter.selectedAt
        if (sceneIndex >= 0) {
            val scene = videoComposer.getScenes()[sceneIndex]

            val state = OptionState(
                id = scene.id,
                blur = scene.isBlur,
                color = scene.currentColor,
                progressBlur = scene.progressBlur,
                crop = scene.cropType.key()
            )

            backgroundViewLayout.visibility = View.VISIBLE
            backgroundViewLayout.setState(state)
            hideMenuBar()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.text_move_image_to_edit),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun selectMenuTransition() {
        transitionViewLayout.visibility = View.VISIBLE
        hideMenuBar()
    }


    fun selectMenuMusic() {
        binding.musicViewLayout.visibility = View.VISIBLE
        hideMenuBar()

    }

    fun selectMenuSpeed() {
        changeDurationScene()

    }

    fun selectMenuSticker() {
        StickerBottomSheet
            .newInstance(arrListSticker)
            .show(
                context.supportFragmentManager,
                "scene-options"
            )
    }

    fun selectMenuText() {
//        TextQuoteViewLayout.newInstance(quoteState)
//            .show(context.supportFragmentManager, "quote")

        textQuoteViewLayout.visibility = View.VISIBLE
        binding.preview.visibility = View.VISIBLE
        val previewText = context.findViewById<com.ynsuper.slideshowver1.view.sticker.StickerView>(R.id.preview)
        textQuoteViewLayout.setState(quoteState, previewText)
        hideMenuBar()
    }

    private fun hideMenuBar() {
        binding.scrollMenuOption.visibility = View.VISIBLE
        binding.horizontalMenu.visibility = View.GONE
    }

    private fun showMenuBar() {
        binding.scrollMenuOption.visibility = View.GONE
        binding.horizontalMenu.visibility = View.VISIBLE

    }

    override fun clickCloseTopBar() {
        transitionViewLayout.visibility = View.GONE
        musicViewLayout.visibility = View.GONE
        showMenuBar()
    }

    override fun clickSubmitTopBar() {

        if (backgroundViewLayout.visibility == View.VISIBLE) {
            backgroundViewLayout.visibility = View.GONE
            showMenuBar()
        }

        if (ratioViewLayout.visibility == View.VISIBLE) {
            ratioViewLayout.visibility = View.GONE
            showMenuBar()
        }

        if (transitionViewLayout.visibility == View.VISIBLE) {
            transitionViewLayout.visibility = View.GONE
            showMenuBar()
        }
        if (musicViewLayout.visibility == View.VISIBLE) {
            musicViewLayout.visibility = View.GONE
            showMenuBar()

        }
        if (durationViewLayout.visibility == View.VISIBLE) {
            durationViewLayout.visibility = View.GONE
            showMenuBar()
        }

        if (textQuoteViewLayout.visibility == View.VISIBLE) {
            textQuoteViewLayout.visibility = View.GONE
            binding.preview.visibility = View.GONE
            showMenuBar()
        }


    }

    private fun applyMusicToView(currentUrl: String?) {
        val audio = AudioEntity(path = currentUrl!!)
//        binding.horizontalImageSlide.itemUtilityGroupList.get(0).utilityItemList.get(0).textItem =
//            "Test appy Music"
        setAudio(audio)

    }

    @SuppressLint("CheckResult")
    fun loadDataMusic() {
        if (Util.isInternetAvailable(context)) {
            APIService.service.getListCategoryMusic()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError({
                    Toast.makeText(context, "Cannot connect to server", Toast.LENGTH_SHORT).show()

                })
                .doFinally {


                }.subscribe(
                    {
                        if (it.getData() != null && it.getData()!!.size > 0) {
                            Log.d(Constant.YNSUPER_TAG, "CategoryListMusic: " + it.getData()!!.size)
                            liveDataCategory.value = it
                        }
                    }, {
                        Toast.makeText(context, "error: " + it.message, Toast.LENGTH_SHORT).show()
                    }
                )

        }

    }

    fun stopPreview() {
        binding.imageButtonControl.setImageResource(R.drawable.ic_icon_awesome_play)
//        binding.seekBarProgress.progress = 0
        littleBox?.stop()
        SoundManager.getInstance(context).getmMediaPlayer().stop()
    }

    fun loadHorizonSlide() {
        if (videoComposer.getScenes().size <= 0) {
            return
        }
        loadHorizonSlideImage()
//        loadMusicUtilitySlide()
//        loadMusicUtilityStickerSlide()

    }

    private fun loadMusicUtilityStickerSlide() {
        val imageSize: Int = DensityUtil.dip2px(context, 40F)
        val utilityItemList: MutableList<HorizontalThumbnailListView.UtilityItem> = ArrayList()
        val bitmapImageAddMusic =
            BitmapUtils.getBitmapFromVectorDrawable(context, R.drawable.sticker)

        utilityItemList.add(
            HorizontalThumbnailListView.UtilityItem(
                "Sticker",
                HorizontalThumbnailListView.UtilityItem.TYPE_STICKER,
                bitmapImageAddMusic,
                (imageSize).toInt(),
                0,
                (imageSize).toInt(),
                context.resources.getDrawable(R.drawable.ic_select_bg_sticker)

            )
        )
//        binding.horizontalImageSlide!!.newUtilityGroup(utilityItemList)
//        binding.horizontalImageSlide!!.setMusicGroupListener(mMusicGroupListener)

    }

//    private fun loadMusicUtilitySlide() {
//        val imageSize: Int = DensityUtil.dip2px(context, 40F)
//        val utilityItemList: MutableList<HorizontalThumbnailListView.UtilityItem> = ArrayList()
//        val bitmapImageAddMusic =
//            BitmapUtils.getBitmapFromVectorDrawable(context, R.drawable.ic_add_music)
//
//        utilityItemList.add(
//            HorizontalThumbnailListView.UtilityItem(
//                "Add Music",
//                HorizontalThumbnailListView.UtilityItem.TYPE_MUSIC,
//                bitmapImageAddMusic,
//                (imageSize).toInt(),
//                0,
//                (imageSize).toInt(), context.resources.getDrawable(R.drawable.ic_select_bg_music)
//            )
//        )
//        binding.horizontalImageSlide!!.newUtilityGroup(utilityItemList)
//        binding.horizontalImageSlide!!.setMusicGroupListener(mMusicGroupListener)
//
//    }

    private fun loadHorizonSlideImage() {
        binding.horizontalImageSlide.clear()
        val imageSize: Int = DensityUtil.dip2px(context, 40F)
        val screenWidth = context.windowManager.defaultDisplay.width
        val groupPadding: Int = DensityUtil.dip2px(context, 8F)
        binding.horizontalImageSlide!!.setImageHeight((imageSize * 1.5).toInt())
        binding.horizontalImageSlide!!.setImageWidth((imageSize).toInt())
        binding.horizontalImageSlide!!.setStartPaddingWidth(screenWidth / 2 - groupPadding)
        binding.horizontalImageSlide!!.setEndPaddingWidth(screenWidth / 2 - groupPadding)
        binding.horizontalImageSlide!!.setGroupPaddingWidth(groupPadding)
        binding.horizontalImageSlide!!.setPaddingVerticalHeight(DensityUtil.dip2px(context, 2F))
        binding.horizontalImageSlide!!.setSelectedGroupBg(context.resources.getDrawable(R.drawable.round_button_select_slides))
        binding.horizontalImageSlide!!.setImageGroupListener(mImageGroupListener)
        var timeInHeader = 0
        val sceneIndex = slideAdapter.selectedAt
        var imageTransition: String? = null
        if (sceneIndex >= 0) {
            imageTransition =
                "image_transition/" + transitionAdapter.items[sceneIndex].imagePreview + ".png"
        } else
            imageTransition = "image_transition/" + transitionAdapter.items[0].imagePreview + ".png"

        for (i in videoComposer.getScenes().listIterator()) {
//            val bitmap = BitmapFactory.decodeFile(mImagePathList!!.get(i))
            val bitmap: Bitmap = BitmapFactory.decodeFile(i.originalPath)
            val imageItemList: MutableList<HorizontalThumbnailListView.ImageItem> =
                ArrayList()
            var durationImage = (i.duration / 1000) - 1

            while (durationImage >= 0) {
                durationImage--
                imageItemList.add(
                    HorizontalThumbnailListView.ImageItem(
                        bitmap,
                        (imageSize).toInt(),
                        0,
                        (imageSize).toInt(), timeInHeader.toInt()
                    )
                )
                timeInHeader++

            }

            binding.horizontalImageSlide!!.newImageGroup(imageItemList, imageTransition)
        }
    }

//    fun changeDurationGroupImage(position: Int, time: Int) {
//        val imageSize: Int = DensityUtil.dip2px(context, 40F)
//        val screenWidth = context.windowManager.defaultDisplay.width
//        val groupPadding: Int = DensityUtil.dip2px(context, 16F)
//        var timeInHeader = 0
//
////            val bitmap = BitmapFactory.decodeFile(mImagePathList!!.get(i))
//        val bitmap: Bitmap = videoComposer.getScenes().get(position).bitmap
//        val imageItemList: MutableList<HorizontalThumbnailListView.ImageItem> =
//            ArrayList<HorizontalThumbnailListView.ImageItem>()
//        var durationImage = time
//
//        while (durationImage >= 0) {
//            durationImage--
//            imageItemList.add(
//                HorizontalThumbnailListView.ImageItem(
//                    bitmap,
//                    (imageSize).toInt(),
//                    0,
//                    (imageSize).toInt(), timeInHeader.toInt(),
//
//                )
//            )
//            timeInHeader++
//
//        }
//
//        binding.horizontalImageSlide!!.replaceImageGroup(imageItemList, position)
//
//    }


    private val mImageGroupListener: ImageGroupListener =
        object : ImageGroupListener() {
            override fun onImageGroupProcess(
                index: Int,
                progress: Float,
                isFromUser: Boolean
            ) {
                super.onImageGroupProcess(index, progress, isFromUser)
                mCurClipIndex = index
                Log.d(
                    Constant.YNSUPER_TAG,
                    "onImageGroupProcess: $index, progress: $progress, is from user: $isFromUser, curr index: $mCurClipIndex"
                )
            }

            override fun onImageGroupClicked(index: Int) {
                super.onImageGroupClicked(index)
                Log.d(
                    Constant.YNSUPER_TAG,
                    "onImageGroupClicked, index: $index"
                )
//                showTransitionSelection()
            }
        }


    private val mMusicGroupListener: MusicGroupListener =
        object : MusicGroupListener() {

            override fun onMusicGroupClicked(index: Int) {
                super.onMusicGroupClicked(index)
                Log.d(
                    Constant.YNSUPER_TAG,
                    "onImageGroupClicked, index: $index"
                )
                selectMenuMusic()
            }
        }

    fun onStateChange(state: OptionState) {

        videoComposer.updateSceneCropType(
            state.id, state.blur,
            state.progressBlur, state.color,
            BitmapProcessor.CropType.fromKey(state.crop!!)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {

            }
            .doOnSubscribe {
//                isLoading.postValue(true)
            }
            .doOnSuccess {
//                isLoading.postValue(false)
            }
            .subscribeBy(onError = {
                Toast.makeText(context, "Error while setting crop type", Toast.LENGTH_SHORT).show()
            }) {
                dispatchDraw()
            }.willBeDisposed()
    }


    override fun onItemListChange(index: Int) {
        Log.d("YnsuperTAG", "imageGroup.isSelected: " + index)



        if (lastIndex != index && durationViewLayout.visibility == View.VISIBLE) {
            Handler().post(Runnable {
                val scene = videoComposer.getScenes()[index]

                val state = DurationViewLayout.OptionState(
                    id = scene.id,
                    duration = scene.duration,
                    crop = scene.cropType.key()
                )
                durationViewLayout.setState(state)
            })
        }

        lastIndex = index
        slideAdapter?.select(index)

    }

    fun onDurationChange(state: DurationViewLayout.OptionState) {
        val scene = videoComposer.getScenes().find { it.id == state.id } ?: return
        scene.duration = state.duration
        state.id
        videoComposer.evaluateDuration()
        dispatchDraw()
        loadHorizonSlide()
    }

    fun showAddImageSheet() {
        AddImageGroupBottomSheet
            .newInstance(videoComposer.getScenes())
            .show(
                context.supportFragmentManager,
                "scene-options"
            )
    }

    fun reloadSlideBar() {
        videoComposer.evaluateDuration()
        dispatchDraw()
        loadHorizonSlide()
    }

    fun addMoreScene() {
        context?.let { data ->
            TedImagePicker.with(data)
                .startMultiImage { uriList ->
                    val arrImageModel = ArrayList<ImageModel>()
                    for (uri in uriList) {
                        arrImageModel.add(ImageModel(uri))
                    }
                    loadDataImage(arrImageModel)
                }
        } ?: run {
            Log.v(Constant.YNSUPER_TAG, "Permission is granted");
        }

    }

    fun onBackgroundConfigChage(state: OptionState) {
        onStateChange(state)

    }

    fun changeImageCloseToBack() {
        binding.musicViewLayout.changeImageCloseToBack()
    }

    fun changeBackToCloseImage() {
        binding.musicViewLayout.changeBackToCloseImage()

    }

    fun saveVideo() {
//        exportAsVideoFile()
        SaveVideoBottomSheet
            .newInstance()
            .show(
                context.supportFragmentManager,
                "save_video"
            )

    }

    private fun exportAsVideoFile(width: Int, height: Int) {
        isExport.value = true
        binding.textViewMessage.text = "Preparing for export..."
        val folderSave = File(Constants.PATH_SAVE_FILE_VIDEO)
        folderSave.mkdirs()
        val file =
            File(Constants.PATH_SAVE_FILE_VIDEO, "my-video-${System.currentTimeMillis()}.mp4")

        file.createNewFile()
        val path = file.path

        val audioPath = SoundManager.getInstance(context).currentUrl

        littleBox?.exportToVideo(width, height, path, audioPath, {
            context.runOnUiThread {
                binding.imageThumb.setImageURI(Uri.parse(slides[0].path))
                binding.textViewMessageExport.text = "Exporting (${formatPercent(it)}%)"
                binding.progressBarExport.progress = (it * 100f).toInt()
            }
        }) {
            context.runOnUiThread {
                binding.textViewMessage.text = "Completed"

//
                val outputAudio =
                    File(
                        Constants.PATH_SAVE_FILE_VIDEO,
                        "my-video-${System.currentTimeMillis()}.aac"
                    )

                outputAudio.createNewFile()

                littleBox?.muxerAudio(
                    context,
                    File(audioPath),
                    File(path),
                    outputAudio,
                    totalDuration,
                    {
                        binding.textViewMessageExport.text = "Muxer music (${it * 100.0f}%)"
                        binding.progressBarExport.progress = (it * 100.0f).toInt()
                    },
                    {
                        saveAsStory(it)
//                    play(it)
                        isExport.value = false
                    })
            }
        }
    }

    private fun formatPercent(value: Float): String {
        return "%.2f".format(value * 100f)
    }

    fun saveDraft() {
        if (slides.isEmpty()) return
        appDatabase.slideDao().upsert(*slides.toTypedArray())
        if (audio != null) appDatabase.audioDao().upsert(audio!!)
        Toast.makeText(context, "Draft Saved", Toast.LENGTH_SHORT).show()

    }

    private fun saveAsStory(path: String) {
        val storyEntity = StoryEntity(
            title = "My SlideShow",
            path = path,
            createdAt = System.currentTimeMillis()
        )

        appDatabase.storyDao().insert(storyEntity)
        Toast.makeText(context, "Saved to stories", Toast.LENGTH_SHORT).show()
    }

    private fun play(path: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(path))
        intent.setDataAndType(Uri.parse(path), "video/mp4")
        context.startActivity(intent)
    }

    fun addStickerInVideo(bitmap: Bitmap?) {

        val sticker = StickerView(context)
        sticker.setBitmap(bitmap)
        sticker.setOperationListener(object : StickerView.OperationListener {
            override fun onDeleteClick() {

            }

            override fun onEdit(stickerView: StickerView?) {

            }

            override fun onTop(stickerView: StickerView?) {

            }

            override fun onApply(stickerView: StickerView?) {

            }

        })
        val lp: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        binding.rlContentRoot.addView(sticker, lp)


    }

    fun selectMusicSong(songName: String) {
        musicViewLayout.visibility = View.GONE
        showMenuBar()
//        binding?.horizontalImageSlide.textMusicSong = songName
        applyMusicToView(SoundManager.getInstance(context).currentUrl)
        showMenuBar()
        binding.playVisualView.updateVisualizer(
            CommonUtils.fileToBytes(
                File(
                    SoundManager.getInstance(
                        context
                    ).currentUrl
                )
            )
        )

    }

    fun exportVideo(width: Int, height: Int) {
        exportAsVideoFile(width, height)
    }

    fun changeRatioPreview(width: Int, height: Int) {
        val layoutParams = binding.textureView.layoutParams as ConstraintLayout.LayoutParams
        binding.textureView.layoutParams = layoutParams
        var ratio = 1
        ratio = if (width > height) {
            1080 / height
        } else {
            1080 / width
        }


        var newSizeVideo = videoComposer.videoSize

        if (width > height) {
            newSizeVideo = Size(ratio * width, ratio * height)
            layoutParams.dimensionRatio = "h,${newSizeVideo.width}:${newSizeVideo.height}"
        } else {
            newSizeVideo = Size(ratio * width, ratio * height)
            layoutParams.dimensionRatio = "w,${newSizeVideo.width}:${newSizeVideo.height}"
        }
        Log.d(
            "Ynsuper",
            "new Size Video: " + "$width:$height-----" + newSizeVideo.width + ": " + newSizeVideo.height
        )
        videoComposer.videoSize = newSizeVideo
        // fix bug not redraw
        dispatchDraw()
        dispatchDraw()
        dispatchDraw()
        dispatchDraw()


    }

    fun loadTextQuote() {
        quoteState = QuoteState(
            text = "Hello, World!",
            textColor = Color.BLACK,
            textSize = 18f.dip(context.resources),
            scaleFactor = 1f,
            fontFamily = null
        )
    }

    fun newQuoteState(quoteState: QuoteState) {
        quoteStatePool[slideAdapter.selectedAt] = quoteState
        this.quoteState = quoteState
        dispatchDraw()
    }

    fun onReceiverQuoteBitMap(bitmap: Bitmap) {
        if (slideAdapter.selectedAt != -1) {
            videoComposer.setQuoteAt(slideAdapter.selectedAt, bitmap)
        } else videoComposer.applyQuoteBitmap(bitmap)
        dispatchDraw()
    }


}