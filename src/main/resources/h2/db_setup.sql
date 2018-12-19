CREATE TABLE IF NOT EXISTS Questions (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    section_id INT,
    question LONGVARCHAR NOT NULL,
    question_language CHAR(20) NOT NULL,
    answer LONGVARCHAR NOT NULL,
    answer_language CHAR(20) NOT NULL,
    FOREIGN KEY(section_id) REFERENCES Sections(section_id)
);
CREATE TABLE IF NOT EXISTS Associations (
    question_id INT NOT NULL,
    associationKey LONGVARCHAR NOT NULL,
    associationValue LONGVARCHAR,
    FOREIGN KEY(question_id) REFERENCES Questions(question_id)
);
CREATE TABLE IF NOT EXISTS Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username LONGVARCHAR UNIQUE NOT NULL,
    salt BINARY(16) NOT NULL,
    password_hash BINARY(64) NOT NULL
);
CREATE TABLE IF NOT EXISTS Vocabulary_State (
    user_id INT NOT NULL,
    question_id INT NOT NULL,
    next_due DATE NOT NULL,
    phase INT NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(user_id),
    FOREIGN KEY(question_id) REFERENCES Questions(question_id)
);
CREATE TABLE IF NOT EXISTS Sections (
    section_id INT AUTO_INCREMENT PRIMARY KEY,
    language LONGVARCHAR NOT NULL,
    res LONGVARCHAR NOT NULL,
    section LONGVARCHAR NOT NULL
);