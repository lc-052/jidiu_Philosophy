package com.xiaojidiu.philosophy

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xiaojidiu.philosophy.data.local.database.ThoughtDatabase
import com.xiaojidiu.philosophy.data.local.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.After

abstract class DatabaseTestBase {
    protected lateinit var db: ThoughtDatabase
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),ThoughtDatabase::class.java).build()
        db.contentDao().putQuestions(listOf(QuestionEntity("q","题目","场景","伦理",1,"背景","原创","编辑",0,emptyList(),emptyList(),emptyList())))
        db.contentDao().putOptions(listOf(QuestionOptionEntity("a","q","选项一",0,emptyList()),QuestionOptionEntity("b","q","选项二",1,emptyList())))
    }
    @After fun teardown() { db.close() }
}
