MERGE INTO Associations(question_id, associationKey, associationValue) KEY(question_id, associationKey)
VALUES(?, ?, ?);