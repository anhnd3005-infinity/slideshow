package com.ynsuper.slideshowver1.viewmodel

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.ynsuper.slideshowver1.MainActivity
import com.ynsuper.slideshowver1.base.BaseViewModel
import com.ynsuper.slideshowver1.database.AppDatabase
import com.ynsuper.slideshowver1.databinding.ActivityMainBinding
import com.ynsuper.slideshowver1.model.ImageModel
import com.ynsuper.slideshowver1.util.Constant
import com.ynsuper.slideshowver1.util.entity.SlideEntity
import com.ynsuper.slideshowver1.view.SlideShowActivity
import com.ynsuper.slideshowver1.view.activity.MyVideosActivity
import com.ynsuper.slideshowver1.view.activity.ProActivity
import com.ynsuper.slideshowver1.view.adapter.DraftListAdapter
import gun0912.tedimagepicker.builder.TedImagePicker
import java.io.File

class MainViewModel : BaseViewModel() {
    private var appDatabase: AppDatabase? = null
    private lateinit var binding: ActivityMainBinding
    private val adapter: DraftListAdapter = DraftListAdapter()
    private var activity: MainActivity? = null
    private var lastTime: Long = 0

    fun startImagePicker() {
        activity?.let { data ->
            TedImagePicker.with(data)
                .startMultiImage { uriList ->
                    val arrImageModel = ArrayList<ImageModel>()
                    for (uri in uriList) {
                        arrImageModel.add(ImageModel(uri))
                    }
                    val intent = Intent(data, SlideShowActivity::class.java)
                    intent.putParcelableArrayListExtra(
                        Constant.EXTRA_ARRAY_IMAGE,
                        arrImageModel
                    )
                    data.startActivity(intent)
                }
        } ?: run {
            Log.v(Constant.YNSUPER_TAG, "Permission is granted");
        }
    }

    fun setMainActivity(mainActivity: MainActivity) {
        this.activity = mainActivity
    }

    fun checkRuntimePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity!!.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {

            Log.v(Constant.YNSUPER_TAG, "Permission is granted");
            //File write logic here
            loadDataDraftAndVideoSaved()
            val arrImageModel = ArrayList<ImageModel>()
            for (uri in adapter.items) {
                val file = File(uri.path)
                arrImageModel.add(ImageModel(Uri.fromFile(file)))
            }

            adapter.onItemClicked = {
                // handle draft
                val intent = Intent(activity, SlideShowActivity::class.java)
                intent.putParcelableArrayListExtra(
                    Constant.EXTRA_ARRAY_IMAGE, arrImageModel
                )
                activity!!.startActivity(intent)
            }


        } else {

        }
    }


    private fun loadDataDraftAndVideoSaved() {
        loadDataVideoDraft()
    }

    fun loadDataVideoDraft() {
        appDatabase =
            Room.databaseBuilder(activity!!.baseContext, AppDatabase::class.java, "slideshow-v1")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
        getAllFlow()
        loadVideoDraft()

    }


    private fun loadVideoDraft() {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding.recycleMyDraft.layoutManager = layoutManager
        binding.recycleMyDraft.adapter = adapter
        binding.recycleMyDraft.setHasFixedSize(true)
    }

    fun setBinding(binding: ActivityMainBinding) {
        this.binding = binding
    }

    fun getAllFlow() {
        val listFilter = ArrayList<SlideEntity>()
        if (appDatabase != null) {

            val fromDb = appDatabase!!.slideDao().getAll()
            fromDb.filter { !File(it.path).exists() }.forEach(appDatabase!!.slideDao()::delete)


            listFilter.addAll(fromDb.filter { File(it.path).exists() })


            adapter.patch(listFilter)
            if (adapter.items.size > 0) {
                binding.linearNoVideo.visibility = View.GONE
                binding.recycleMyDraft.visibility = View.VISIBLE
            }

        }
    }


    fun selectNavigatorHome(): Boolean {

        return true
    }

    fun selectNavigatorMyVideo(): Boolean? {
        val intent = Intent(activity, MyVideosActivity::class.java)
        activity?.startActivity(intent)
        return true
    }

    fun selectNavigatorPro(): Boolean? {
        val intent = Intent(activity, ProActivity::class.java)
        activity?.startActivity(intent)
        return true
    }

}