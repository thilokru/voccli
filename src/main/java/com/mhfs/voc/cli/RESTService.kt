package com.mhfs.voc.cli

import com.mhfs.voc.Question
import com.mhfs.voc.VocabularyService
import com.mhfs.voc.VocabularyService.Companion.ILLEGAL_ARGUMENT_STATUS
import com.mhfs.voc.VocabularyService.Companion.ILLEGAL_STATE_STATUS
import com.mhfs.voc.Word
import com.mhfs.voc.server.ServerAPIProvider
import java.io.IOException
import java.lang.IllegalArgumentException

import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

/**
 * Implements the client stub to access the REST Methods specified in {@link ServerAPIProvider}
 */
class RESTService(url: String): VocabularyService {

    private val client = ClientBuilder.newClient()
    private val target = client.target(url)
    private val sessionTarget = target.path("session")
    private val activationSessionTarget = target.path("activationSession")
    private val cancelTarget = target.path("cancel")
    private val hasSessionTarget = target.path("hasSession")
    private val remainingQuestionsTarget = target.path("remaining")
    private val currentQuestionTarget = target.path("question")
    private val answerTarget = target.path("answer")
    private val correctionTarget = target.path("correction")
    private val solutionTarget = target.path("solution")

    override fun createSession(maxCount: Int): Int {
        val requestBuilder = sessionTarget.queryParam("maxCount", maxCount.toString()).request()
        val response = requestBuilder.post(Entity.json(maxCount))
        if (response.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("A session already exists!")
        }
        return response.readEntity(Int::class.java)
    }

    override fun createActivationSession(maxCount: Int, selector: String): Int {
        val requestBuilder = activationSessionTarget.request()
        val response = requestBuilder.post(Entity.json(ServerAPIProvider.CreateActivationSessionRequest(maxCount, selector)))
        if (response.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("A session already exists!")
        } else if (response.status == ILLEGAL_ARGUMENT_STATUS) {
            throw IllegalArgumentException("The selector was malformed.")
        }
        return response.readEntity(Int::class.java)
    }

    override fun cancelSession() {
        cancelTarget.request().get()
    }

    override fun hasSession(): Boolean {
        return hasSessionTarget.request().get().readEntity(Boolean::class.java)
    }

    override fun getRemainingQuestionsCount(): Int {
        val answer = remainingQuestionsTarget.request().get()
        if (answer.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("No session exists.")
        }
        return answer.readEntity(Int::class.java)
    }

    override fun currentQuestion(): Question {
        val answer = currentQuestionTarget.request().get()
        if (answer.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("No session exists.")
        }
        return answer.readEntity(Question::class.java)
    }

    override fun answer(answer: String): VocabularyService.AnswerResult {
        val result = answerTarget.request().post(Entity.json(answer))
        if (result.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("No session exists.")
        }
        return result.readEntity(VocabularyService.AnswerResult::class.java)
    }

    override fun correction(correct: Boolean) {
        val result = correctionTarget.request().post(Entity.json(correct))
        if (result.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("Previous answer not correctable.")
        }
    }

    override fun getActivationSolution(): Word {
        val result = solutionTarget.request().get()
        if (result.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("Couldn't retrieve activation solution!")
        }
        return result.readEntity(Word::class.java)
    }
}