package com.ynsuper.slideshowver1.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ynsuper.slideshowver1.dao.SlideDao
import com.ynsuper.slideshowver1.dao.StoryDao
import com.ynsuper.slideshowver1.dao.AudioDao
import com.ynsuper.slideshowver1.util.entity.AudioEntity

import com.ynsuper.slideshowver1.util.entity.SlideEntity
import com.ynsuper.slideshowver1.util.entity.StoryEntity


@Database(version = 1,
    exportSchema = false,
    entities = [SlideEntity::class, AudioEntity::class, StoryEntity::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun slideDao(): SlideDao
    abstract fun audioDao(): AudioDao
    abstract fun storyDao(): StoryDao
}