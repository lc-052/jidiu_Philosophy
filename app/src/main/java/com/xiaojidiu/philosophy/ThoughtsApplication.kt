package com.xiaojidiu.philosophy
import android.app.Application
class ThoughtsApplication : Application() { val graph by lazy { AppGraph(this) } }
