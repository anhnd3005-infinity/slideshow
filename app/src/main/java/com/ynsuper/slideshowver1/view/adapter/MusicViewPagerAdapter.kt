package com.ynsuper.slideshowver1.view.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ynsuper.slideshowver1.adapter.MusicAdapter
import com.ynsuper.slideshowver1.model.AlbumMusicModel
import com.ynsuper.slideshowver1.view.fragment.FragmentAlbumMusic
import com.ynsuper.slideshowver1.view.fragment.FragmentAlbumMusic.Companion.newInstance
import com.ynsuper.slideshowver1.view.fragment.FragmentMyMusic

class MusicViewPagerAdapter(fm: FragmentManager?, private var onSongClickListener: MusicAdapter.OnSongClickListener) :
    FragmentPagerAdapter(fm!!) {
    private var fragmentManager = fm
    private val fragmentAlbumMusic: FragmentAlbumMusic? = null
    private val fragmentMusic: FragmentMyMusic? = null


    override fun getItem(position: Int): Fragment {
        var fragment: Fragment? = null
//        val transaction = fragmentManager!!.beginTransaction()

        if (position == 0) {
            fragment = FragmentAlbumMusic.newInstance(fragmentManager!!, onSongClickListener)
//            transaction.add(R.id.albumMusicContainer, fragment)
//            transaction.addToBackStack(null)
//            transaction.commit()
        } else if (position == 1) {
            fragment = FragmentMyMusic(onSongClickListener)

        }
        return fragment!!
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence? {
        var title: String? = null
        if (position == 0) {
            title = "Album"
        } else if (position == 1) {
            title = "My Music"
        }
        return title
    }

    
    fun setCategoryList(it: AlbumMusicModel) {
        (getItem(0) as FragmentAlbumMusic).viewModel?.categoryMusicList = it
        newInstance(fragmentManager!!, onSongClickListener).categoryMusicList = it
    }
}