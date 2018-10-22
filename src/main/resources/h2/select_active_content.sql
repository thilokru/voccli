SELECT a.question_id, q.question, q.question_language, q.answer, q.answer_language FROM Active as a
NATURAL JOIN Questions as q
WHERE a.next_due <= ?
ORDER BY RAND()
LIMIT ?;