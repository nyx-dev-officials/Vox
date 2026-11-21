package com.vox.music.di

import com.vox.music.ui.screens.search.AppleSearchViewModel
import com.vox.music.ui.screens.search.suggestions.SuggestionsViewModel
import com.vox.music.viewmodels.MoodAndGenresViewModel
import com.vox.music.viewmodels.OnlineSearchViewModel
import com.vox.music.viewmodels.OnlineSearchSuggestionViewModel
import com.vox.music.viewmodels.ExploreViewModel
import com.vox.music.viewmodels.LocalSearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchPresentationModule = module {
    viewModelOf(::AppleSearchViewModel)
    viewModelOf(::SuggestionsViewModel)
    viewModelOf(::MoodAndGenresViewModel)
    viewModelOf(::OnlineSearchViewModel)
    viewModelOf(::OnlineSearchSuggestionViewModel)
    viewModelOf(::ExploreViewModel)
    viewModelOf(::LocalSearchViewModel)
}
