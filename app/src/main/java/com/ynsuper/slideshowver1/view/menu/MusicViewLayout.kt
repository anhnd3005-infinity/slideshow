package com.ynsuper.slideshowver1.view.menu

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Toast
import com.ynsuper.slideshowver1.R
import com.ynsuper.slideshowver1.adapter.MusicAdapter
import com.ynsuper.slideshowver1.adapter.SoundManager
import com.ynsuper.slideshowver1.base.BaseCustomConstraintLayout
import com.ynsuper.slideshowver1.callback.APIService
import com.ynsuper.slideshowver1.callback.TopBarController
import com.ynsuper.slideshowver1.model.AlbumMusicModel
import com.ynsuper.slideshowver1.model.MusicModel
import com.ynsuper.slideshowver1.util.Constant
import com.ynsuper.slideshowver1.util.Constants
import com.ynsuper.slideshowver1.view.SlideShowActivity
import com.ynsuper.slideshowver1.view.adapter.MusicViewPagerAdapter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_album_music.view.*
import kotlinx.android.synthetic.main.item_layout_edit_top_view.view.image_close_menu
import kotlinx.android.synthetic.main.layout_music_view.view.*
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.buffer
import okio.sink
import retrofit2.Response
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLConnection


class MusicViewLayout : BaseCustomConstraintLayout, MusicAdapter.OnSongClickListener {
    private lateinit var onSelectedSongListener: OnSelectedSongListener
    private var categoryListMusic: AlbumMusicModel? = null
    private lateinit var musicViewPagerAdapter: MusicViewPagerAdapter
    private lateinit var topbarController: TopBarController

    constructor(context: Context?) : super(context) {
        init(context)
        setLayoutInflate(R.layout.layout_music_view)
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!,
        attrs
    ) {
        init(context)
        setLayoutInflate(R.layout.layout_music_view)
        initView()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context!!, attrs, defStyleAttr) {
        init(context)
        setLayoutInflate(R.layout.layout_music_view)
        initView()

    }

    private fun initView() {
        musicViewPagerAdapter = MusicViewPagerAdapter(
            (context as SlideShowActivity).supportFragmentManager,
            this
        )
        viewpager.adapter = musicViewPagerAdapter
        tab_layout_music.setupWithViewPager(viewpager)
        categoryListMusic?.let { musicViewPagerAdapter.setCategoryList(it) }
        image_close_menu.visibility = View.VISIBLE
        image_close_menu.setOnClickListener {
            SoundManager.getInstance(context).stopSound()
            topbarController.clickCloseTopBar()
        }
        setSelectedSongListener()
    }

    private fun handleSaveMp3ToDisk(
        url: String?,
        name: String,
        progressBar: ProgressBar,
        imgDownUse: ImageView
    ) {
        APIService.service.downloadMP3(url!!)
            .flatMap<File> {
                val response = it
                Observable.create { it ->
                    try {
                        // you can access headers of response
                        val header: String =
                            response.headers()["x-amz-meta-sha256"]!!
                        // this is specific case, it's up to you how you want to save your file
                        // if you are not downloading file from direct link, you might be lucky to obtain file name from header
                        val fileName = "$name.mp3"
                        // will create file in global Music directory, can be any other directory, just don't forget to handle permissions
                        val sizeContentLength = getSizeContentLength(URL(url))
                        val file = File(
                            Constants.PATH_DOWNLOAD_MUSIC_FROM_CLOUD, fileName
                        )

                        val sink: BufferedSink = file.sink().buffer()
                        // you can access body of response

                        val dataResponse: InputStream = response.body()!!.byteStream()
                        val data = ByteArray(1024 * 8)
                        val input = BufferedInputStream(dataResponse)
                        var total: Long = 0
                        var count = 0
                        while (input.read(data).also { count = it } !== -1) {
                            total += count
                            sink.write(data, 0, count)
                            Log.e(
                                Constant.NDPHH_TAG,
                                "downloadZipFile progress: " + total * 1.0f / sizeContentLength * 100
                            )
                            progressBar.progress = (total*1.0f/sizeContentLength*100).toInt()
                        }

//                        sink.writeAll(response.body()!!.source())
                        sink.close()
                        it.onNext(file)
                        it.onComplete()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        it.onError(e)
                    }
                }
            }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError({
                Toast.makeText(context,"Cannot connect to server", Toast.LENGTH_SHORT).show()

            })
            .subscribeBy(
                onError = {
                    Log.e(Constant.NDPHH_TAG, "downloadZipFile error ${it.message}")
                },
                onNext = { Log.d(Constant.NDPHH_TAG, "downloadZipFile onNext  ${it.name}") },
                onComplete = {
                    Log.d(Constant.NDPHH_TAG, "downloadZipFile onComplete ")
                    progressBar.visibility = GONE
                    imgDownUse.visibility = VISIBLE
                    imgDownUse.setImageResource(R.drawable.ic_check_circle_black_24dp)
                }
            )


    }

    private fun getSizeContentLength(url: URL): Int {
        val urlConnection: URLConnection = url.openConnection()
        urlConnection.connect()
        return urlConnection.getContentLength()
    }

    private fun handleResponseBody(response: Response<ResponseBody>) {

    }

    fun setTopbarController(topbarController: TopBarController) {
        this.topbarController = topbarController
    }

    fun setSelectedSongListener() {
        this.onSelectedSongListener = context as SlideShowActivity
    }

