package com.mhfs.voc.server

import com.mhfs.voc.VocabularyService.*
import java.util.*

/**
 * For method details refer to {@link VocabularyService}
 * This is a multi user server. It must support multiple sessions and therefore requires authentication and passing
 * of the session. This is incompatible with the mentioned interface.
 *
 * This class may handler asynchronous calls. Concurrency problems may occur if the session is passed in a different manner.
 *
 * dbAccess is inherently single threaded. This may change in the future if other DBMS are used.
 */
class ServerAPIProvider(private val dbAccess: DBAccess) {

    fun createSession(description: SessionDescription, userSession: UserSession): State {
        if (description.maxCount <= 0) description.maxCount = Integer.MAX_VALUE
        userSession.session = if (description.isActivation) {
            dbAccess.getVocabularyForActivation(userSession.userID, description.maxCount)
        } else {
            dbAccess.getVocabulary(userSession.userID, description.maxCount)
        }
        userSession.isActivation = description.isActivation
        userSession.previousResult = null
        if (userSession.session!!.isEmpty()) {
            userSession.session = null
            userSession.isActivation = false
            userSession.currentQuestion = null
        } else {
            userSession.currentQuestion = userSession.session!![0]
        }
        return getState(userSession)
    }

    fun getState(userSession: UserSession): State = getState(true, userSession)

    private fun getState(censor: Boolean, userSession: UserSession): State {
        return if (userSession.session != null) {
            var q  = userSession.currentQuestion
            if (censor && !userSession.isActivation && q != null)
                q = DBAccess.DBQuestion(q.id, q.uid, q.question, q.questionLanguage, null, q.targetLanguage,
                        q.phase, q.associatedData)
            State(q, userSession.previousResult, userSession.session?.size ?: 0)
        } else {
            State(null, userSession.previousResult, 0)
        }
    }

    fun cancelSession(userSession: UserSession): State {
        userSession.session = null
        userSession.previousResult = null
        return getState(userSession)
    }

    fun answer(answer: String, userSession: UserSession): State {
        if (userSession.currentQuestion == null)
            throw IllegalStateException("No current question exists to be answered.")
        val solution = userSession.currentQuestion!!.solution!!
        val reducedSolution = solution.replace(Regex("(\\(|\\)|(\\[[^\\]]*]))"), "").trim()
        if(answer == userSession.currentQuestion?.solution || answer == reducedSolution) {
            markQuestion(true, userSession)
        } else if(userSession.currentQuestion!!.solution!!.contains(answer) && !answer.trim().isEmpty()) {
            userSession.previousResult = Result(ResultType.UNDETERMINED, userSession.currentQuestion!!.solution!!)
        } else {
            markQuestion(false, userSession)
        }
        return getState(true, userSession)
    }

    fun correction(correct: Boolean, userSession: UserSession): State {
        if (userSession.session == null) {
            throw IllegalStateException("No session exists.")
        }
        if (userSession.currentQuestion == null) {
            throw IllegalStateException("No current question.")
        }
        if (!(userSession.previousResult != null && userSession.previousResult!!.type == ResultType.UNDETERMINED)) {
            throw IllegalStateException("Not correcting.")
        }
        markQuestion(correct, userSession)
        return getState(userSession)
    }

    private fun markQuestion(correct: Boolean, userSession: UserSession) {
        userSession.session!!.remove(userSession.currentQuestion)
        val q = userSession.currentQuestion!!
        if (correct) {
            q.phase++
            if (userSession.isActivation) {
                dbAccess.activate(q)
            } else {
                val cal = Calendar.getInstance()
                cal.time = Date()
                cal.add(Calendar.DAY_OF_MONTH, userSession.phaseDuration[q.phase - 1])
                dbAccess.updateQuestion(q, cal.time)
            }
            userSession.previousResult = Result(ResultType.CORRECT, userSession.currentQuestion!!.solution!!)
        } else if (!userSession.isActivation) {
            userSession.session!!.add(userSession.currentQuestion!!) //Q was answered incorrectly, we need to ask again.
            q.phase = Math.max(q.phase - 1, 1)
            val cal = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.DAY_OF_MONTH, userSession.phaseDuration[q.phase - 1])
            dbAccess.updateQuestion(q, cal.time)
            userSession.previousResult = Result(ResultType.WRONG, userSession.currentQuestion!!.solution!!)
        }
        if (userSession.session!!.isEmpty()) {
            userSession.session = null
            userSession.currentQuestion = null
        } else {
            userSession.currentQuestion = userSession.session!![0]
        }
    }
}