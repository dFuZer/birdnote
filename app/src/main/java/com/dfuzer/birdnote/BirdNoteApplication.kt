package com.dfuzer.birdnote

import android.app.Application
import android.content.Context

class BirdNoteApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withResolvedLocale())
    }
}
