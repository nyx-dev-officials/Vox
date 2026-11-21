package com.vox.music.di

import com.vox.music.viewmodels.BackupRestoreViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val backupRestorePresentationModule = module {
    viewModelOf(::BackupRestoreViewModel)
}
