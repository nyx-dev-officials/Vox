package com.vox.music.db

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Runs database-only instrumentation without starting Vox's full application graph. */
class RoomMigrationTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
