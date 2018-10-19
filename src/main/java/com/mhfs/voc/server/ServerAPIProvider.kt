package com.mhfs.voc.server

import com.mhfs.voc.Question
import com.mhfs.voc.VocabularyService
import com.mhfs.voc.Word
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("vocserv")
@Produces(MediaType.APPLICATION_JSON)
class ServerAPIProvider(val dbAccess: DBAccess) : VocabularyService {

    private var session: Map<Question, Word>? = null
    private var isActivation = false
    private var isCorrecting = false
    private var currentQuestion: Question? = null

    @Path("session")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    override fun createSession(maxCount: Int): Int {
        if (session != null) throw IllegalStateException("A session already exists!")
        //TODO: Create session
        return 1
    }

    @Path("activationSession")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    fun createActivationSession(data: CreateActivationSessionRequest) = createActivationSession(data.maxCount, data.selector)

    override fun createActivationSession(maxCount: Int, selector: String): Int {
        if (session != null) throw IllegalStateException("A session already exists!")
        //TODO: Create activation session
        return 1
    }

    @Path("cancel")
    @GET
    override fun cancelSession() {
        session = null
    }

    @Path("hasSession")
    @GET
    override fun hasSession(): Boolean {
        return session != null
    }

    @Path("remaining")
    @GET
    override fun getRemainingQuestionsCount(): Int {
        if (session == null) {
            throw IllegalStateException("No session exists.")
        }
        return session!!.size
    }

    @Path("question")
    @GET
    override fun currentQuestion(): Question {
        if (session == null) {
            throw IllegalStateException("No session exists.")
        }
        if (currentQuestion == null) {
            val opt = session!!.keys.stream().findAny()
            if (!opt.isPresent) {
                session = null
                throw IllegalStateException("No session exists.")
            }
            currentQuestion = opt.get()
        }
        return currentQuestion!!
    }

    @Path("answer")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    override fun answer(answer: String): VocabularyService.AnswerResult {
        //TODO: Implement answer checking. See markQuestion()
        return VocabularyService.AnswerResult(VocabularyService.ResultType.CORRECT, Word("hello", "EN"))
    }

    @Path("correction")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    override fun correction(correct: Boolean) {
        if (session == null) {
            throw IllegalStateException("No session exists.")
        }
        if (currentQuestion == null) {
            throw IllegalStateException("No current question.")
        }
        if (!isCorrecting) {
            throw IllegalStateException("Not correcting.")
        }
        markQuestion(correct)
    }

    private fun markQuestion(correct: Boolean) {
        //TODO: Advance to next question, remove correctly answered questions from queue
    }

    @Path("solution")
    @GET
    override fun getActivationSolution(): Word {
        if (session == null) throw IllegalStateException("No session exists.")
        if (currentQuestion == null) {
            val opt = session!!.keys.stream().findAny()
            if (!opt.isPresent) {
                session = null
                throw IllegalStateException("No session exists.")
            }
            currentQuestion = opt.get()
        }
        if (!isActivation) throw IllegalStateException("Not activating!")
        return session!![currentQuestion!!]!!
    }

    class CreateActivationSessionRequest() {
        var maxCount: Int = 0
        var selector: String = ""

        constructor(maxCount: Int, selector: String): this() {
            this.maxCount = maxCount
            this.selector = selector
        }
    }
}