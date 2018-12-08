SELECT v.question_id, q.question, q.question_language, q.answer, q.answer_language, v.phase FROM Vocabulary_State as v
NATURAL JOIN Questions as q
WHERE v.user_id = ? AND v.next_due <= ?
ORDER BY RAND()
LIMIT ?;