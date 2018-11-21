package com.mhfs.voc.cli

import com.mhfs.voc.VocabularyService
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import javax.net.ssl.SSLContext

/**
 * Implements the client stub to access the REST Methods specified in {@link ServerAPIProvider}
 */
class RESTService(url: String): VocabularyService {

    private val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        engine {
            sslContext = SSLContext.getDefault()
            followRedirects = true
        }
    }

    private val sessionURL = "$url/api/v1/session"
    private val cancelURL = "$url/api/v1/cancel"
    private val stateURL = "$url/api/v1/state"
    private val answerURL = "$url/api/v1/answer"
    private val correctionURL = "$url/api/v1/answer"

    override fun createSession(description: VocabularyService.SessionDescription): VocabularyService.State {
        lateinit var state: VocabularyService.State
        runBlocking {
            state = client.post(sessionURL, body = description)
        }
        return state
    }

    override fun cancelSession(): VocabularyService.State {
        lateinit var state: VocabularyService.State
        runBlocking {
            state = client.post(cancelURL)
        }
        return state
    }

    override fun getState(): VocabularyService.State {
        lateinit var state: VocabularyService.State
        runBlocking {
            state = client.get(stateURL)
        }
        return state
    }

    override fun answer(answer: String): VocabularyService.State {
        lateinit var state: VocabularyService.State
        runBlocking {
            state = client.post(answerURL, body = answer)
        }
        return state
    }

    override fun correction(correct: Boolean): VocabularyService.State {
        lateinit var state: VocabularyService.State
        runBlocking {
            state = client.post(correctionURL, body = correct)
        }
        return state
    }
}