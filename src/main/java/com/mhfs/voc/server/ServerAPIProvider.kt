package com.mhfs.voc.server

import com.mhfs.voc.VocabularyService
import com.mhfs.voc.VocabularyService.*
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("vocserv")
@Produces(MediaType.APPLICATION_JSON)
class ServerAPIProvider(val dbAccess: DBAccess, val phaseDuration: Array<Int>) : VocabularyService {

    private var session: MutableList<DBAccess.DBQuestion>? = null
    private var isActivation = false
    private var previousResult: Result? = null
    private var currentQuestion: DBAccess.DBQuestion? = null

    @Path("session")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    override fun createSession(description: SessionDescription): State {
        session = if (description.isActivation) {
            dbAccess.getVocabularyForActivation(description.maxCount)
        } else {
            dbAccess.getVocabulary(description.maxCount)
        }
        isActivation = description.isActivation
        previousResult = null
        if (session!!.isEmpty()) {
            session = null
            isActivation = false
            currentQuestion = null
        } else {
            currentQuestion = session!![0]
        }
        return getState()
    }

    @Path("state")
    @GET
    override fun getState(): State = getState(true)

    private fun getState(censor: Boolean): State {
        return if (session != null) {
            var q  = currentQuestion
            if (censor &&!isActivation && q != null)
                q = DBAccess.DBQuestion(q.id, q.question, q.questionLanguage, null, q.targetLanguage, q.associatedData)
            State(q, previousResult, session?.size ?: 0)
        } else {
            State(null, previousResult, 0)
        }
    }

    @Path("cancel")
    @GET //Should be POST, but that requires a data transfer.
    override fun cancelSession(): State {
        session = null
        previousResult = null
        return getState()
    }

    @Path("answer")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    override fun answer(answer: String): State {
        if (currentQuestion == null)
            throw IllegalStateException("No current question exists to be answered.")
        val solution = currentQuestion!!.solution!!
        val reducedSolution = solution.replace(Regex("(\\(|\\)|(\\[[^\\]]*]))"), "").trim()
        if(answer == currentQuestion?.solution || answer == reducedSolution) {
            markQuestion(true)
        } else if(currentQuestion!!.solution!!.contains(answer)) {
            previousResult = Result(ResultType.UNDETERMINED, currentQuestion!!.solution!!)
        } else {
            markQuestion(false)
        }
        return getState(true)
    }

    @Path("correction")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    override fun correction(correct: Boolean): State {
        if (session == null) {
            throw IllegalStateException("No session exists.")
        }
        if (currentQuestion == null) {
            throw IllegalStateException("No current question.")
        }
        if (!(previousResult != null && previousResult!!.type == ResultType.UNDETERMINED)) {
            throw IllegalStateException("Not correcting.")
        }
        markQuestion(correct)
        return getState()
    }

    private fun markQuestion(correct: Boolean) {
        session!!.remove(currentQuestion)
        val q = currentQuestion!!
        val currentPhase = (q.associatedData["phase"]?.toInt() ?:0)
        if (correct) {
            val newPhase = currentPhase + 1
            q.associatedData["phase"] = newPhase.toString()
            if (isActivation) {
                dbAccess.activate(q)
            } else {
                val cal = Calendar.getInstance()
                cal.time = Date()
                cal.add(Calendar.DAY_OF_MONTH, phaseDuration[newPhase - 1])
                dbAccess.updateQuestion(q, cal.time)
            }
            previousResult = Result(ResultType.CORRECT, currentQuestion!!.solution!!)
        } else if (!isActivation) {
            session!!.add(currentQuestion!!) //Q was answered incorrectly, we need to ask again.
            val newPhase = Math.max(currentPhase - 1, 1)
            q.associatedData["phase"] = newPhase.toString()
            val cal = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.DAY_OF_MONTH, phaseDuration[newPhase - 1])
            dbAccess.updateQuestion(q, cal.time)
            previousResult = Result(ResultType.WRONG, currentQuestion!!.solution!!)
        }
        if (session!!.isEmpty()) {
            session = null
            currentQuestion = null
        } else {
            currentQuestion = session!![0]
        }
    }
}