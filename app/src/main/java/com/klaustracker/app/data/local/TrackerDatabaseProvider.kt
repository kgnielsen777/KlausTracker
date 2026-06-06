package com.klaustracker.app.data.local

import android.content.Context

object TrackerDatabaseProvider {
    fun database(context: Context): TrackerDatabase = TrackerDatabase.getInstance(context)
}
