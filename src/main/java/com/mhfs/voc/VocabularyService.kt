package com.mhfs.voc

/**
 * This interface specifies the behaviour of the application providing the questions.
 * As this may later be implemented by a web service, and the user may be malicious, it is vital that all necessary data
 * is passed to the client, while keeping the state on the server, to prevent malicious tampering.
 *
 * If this API is called over network, then a connection loss will cancel any remaining sessions. Therefore, any newly
 * connected client can assume a blank slate and therefore requires fewer API calls.
 */

interface VocabularyService {

    /**
     * If this is implemented as a web application, then the following exceptions map to the following error codes:
     * {@code IllegalStateException} -> 409 Conflict
     * {@code IllegalArgumentException} -> 508 Loop Detected (in case of too long run times) or otherwise 406 Not Acceptable
     */
    companion object {
        const val ILLEGAL_STATE_STATUS = 409
        const val ILLEGAL_ARGUMENT_STATUS = 508
    }

    /**
     * Creates a new activation session.
     *
     * Note, that the selector may be interpreted as a script language or a regex. This could cause infinite loops or long run times.
     * It is advised to monitor the process or thread parsing the {@code selector} argument carefully. If the process fails,
     * an exception may be thrown, see below.
     * @throws IllegalStateException if a session already exists.
     * @throws IllegalArgumentException if the selector argument caused an exception. It should specify the precise cause.
     * @param description A object describing the desired session.
     * @return The state of the created session.
     */
    fun createSession(description: SessionDescription): State

    /**
     * Cancels the currently active session. Does nothing, if there is no active session.
     * A session is automatically canceled, if no further questions exist
     */
    fun cancelSession(): State

    /**
     * The state must fulfill the following conditions:
     * - There must be an active session.
     *
     * Note, that if a session exists, there automatically is an active question. If the last answer is given,
     * the session is terminated.
     *
     * Note, that checking weather an answer is correct or not, may not be as easy as comparing two strings:
     * Many times, vocabulary sets contain grammatical information hard to remember and learned by other means.
     *
     * If the answer was wrong, the state may not advance. To ensure the user learned the word correctly, the user
     * needs to reenter it.
     *
     * If the answer was correct, then the call will advance to the next question.
     *
     * @throws IllegalStateException if any of the above conditions fails
     *
     * @param answer The answer, which shall be checked
     * @return The current state, containing weather the answer was correct in {@link State.lastResult}
     */
    fun answer(answer: String): State

    /**
     * If the previous answer returned {@code ResultType.UNDETERMINED}, then the client calls this method to indicate
     * if the question was correct of not.
     * Independent of the argument, it will advance to the next question.
     * @throws IllegalStateException if the current state is not one with a valid session and the previous answer having the above state.
     * @param correct Indicating, if the previous answer was correct (true) or not (false).
     * @return The state of the session.
     */
    fun correction(correct: Boolean): State

    /**
     * If the other specified methods are called, it should not be necessary to call this method.
     * However, should the client lose the state, then it may call this method to retrieve it.
     * This method will not change the state and can therefore be called as many times as necessary.
     * @return The state of the session.
     */
    fun getState(): State

    /**
     * This describes the session a client wants to create when calling #createSession.
     * The fields assumed to be universal are {@code isActivation} and {@code maxCount}.
     * Implementers are encouraged to extend this class to convey additional information.
     */
    open class SessionDescription(val isActivation: Boolean, val maxCount: Int)

    class Result(val type: ResultType, val solution: String)

    /**
     * To reduce API calls (this may be done over REST-requests and may therefore require a lot of resources), this State
     * object shall be returned.
     */
    class State(currentQuestion: Question?, lastResult: Result?, remainingQuestions: Int) {
        /**
         * This field holds the next question. Its answer field may be null, indicating, that we currently are in a
         * vocabulary test. If it is null, the meaning depends on the states of the other fields.
         *
         * If currentQuestion is null, lastResult is null and remainingQuestions is 0, then there currently is no session.
         * If currentQuestion is null, but lastResult is in state WRONG, then the answer needs to be corrected.
         * If currentQuestion is null, but lastResult is in state UNDETERMINED, then the correctness of the answer must be reported.
         */
        val currentQuestion: Question? = currentQuestion

        /**
         * Holds information about weather the last answer was correct. Can be null, if no previous answer exists.
         */
        val lastResult: Result? = lastResult

        /**
         * Remaining questions.
         */
        val remainingQuestions: Int = remainingQuestions
    }

    /**
     * This class describes a question, including the question itself, its language, the target language,
     * if currently accssible to the user, the solution to the question and the possibility to provide additional
     * associated data.
     */
    open class Question(val question: String, val questionLanguage: String,
                   val solution: String?, val targetLanguage: String,
                   val associatedData: MutableMap<String, String>)

    enum class ResultType {CORRECT, WRONG, UNDETERMINED}
}