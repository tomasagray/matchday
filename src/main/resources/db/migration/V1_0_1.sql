# Change Synonym ID type from Long -> MD5 hash string
# ---
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE synonym
    ADD tmp_hash VARCHAR(32) NULL;

UPDATE synonym
SET name     = TRIM(name),
    tmp_hash = MD5(TRIM(name));

ALTER TABLE proper_name_synonyms
    DROP FOREIGN KEY `FKf0l31227lytyfh3gg2e4oo9df`,
    DROP FOREIGN KEY `fk_pronamsyn_on_synonym`;

ALTER TABLE synonym
    DROP PRIMARY KEY,
    MODIFY id VARCHAR(32),
    ADD PRIMARY KEY (id);

ALTER TABLE proper_name_synonyms
    MODIFY synonyms_id VARCHAR(32),
    ADD FOREIGN KEY `FKf0l31227lytyfh3gg2e4oo9df` (synonyms_id)
        REFERENCES synonym (id);

UPDATE proper_name_synonyms pns
    JOIN synonym s on s.id = pns.synonyms_id
SET synonyms_id = s.tmp_hash;

UPDATE synonym
SET id = tmp_hash;

ALTER TABLE synonym
    DROP COLUMN tmp_hash;

ALTER TABLE proper_name_synonyms
    ADD CONSTRAINT `FKf0l31227lytyfh3gg2e4oo9df`
        FOREIGN KEY (synonyms_id) REFERENCES synonym (id),
    ADD CONSTRAINT `fk_pronamsyn_on_synonym`
        FOREIGN KEY (synonyms_id) REFERENCES synonym (id);

SET FOREIGN_KEY_CHECKS = 1;
