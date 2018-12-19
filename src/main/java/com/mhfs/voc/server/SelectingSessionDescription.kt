package com.mhfs.voc.server

import com.mhfs.voc.VocabularyService

class SelectingSessionDescription(isActivation: Boolean = false, maxCount: Int = 0, val selector: String):
        VocabularyService.SessionDescription(isActivation, maxCount)