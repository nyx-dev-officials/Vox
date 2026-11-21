package com.vox.music.viewmodels

import androidx.lifecycle.ViewModel
import com.vox.music.core.updater.UpdateRepository

class UpdateViewModel(private val repository: UpdateRepository) : ViewModel() {
    val state = repository.state
    init { repository.initialize() }
    fun check(manual: Boolean = false) { repository.check(manual) }
    fun download() { repository.download() }
    fun cancel() { repository.cancel() }
    fun clearDownloadedUpdate() { repository.clearDownloadedUpdate() }
    fun dismiss() { repository.dismiss() }
    fun snooze() { repository.snooze() }
}
