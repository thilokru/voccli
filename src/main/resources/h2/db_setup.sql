CREATE TABLE IF NOT EXISTS Questions (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    question LONGVARCHAR NOT NULL,
    question_language CHAR(20) NOT NULL,
    answer LONGVARCHAR NOT NULL,
    answer_language CHAR(20) NOT NULL
);
CREATE TABLE IF NOT EXISTS Associations (
    question_id INT NOT NULL,
    associationKey LONGVARCHAR NOT NULL,
    associationValue LONGVARCHAR,
    FOREIGN KEY(question_id) REFERENCES Questions(question_id)
);
CREATE TABLE IF NOT EXISTS Active (
    question_id INT NOT NULL,
    next_due DATE NOT NULL,
    FOREIGN KEY(question_id) REFERENCES Questions(question_id)
);
CREATE TABLE IF NOT EXISTS Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username LONGVARCHAR UNIQUE NOT NULL,
    salt BINARY(16) NOT NULL,
    password_hash BINARY(64) NOT NULL
);
