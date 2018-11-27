package com.mhfs.voc.server

import com.mhfs.voc.VocabularyService
import io.ktor.auth.Principal
import java.io.Serializable

data class UserSession(val userName: String, val userID: Int): Principal, Serializable {
    var phaseDuration: Array<Int> = arrayOf(0, 1, 3, 7, 14, 60)
    var session: MutableList<DBAccess.DBQuestion>? = null
    var isActivation = false
    var previousResult: VocabularyService.Result? = null
    var currentQuestion: DBAccess.DBQuestion? = null
}