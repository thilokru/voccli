CREATE TABLE IF NOT EXISTS Words {
    word_id integer AUTO_INCREMENT PRIMARY KEY,
    text longvarchar NOT NULL,
    language CHAR(20) NOT NULL,
    UNIQUE(text)
};
CREATE TABLE IF NOT EXISTS Vocable {
    vocable_id integer AUTO_INCREMENT PRIMARY KEY,
    source_word integer,
    target_word integer,
    next_due date,
    active integer CHECK(completed IN (1, 0)),
    FOREIGN KEY(source_word) REFERENCES Words(word_id),
    FOREIGN KEY(target_word) REFERENCES Words(word_id)
};
CREATE TABLE IF NOT EXISTS Associations {
    vocable_id integer,
    associationKey longvarchar NOT NULL,
    associationValue longvarchar,
    FOREIGN KEY(vocable_id) REFERENCES Vocable(vocable_id)
};