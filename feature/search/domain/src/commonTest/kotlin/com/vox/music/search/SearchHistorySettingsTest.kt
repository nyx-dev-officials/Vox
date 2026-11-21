package com.vox.music.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchHistorySettingsTest {
    @Test
    fun recordsNonemptyQueriesWhenHistoryIsEnabled() {
        assertTrue(shouldRecordSearchQuery("vox", SearchHistorySettings()))
    }

    @Test
    fun doesNotRecordQueriesWhenHistoryIsPaused() {
        assertFalse(shouldRecordSearchQuery("vox", SearchHistorySettings(paused = true)))
    }

    @Test
    fun doesNotRecordQueriesBeforeSettingsAreLoaded() {
        assertFalse(shouldRecordSearchQuery("vox", null))
    }

    @Test
    fun doesNotRecordEmptyQueries() {
        assertFalse(shouldRecordSearchQuery("", SearchHistorySettings()))
    }
}
