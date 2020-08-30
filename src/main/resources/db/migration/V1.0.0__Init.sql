-- Artwork
CREATE TABLE IF NOT EXISTS `matchday`.`artwork` (
  `id` BIGINT NOT NULL,
  `created` DATETIME(6) NULL DEFAULT NULL,
  `file_name` VARCHAR(255) NULL DEFAULT NULL,
  `file_path` VARCHAR(255) NULL DEFAULT NULL,
  `file_size` BIGINT NULL DEFAULT NULL,
  `height` INT NOT NULL,
  `media_type` VARCHAR(255) NULL DEFAULT NULL,
  `modified` DATETIME(6) NULL DEFAULT NULL,
  `width` INT NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- Teams
CREATE TABLE IF NOT EXISTS `matchday`.`teams` (
  `team_id` VARCHAR(255) NOT NULL,
  `abbreviation` VARCHAR(255) NULL DEFAULT NULL,
  `locale` VARCHAR(255) NULL DEFAULT NULL,
  `name` VARCHAR(255) NULL DEFAULT NULL,
  `emblem_id` BIGINT NULL DEFAULT NULL,
  `fanart_id` BIGINT NULL DEFAULT NULL,
  PRIMARY KEY (`team_id`),
  INDEX `FKqitbrm8me52jusja57cqcu0sn` (`emblem_id` ASC) VISIBLE,
  INDEX `FKh6bp71x0jfncpdgkw3ox47iyf` (`fanart_id` ASC) VISIBLE,
  CONSTRAINT `FKh6bp71x0jfncpdgkw3ox47iyf`
    FOREIGN KEY (`fanart_id`)
    REFERENCES `matchday`.`artwork` (`id`),
  CONSTRAINT `FKqitbrm8me52jusja57cqcu0sn`
    FOREIGN KEY (`emblem_id`)
    REFERENCES `matchday`.`artwork` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- Competitions
CREATE TABLE IF NOT EXISTS `matchday`.`competitions` (
  `competition_id` VARCHAR(255) NOT NULL,
  `abbreviation` VARCHAR(255) NULL DEFAULT NULL,
  `locale` VARCHAR(255) NULL DEFAULT NULL,
  `name` VARCHAR(255) NULL DEFAULT NULL,
  `emblem_id` BIGINT NULL DEFAULT NULL,
  `fanart_id` BIGINT NULL DEFAULT NULL,
  `landscape_id` BIGINT NULL DEFAULT NULL,
  `monochrome_emblem_id` BIGINT NULL DEFAULT NULL,
  PRIMARY KEY (`competition_id`),
  INDEX `FK2n0w59wpt5uktyouggos3g9rc` (`emblem_id` ASC) VISIBLE,
  INDEX `FKibssjoxp55qmsyavsfrbugr4v` (`fanart_id` ASC) VISIBLE,
  INDEX `FKd3r0dug8iiyii0xtkb1wbsfc6` (`landscape_id` ASC) VISIBLE,
  INDEX `FKoxtcx8oy4tnx9tis0ynwxe8e2` (`monochrome_emblem_id` ASC) VISIBLE,
  CONSTRAINT `FK2n0w59wpt5uktyouggos3g9rc`
    FOREIGN KEY (`emblem_id`)
    REFERENCES `matchday`.`artwork` (`id`),
  CONSTRAINT `FKd3r0dug8iiyii0xtkb1wbsfc6`
    FOREIGN KEY (`landscape_id`)
    REFERENCES `matchday`.`artwork` (`id`),
  CONSTRAINT `FKibssjoxp55qmsyavsfrbugr4v`
    FOREIGN KEY (`fanart_id`)
    REFERENCES `matchday`.`artwork` (`id`),
  CONSTRAINT `FKoxtcx8oy4tnx9tis0ynwxe8e2`
    FOREIGN KEY (`monochrome_emblem_id`)
    REFERENCES `matchday`.`artwork` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- Fixtures
CREATE TABLE IF NOT EXISTS `matchday`.`fixtures` (
  `fixture_id` VARCHAR(255) NOT NULL,
  `fixture_number` INT NULL DEFAULT NULL,
  `title` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`fixture_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- Seasons
CREATE TABLE IF NOT EXISTS `matchday`.`seasons` (
  `season_id` VARCHAR(255) NOT NULL,
  `end_date` DATE NULL DEFAULT NULL,
  `start_date` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`season_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- Event
CREATE TABLE IF NOT EXISTS `matchday`.`event` (
  `event_id` VARCHAR(255) NOT NULL,
  `date` DATETIME(6) NULL DEFAULT NULL,
  `title` VARCHAR(255) NULL DEFAULT NULL,
  `competition_competition_id` VARCHAR(255) NULL DEFAULT NULL,
  `fixture_fixture_id` VARCHAR(255) NULL DEFAULT NULL,
  `season_season_id` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`event_id`),
  INDEX `FKp0qlfj6u4qa48b520y5bx8i4h` (`competition_competition_id` ASC) VISIBLE,
  INDEX `FKglv227hrriar9p9r0rl2rohp4` (`fixture_fixture_id` ASC) VISIBLE,
  INDEX `FKbynxvtb415hyu2bs7cbehkfw3` (`season_season_id` ASC) VISIBLE,
  CONSTRAINT `FKbynxvtb415hyu2bs7cbehkfw3`
    FOREIGN KEY (`season_season_id`)
    REFERENCES `matchday`.`seasons` (`season_id`),
  CONSTRAINT `FKglv227hrriar9p9r0rl2rohp4`
    FOREIGN KEY (`fixture_fixture_id`)
    REFERENCES `matchday`.`fixtures` (`fixture_id`),
  CONSTRAINT `FKp0qlfj6u4qa48b520y5bx8i4h`
    FOREIGN KEY (`competition_competition_id`)
    REFERENCES `matchday`.`competitions` (`competition_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- EventFileSource
CREATE TABLE IF NOT EXISTS `matchday`.`event_file_source` (
  `event_file_src_id` BINARY(255) NOT NULL,
  `approximate_duration` VARCHAR(255) NULL DEFAULT NULL,
  `audio_channels` INT NOT NULL,
  `audio_codec` VARCHAR(255) NULL DEFAULT NULL,
  `bitrate` BIGINT NULL DEFAULT NULL,
  `channel` VARCHAR(255) NULL DEFAULT NULL,
  `file_size` BIGINT NULL DEFAULT NULL,
  `frame_rate` INT NOT NULL,
  `media_container` VARCHAR(255) NULL DEFAULT NULL,
  `resolution` INT NULL DEFAULT NULL,
  `source` VARCHAR(255) NULL DEFAULT NULL,
  `video_codec` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`event_file_src_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- EventFileSources
CREATE TABLE IF NOT EXISTS `matchday`.`event_file_sources` (
  `event_event_id` VARCHAR(255) NOT NULL,
  `file_sources_event_file_src_id` BINARY(255) NOT NULL,
  UNIQUE INDEX `UK_dakuu81r709vh0k1qo4bcejrt` (`file_sources_event_file_src_id` ASC) VISIBLE,
  INDEX `FK32qvdypbk57j8h2syo7flmh4q` (`event_event_id` ASC) VISIBLE,
  CONSTRAINT `FK32qvdypbk57j8h2syo7flmh4q`
    FOREIGN KEY (`event_event_id`)
    REFERENCES `matchday`.`event` (`event_id`),
  CONSTRAINT `FKmnpg6lebg7gyy8k46im7wp82v`
    FOREIGN KEY (`file_sources_event_file_src_id`)
    REFERENCES `matchday`.`event_file_source` (`event_file_src_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- EventFile
CREATE TABLE IF NOT EXISTS `matchday`.`event_file` (
  `event_file_id` BIGINT NOT NULL,
  `external_url` VARCHAR(255) NULL DEFAULT NULL,
  `internal_url` LONGTEXT NULL DEFAULT NULL,
  `last_refreshed` DATETIME(6) NULL DEFAULT NULL,
  `metadata` LONGTEXT NULL DEFAULT NULL,
  `title` INT NULL DEFAULT NULL,
  PRIMARY KEY (`event_file_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- EventFileSource EventFiles
CREATE TABLE IF NOT EXISTS `matchday`.`event_file_source_event_files` (
  `event_file_source_event_file_src_id` BINARY(255) NOT NULL,
  `event_files_event_file_id` BIGINT NOT NULL,
  UNIQUE INDEX `UK_jxn8qvuddf4hm03b1u0dn2uob` (`event_files_event_file_id` ASC) VISIBLE,
  INDEX `FKa8sd7dsyggnmcjo1p2q5svmsv` (`event_file_source_event_file_src_id` ASC) VISIBLE,
  CONSTRAINT `FKa8sd7dsyggnmcjo1p2q5svmsv`
    FOREIGN KEY (`event_file_source_event_file_src_id`)
    REFERENCES `matchday`.`event_file_source` (`event_file_src_id`),
  CONSTRAINT `FKsgkdutwpr1arwhe2pu2b184ya`
    FOREIGN KEY (`event_files_event_file_id`)
    REFERENCES `matchday`.`event_file` (`event_file_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- EventFileSource Languages
CREATE TABLE IF NOT EXISTS `matchday`.`event_file_source_languages` (
  `event_file_source_event_file_src_id` BINARY(255) NOT NULL,
  `languages` VARCHAR(255) NULL DEFAULT NULL,
  INDEX `FK2ien3eb5kd785730gakh2wcmp` (`event_file_source_event_file_src_id` ASC) VISIBLE,
  CONSTRAINT `FK2ien3eb5kd785730gakh2wcmp`
    FOREIGN KEY (`event_file_source_event_file_src_id`)
    REFERENCES `matchday`.`event_file_source` (`event_file_src_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- Highlights
CREATE TABLE IF NOT EXISTS `matchday`.`highlights` (
  `event_id` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`event_id`),
  CONSTRAINT `FKbaiqg01iiuq767cjj1wye5lob`
    FOREIGN KEY (`event_id`)
    REFERENCES `matchday`.`event` (`event_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- Matches
CREATE TABLE IF NOT EXISTS `matchday`.`matches` (
  `event_id` VARCHAR(255) NOT NULL,
  `away_team_id` VARCHAR(255) NULL DEFAULT NULL,
  `home_team_id` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`event_id`),
  INDEX `FK2e8erbfecb0tjtq9iudg36bxu` (`away_team_id` ASC) VISIBLE,
  INDEX `FK8k68nekawp47js52dq8720voe` (`home_team_id` ASC) VISIBLE,
  CONSTRAINT `FK232ts2rxn39osyh1euvgwrech`
    FOREIGN KEY (`event_id`)
    REFERENCES `matchday`.`event` (`event_id`),
  CONSTRAINT `FK2e8erbfecb0tjtq9iudg36bxu`
    FOREIGN KEY (`away_team_id`)
    REFERENCES `matchday`.`teams` (`team_id`),
  CONSTRAINT `FK8k68nekawp47js52dq8720voe`
    FOREIGN KEY (`home_team_id`)
    REFERENCES `matchday`.`teams` (`team_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- FileServerUser
CREATE TABLE IF NOT EXISTS `matchday`.`file_server_user` (
  `user_id` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NULL DEFAULT NULL,
  `logged_in` BIT(1) NOT NULL,
  `password` VARCHAR(255) NULL DEFAULT NULL,
  `server_id` VARCHAR(255) NULL DEFAULT NULL,
  `user_name` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- SecureCookie
CREATE TABLE IF NOT EXISTS `matchday`.`secure_cookie` (
  `id` BIGINT NOT NULL,
  `domain` VARCHAR(255) NULL DEFAULT NULL,
  `http_only` BIT(1) NOT NULL,
  `max_age` BIGINT NULL DEFAULT NULL,
  `name` VARCHAR(255) NULL DEFAULT NULL,
  `path` VARCHAR(255) NULL DEFAULT NULL,
  `same_site` VARCHAR(255) NULL DEFAULT NULL,
  `secure` BIT(1) NOT NULL,
  `value` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- FileServerUser Cookies
CREATE TABLE IF NOT EXISTS `matchday`.`file_server_user_cookies` (
  `file_server_user_user_id` VARCHAR(255) NOT NULL,
  `cookies_id` BIGINT NOT NULL,
  UNIQUE INDEX `UK_6ghaw1c3cqv4lhkleqdcmegb2` (`cookies_id` ASC) VISIBLE,
  INDEX `FKj4y244w4om91bo6i15vrxnmu7` (`file_server_user_user_id` ASC) VISIBLE,
  CONSTRAINT `FK6emxl5asofqob2lfgn23j6ogr`
    FOREIGN KEY (`cookies_id`)
    REFERENCES `matchday`.`secure_cookie` (`id`),
  CONSTRAINT `FKj4y244w4om91bo6i15vrxnmu7`
    FOREIGN KEY (`file_server_user_user_id`)
    REFERENCES `matchday`.`file_server_user` (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- VideoStreamLocator
CREATE TABLE IF NOT EXISTS `matchday`.`video_stream_playlist_locator` (
  `event_id` VARCHAR(255) NOT NULL,
  `file_src_id` BINARY(255) NOT NULL,
  `playlist_path` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`event_id`, `file_src_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;