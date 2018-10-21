package com.mhfs.voc.cli

import com.mhfs.voc.VocabularyService
import com.mhfs.voc.VocabularyService.Companion.ILLEGAL_STATE_STATUS
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity

/**
 * Implements the client stub to access the REST Methods specified in {@link ServerAPIProvider}
 */
class RESTService(url: String): VocabularyService {

    private val client = ClientBuilder.newClient()
    private val target = client.target(url)
    private val sessionTarget = target.path("session")
    private val cancelTarget = target.path("cancel")
    private val stateTarget = target.path("state")
    private val answerTarget = target.path("answer")
    private val correctionTarget = target.path("correction")

    override fun createSession(description: VocabularyService.SessionDescription): VocabularyService.State {
        val requestBuilder = sessionTarget.request()
        val response = requestBuilder.post(Entity.json(description))
        if (response.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("A session already exists!")
        }
        return response.readEntity(VocabularyService.State::class.java)
    }

    override fun cancelSession(): VocabularyService.State {
        val result = cancelTarget.request().get()
        return result.readEntity(VocabularyService.State::class.java)
    }

    override fun getState(): VocabularyService.State {
        val answer = stateTarget.request().get()
        return answer.readEntity(VocabularyService.State::class.java)
    }

    override fun answer(answer: String): VocabularyService.State {
        val result = answerTarget.request().post(Entity.json(answer))
        if (result.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("No session exists.")
        }
        return result.readEntity(VocabularyService.State::class.java)
    }

    override fun correction(correct: Boolean): VocabularyService.State {
        val result = correctionTarget.request().post(Entity.json(correct))
        if (result.status == ILLEGAL_STATE_STATUS) {
            throw IllegalStateException("Previous answer not correctable.")
        }
        return result.readEntity(VocabularyService.State::class.java)
    }
}