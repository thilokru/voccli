package com.mhfs.voc

/**
 * This interface specifies the behaviour of the application providing the questions.
 * As this may later be implemented by a web service, and the user may be malicious, it is vital that all necessary data
 * is passed to the client, while keeping the state on the server, to prevent malicious tampering.
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
     * Creates a new session with questions.
     * @throws IllegalStateException if a session already exists.
     * @param maxCount The maximum amount of questions in the session. Negative values indicate no limit.
     * @return the number of actual questions in the session
     */
    fun createSession(maxCount: Int): Int

    /**
     * Creates a new activation session.
     *
     * Note, that the selector may be interpreted as a script language or a regex. This could cause infinite loops or long run times.
     * It is advised to monitor the process or thread parsing the {@code selector} argument carefully. If the process fails,
     * an exception may be thrown, see below.
     * @throws IllegalStateException if a session already exists.
     * @throws IllegalArgumentException if the selector argument caused an exception. It should specify the precise cause.
     * @param maxCount The maximum amount of questions in the session. Negative values indicate no limit.
     * @param selector Contains information about what questions to activate.
     */
    fun createActivationSession(maxCount: Int, selector: String): Int

    /**
     * Cancels the currently active session. Does nothing, if there is no active session.
     * A session is automatically canceled, if no further questions exist
     */
    fun cancelSession()

    /**
     * Checks if there is currently an active session
     * @return if there is a session.
     */
    fun hasSession(): Boolean

    /**
     * Returns the amount of remaining questions
     * @throws IllegalStateException if no session exists
     * @return the number of remaining questions
     */
    fun getRemainingQuestionsCount(): Int

    /**
     * Returns the next question in the session, if one is available
     * @throws IllegalStateException if no session exists.
     */
    fun currentQuestion(): Question

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
     * @throws IllegalStateException if any of the above conditions fails
     *
     * @param answer The answer, which shall be checked
     * @return An AnswerResult, indicating if the answer was correct or wrong or needs user interaction
     */
    fun answer(answer: String): AnswerResult

    class AnswerResult(val result: ResultType, val correctAnswer: Word)
    enum class ResultType {CORRECT, WRONG, UNDETERMINED}

    /**
     * If the previous answer returned {@code ResultType.UNDETERMINED}, then the client calls this method to indicate
     * if the question was correct of not.
     * @throws IllegalStateException if the current state is not one with a valid session and the previous answer having the above state.
     * @param correct Indicating, if the previous answer was correct (true) or not (false).
     */
    fun correction(correct: Boolean)

    /**
     * If the session is a activation session, see {@code com.mhfs.voc.Session.isActivation}, then the client may ask the solution to a question.
     * @throws IllegalStateException if there is no current session or it is not an activation session or if no current question exists.
     * @return The solution to the current question
     */
    fun getActivationSolution(): Word
}