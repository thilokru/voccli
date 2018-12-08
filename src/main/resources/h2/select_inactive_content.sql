SELECT question_id, question, question_language, answer, answer_language FROM Questions
WHERE question_id NOT IN (SELECT question_id FROM Vocabulary_State WHERE user_id = ?)
ORDER BY RAND()
LIMIT ?;