    fun setCategoryListModel(it: AlbumMusicModel) {
        this.categoryListMusic = it
        musicViewPagerAdapter?.setCategoryList(it)
    }

    fun changeImageCloseToBack() {
        image_close_menu.setImageResource(R.drawable.ic_arrow_back_black_24dp)
        image_close_menu.setOnClickListener {
            container_seekbar?.visibility = View.GONE
            SoundManager.getInstance(context).stopSound()
            if (context is SlideShowActivity) {
                (context as SlideShowActivity).changeBackToCloseImage()
            }
        }
    }

    fun changeBackToCloseImage() {
        image_close_menu.setImageResource(R.drawable.ic_close_black_24dp)
        image_close_menu.setOnClickListener {
            topbarController.clickCloseTopBar()
        }
        recyclerViewAlbum.visibility = View.VISIBLE
        list_view_music.visibility = View.INVISIBLE
    }

    fun createTimeLabel(time: Int): String? {
        var timeLabel: String? = ""
        val min = time / 1000 / 60
        if (min < 10) timeLabel += "0"
        val sec = time / 1000 % 60
        timeLabel += "$min:"
        if (sec < 10) timeLabel += "0"
        timeLabel += sec
        return timeLabel
    }

    override fun onSongDownloadClick(
        url: String,
        name: String,
        progressBar: ProgressBar,
        imgDownUse: ImageView,
        isFromInternet: Boolean
    ) {
        if (isFromInternet) {
            val file = File(Constants.PATH_DOWNLOAD_MUSIC_FROM_CLOUD, "$name.mp3")
            if (!file.exists()) {
                progressBar.visibility = VISIBLE
                imgDownUse.visibility = GONE
                handleSaveMp3ToDisk(url, name, progressBar, imgDownUse)
            } else {
                Log.d(Constant.NDPHH_TAG, "Chose downloaded: " + file.absolutePath)
                SoundManager.getInstance(context).changeSound(file.absolutePath, name)
                SoundManager.getInstance(context).getmMediaPlayer().stop()
                onSelectedSongListener.onSelectedSong(name)
            }
        } else {
            Log.d(Constant.NDPHH_TAG, "chose local song: $url")
            SoundManager.getInstance(context).changeSound(url, name)
            SoundManager.getInstance(context).getmMediaPlayer().stop()
            onSelectedSongListener.onSelectedSong(name)
        }


    }

    override fun onSongClick(musicModel: MusicModel?) {
        val file = File(Constants.PATH_DOWNLOAD_MUSIC_FROM_CLOUD, "${musicModel!!.name}.mp3")
        if (file.exists()) { // play offline
            playSong(file.absolutePath, musicModel.name)
            Log.e(Constant.NDPHH_TAG, "Playing offline: ${musicModel.name}")
        } else { // play online
            playSong(musicModel.audio, musicModel.name)
            Log.e(Constant.NDPHH_TAG, "Playing online: ${musicModel.name}")
        }
    }

    private fun playSong(url: String?, name: String?) {
        container_seekbar.visibility = View.VISIBLE
        progress_load_music.visibility = View.VISIBLE
        btn_play_music.visibility = View.INVISIBLE

        val soundManager = SoundManager.getInstance(context)
        Thread(Runnable {
            soundManager.changeSound(url, name)
        }).start()

        soundManager.getmMediaPlayer().setOnPreparedListener {
            progress_load_music.visibility = View.INVISIBLE
            btn_play_music.visibility = View.VISIBLE
            seek_bar!!.max = soundManager.getmMediaPlayer()!!.duration
            seek_bar!!.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            soundManager.getmMediaPlayer()!!.seekTo(progress)
                            seekBar.progress = progress
                        }
                        seekBar.max = soundManager.getmMediaPlayer()!!.duration
                        txtTimeDuration!!.text =
                            createTimeLabel(soundManager.getmMediaPlayer()!!.currentPosition)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                }
            )

            txtTimeTotal!!.text = createTimeLabel(soundManager.getmMediaPlayer().duration)
            val handler = android.os.Handler()
            val runnable = object : Runnable {
                override fun run() {
                    seek_bar.progress = soundManager!!.getmMediaPlayer().currentPosition
//                    binding?.txtTimeDuration!!.text =
//                        createTimeLabel(soundManager.getmMediaPlayer()!!.currentPosition)
                    handler.postDelayed(this, 1000)
                }
            }
            if (visibility == View.VISIBLE){
                soundManager.startSound()
            }

            if (soundManager.getmMediaPlayer().isPlaying) btn_play_music!!.setImageDrawable(
                context!!.getDrawable(
                    R.drawable.ic_round_pause_24
                )
            )


            btn_play_music!!.setOnClickListener(View.OnClickListener {
                if (soundManager.getmMediaPlayer()!!.isPlaying) {
                    soundManager.pauseSound()
                    btn_play_music!!.setImageDrawable(context!!.getDrawable(R.drawable.ic_icon_awesome_play))
                } else {
                    soundManager.startSound()
                    btn_play_music!!.setImageDrawable(context!!.getDrawable(R.drawable.ic_round_pause_24))

                }
            })
            handler.post(runnable)
        }
    }

    override fun onSongClick(filePath: String?, fileName: String?) {
        playSong(filePath, fileName)
    }

    public interface OnSelectedSongListener {
        fun onSelectedSong(songName: String) {
        }
    }

}