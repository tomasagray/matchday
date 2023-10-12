CREATE TABLE artwork
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created    datetime              NULL,
    file       VARCHAR(255)          NULL,
    file_size  BIGINT                NULL,
    height     INT                   NOT NULL,
    media_type VARCHAR(255)          NULL,
    modified   datetime              NULL,
    width      INT                   NOT NULL,
    CONSTRAINT PK_ARTWORK PRIMARY KEY (id)
);

CREATE TABLE artwork_collection
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    `role`         INT                   NULL,
    selected_index INT                   NOT NULL,
    CONSTRAINT PK_ARTWORK_COLLECTION PRIMARY KEY (id)
);

CREATE TABLE artwork_collection_collection
(
    artwork_collection_id BIGINT NOT NULL,
    collection_id         BIGINT NOT NULL,
    CONSTRAINT PK_ARTWORK_COLLECTION_COLLECTION PRIMARY KEY (artwork_collection_id, collection_id),
    UNIQUE (collection_id)
);

CREATE TABLE competition
(
    id           VARCHAR(255) NOT NULL,
    country_name VARCHAR(255) NULL,
    emblem_id    BIGINT       NULL,
    fanart_id    BIGINT       NULL,
    name_id      BIGINT       NULL,
    CONSTRAINT PK_COMPETITION PRIMARY KEY (id)
);

CREATE TABLE country
(
    name           VARCHAR(255) NOT NULL,
    flag_file_name VARCHAR(255) NULL,
    CONSTRAINT PK_COUNTRY PRIMARY KEY (name)
);

CREATE TABLE country_locales
(
    country_name VARCHAR(255) NOT NULL,
    locales      VARCHAR(255) NULL
);

CREATE TABLE data_source
(
    data_source_id VARCHAR(255) NOT NULL,
    base_uri       VARCHAR(255) NULL,
    clazz          VARCHAR(255) NULL,
    enabled        BIT(1)       NOT NULL,
    plugin_id      VARCHAR(255) NULL,
    title          VARCHAR(255) NULL,
    CONSTRAINT PK_DATA_SOURCE PRIMARY KEY (data_source_id)
);

CREATE TABLE event
(
    event_id       VARCHAR(255) NOT NULL,
    date           datetime     NULL,
    fixture_number INT          NULL,
    title          VARCHAR(255) NULL,
    end_date       date         NULL,
    start_date     date         NULL,
    artwork_id     BIGINT       NULL,
    competition_id VARCHAR(255) NULL,
    CONSTRAINT PK_EVENT PRIMARY KEY (event_id)
);

CREATE TABLE event_file_sources
(
    event_event_id           VARCHAR(255) NOT NULL,
    file_sources_file_src_id VARCHAR(255) NOT NULL,
    CONSTRAINT PK_EVENT_FILE_SOURCES PRIMARY KEY (event_event_id, file_sources_file_src_id),
    UNIQUE (file_sources_file_src_id)
);

CREATE TABLE file_server_user
(
    user_id   VARCHAR(255) NOT NULL,
    email     TEXT         NULL,
    logged_in BIT(1)       NOT NULL,
    password  TEXT         NULL,
    server_id VARCHAR(255) NULL,
    username  TEXT         NULL,
    CONSTRAINT PK_FILE_SERVER_USER PRIMARY KEY (user_id)
);

CREATE TABLE file_server_user_cookies
(
    file_server_user_user_id VARCHAR(255) NOT NULL,
    cookies_id               BIGINT       NOT NULL,
    UNIQUE (cookies_id)
);

CREATE TABLE hibernate_sequence
(
    next_val BIGINT NULL
);

CREATE TABLE highlight
(
    event_id VARCHAR(255) NOT NULL,
    CONSTRAINT PK_HIGHLIGHT PRIMARY KEY (event_id)
);

CREATE TABLE highlight_file_sources
(
    highlight_event_id       CHAR(36) NOT NULL,
    file_sources_file_src_id CHAR(36) NOT NULL,
    CONSTRAINT PK_HIGHLIGHT_FILE_SOURCES PRIMARY KEY (highlight_event_id, file_sources_file_src_id),
    UNIQUE (file_sources_file_src_id)
);

CREATE TABLE match_game
(
    event_id     VARCHAR(255) NOT NULL,
    away_team_id VARCHAR(255) NOT NULL,
    home_team_id VARCHAR(255) NOT NULL,
    CONSTRAINT PK_MATCH_GAME PRIMARY KEY (event_id)
);

CREATE TABLE match_game_file_sources
(
    match_game_event_id      CHAR(36) NOT NULL,
    file_sources_file_src_id CHAR(36) NOT NULL,
    CONSTRAINT PK_MATCH_GAME_FILE_SOURCES PRIMARY KEY (match_game_event_id, file_sources_file_src_id),
    UNIQUE (file_sources_file_src_id)
);

CREATE TABLE pattern_kit
(
    id      BIGINT AUTO_INCREMENT NOT NULL,
    clazz   VARCHAR(255)          NULL,
    pattern LONGTEXT              NULL,
    CONSTRAINT PK_PATTERN_KIT PRIMARY KEY (id)
);

CREATE TABLE pattern_kit_fields
(
    pattern_kit_id BIGINT       NOT NULL,
    fields         VARCHAR(255) NULL,
    fields_key     INT          NOT NULL,
    CONSTRAINT PK_PATTERN_KIT_FIELDS PRIMARY KEY (pattern_kit_id, fields_key)
);

CREATE TABLE pattern_kit_template
(
    id   BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(255)          NULL,
    type VARCHAR(255)          NULL,
    CONSTRAINT PK_PATTERN_KIT_TEMPLATE PRIMARY KEY (id)
);

CREATE TABLE pattern_kit_template_fields
(
    pattern_kit_template_id BIGINT       NOT NULL,
    field_name              VARCHAR(255) NULL,
    required                BIT(1)       NULL
);

CREATE TABLE pattern_kit_template_related_templates
(
    pattern_kit_template_id BIGINT NOT NULL,
    related_templates_id    BIGINT NOT NULL,
    UNIQUE (related_templates_id)
);

CREATE TABLE plaintext_data_source
(
    data_source_id VARCHAR(255) NOT NULL,
    CONSTRAINT PK_PLAINTEXT_DATA_SOURCE PRIMARY KEY (data_source_id)
);

CREATE TABLE plaintext_data_source_pattern_kits
(
    plaintext_data_source_data_source_id VARCHAR(255) NOT NULL,
    pattern_kits_id                      BIGINT       NOT NULL,
    UNIQUE (pattern_kits_id)
);

CREATE TABLE proper_name
(
    id   BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(255)          NULL,
    CONSTRAINT PK_PROPER_NAME PRIMARY KEY (id)
);

CREATE TABLE proper_name_synonyms
(
    proper_name_id BIGINT      NOT NULL,
    synonyms_id    VARCHAR(32) NULL,
    UNIQUE (synonyms_id)
);

CREATE TABLE restore_point
(
    id                     VARCHAR(255) NOT NULL,
    backup_archive         VARCHAR(255) NULL,
    competition_count      INT          NULL,
    data_source_count      INT          NULL,
    event_count            INT          NULL,
    file_server_user_count INT          NULL,
    filesize               BIGINT       NULL,
    team_count             INT          NULL,
    timestamp              BIGINT       NULL,
    CONSTRAINT PK_RESTORE_POINT PRIMARY KEY (id)
);

CREATE TABLE secure_cookie
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    cookie_value LONGTEXT              NULL,
    domain       VARCHAR(255)          NULL,
    http_only    BIT(1)                NOT NULL,
    max_age      BIGINT                NULL,
    name         VARCHAR(255)          NULL,
    `path`       VARCHAR(255)          NULL,
    same_site    VARCHAR(255)          NULL,
    secure       BIT(1)                NOT NULL,
    CONSTRAINT PK_SECURE_COOKIE PRIMARY KEY (id)
);

CREATE TABLE settings
(
    id                       BIGINT AUTO_INCREMENT NOT NULL,
    artwork_storage_location VARCHAR(255)          NULL,
    backup_location          VARCHAR(255)          NULL,
    log_filename             VARCHAR(255)          NULL,
    prune_videos             VARCHAR(255)          NULL,
    refresh_events           VARCHAR(255)          NULL,
    timestamp                datetime              NULL,
    video_expired_days       INT                   NOT NULL,
    video_storage_location   VARCHAR(255)          NULL,
    CONSTRAINT PK_SETTINGS PRIMARY KEY (id)
);

CREATE TABLE single_stream_locator
(
    stream_locator_id BIGINT NOT NULL,
    CONSTRAINT PK_SINGLE_STREAM_LOCATOR PRIMARY KEY (stream_locator_id)
);

CREATE TABLE stream_job_state
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    completion_ratio DOUBLE                NULL,
    error            LONGTEXT              NULL,
    status           INT                   NULL,
    CONSTRAINT PK_STREAM_JOB_STATE PRIMARY KEY (id)
);

CREATE TABLE synonym
(
    id   VARCHAR(32)  NOT NULL,
    name VARCHAR(255) NULL,
    CONSTRAINT PK_SYNONYM PRIMARY KEY (id)
);

CREATE TABLE task_list_state
(
    id BIGINT NOT NULL,
    CONSTRAINT PK_TASK_LIST_STATE PRIMARY KEY (id)
);

CREATE TABLE task_list_state_task_states
(
    task_list_state_id BIGINT NOT NULL,
    task_states_id     BIGINT NOT NULL,
    UNIQUE (task_states_id)
);

CREATE TABLE task_state
(
    id BIGINT NOT NULL,
    CONSTRAINT PK_TASK_STATE PRIMARY KEY (id)
);

CREATE TABLE team
(
    id           VARCHAR(255) NOT NULL,
    country_name VARCHAR(255) NULL,
    emblem_id    BIGINT       NULL,
    fanart_id    BIGINT       NULL,
    name_id      BIGINT       NULL,
    CONSTRAINT PK_TEAM PRIMARY KEY (id)
);

CREATE TABLE team_colors
(
    team_id VARCHAR(255)  NOT NULL,
    alpha   INT DEFAULT 0 NOT NULL,
    blue    INT DEFAULT 0 NOT NULL,
    green   INT DEFAULT 0 NOT NULL,
    red     INT DEFAULT 0 NOT NULL
);

CREATE TABLE video_file
(
    file_id        VARCHAR(255) NOT NULL,
    external_url   VARCHAR(255) NULL,
    internal_url   LONGTEXT     NULL,
    last_refreshed BIGINT       NULL,
    metadata       LONGTEXT     NULL,
    title          INT          NULL,
    CONSTRAINT PK_VIDEO_FILE PRIMARY KEY (file_id)
);

CREATE TABLE video_file_pack
(
    id VARCHAR(255) NOT NULL,
    CONSTRAINT PK_VIDEO_FILE_PACK PRIMARY KEY (id)
);

CREATE TABLE video_file_pack_video_files
(
    video_file_pack_id  VARCHAR(255) NOT NULL,
    video_files_file_id VARCHAR(255) NOT NULL,
    pack_id             INT          NOT NULL,
    CONSTRAINT PK_VIDEO_FILE_PACK_VIDEO_FILES PRIMARY KEY (video_file_pack_id, pack_id),
    UNIQUE (video_files_file_id)
);

CREATE TABLE video_file_source
(
    file_src_id          VARCHAR(255) NOT NULL,
    approximate_duration VARCHAR(255) NULL,
    audio_bitrate        BIGINT       NULL,
    audio_channels       VARCHAR(255) NULL,
    audio_codec          VARCHAR(255) NULL,
    channel              VARCHAR(255) NULL,
    filesize             BIGINT       NULL,
    framerate            INT          NOT NULL,
    languages            VARCHAR(255) NULL,
    media_container      VARCHAR(255) NULL,
    resolution           INT          NULL,
    source               VARCHAR(255) NULL,
    video_bitrate        BIGINT       NULL,
    video_codec          VARCHAR(255) NULL,
    CONSTRAINT PK_VIDEO_FILE_SOURCE PRIMARY KEY (file_src_id)
);

CREATE TABLE video_file_source_video_file_packs
(
    video_file_source_file_src_id VARCHAR(255) NOT NULL,
    video_file_packs_id           VARCHAR(255) NOT NULL,
    UNIQUE (video_file_packs_id)
);

CREATE TABLE video_stream_locator
(
    stream_locator_id  BIGINT       NOT NULL,
    playlist_path      VARCHAR(255) NULL,
    timestamp          datetime     NULL,
    state_id           BIGINT       NULL,
    video_file_file_id VARCHAR(255) NULL,
    CONSTRAINT PK_VIDEO_STREAM_LOCATOR PRIMARY KEY (stream_locator_id)
);

CREATE TABLE video_stream_locator_playlist
(
    id                      BIGINT       NOT NULL,
    storage_location        VARCHAR(255) NULL,
    timestamp               datetime     NULL,
    file_source_file_src_id VARCHAR(255) NULL,
    state_id                BIGINT       NULL,
    CONSTRAINT PK_VIDEO_STREAM_LOCATOR_PLAYLIST PRIMARY KEY (id)
);

CREATE TABLE video_stream_locator_playlist_stream_locators
(
    video_stream_locator_playlist_id  BIGINT NOT NULL,
    stream_locators_stream_locator_id BIGINT NOT NULL,
    UNIQUE (stream_locators_stream_locator_id)
);

CREATE INDEX FK1u5lmbivs7ut9hp6776lvu14t ON video_stream_locator_playlist (file_source_file_src_id);

CREATE INDEX FK3rgkgnn6l57r7uu86s1yxxoqq ON team (name_id);

CREATE INDEX FK_COMPETITION_ON_COUNTRY_NAME ON competition (country_name);

CREATE INDEX FK_COMPETITION_ON_EMBLEM ON competition (emblem_id);

CREATE INDEX FK_COMPETITION_ON_FANART ON competition (fanart_id);

CREATE INDEX FK_EVENT_ON_ARTWORK ON event (artwork_id);

CREATE INDEX FK_EVENT_ON_COMPETITION ON event (competition_id);

CREATE INDEX FK_MATCH_GAME_ON_AWAY_TEAM ON match_game (away_team_id);

CREATE INDEX FK_MATCH_GAME_ON_HOME_TEAM ON match_game (home_team_id);

CREATE INDEX FK_TEAM_ON_COUNTRY_NAME ON team (country_name);

CREATE INDEX FK_TEAM_ON_EMBLEM ON team (emblem_id);

CREATE INDEX FK_TEAM_ON_FANART ON team (fanart_id);

CREATE INDEX FK_VIDEO_STREAM_LOCATOR_PLAYLIST_ON_STATE ON video_stream_locator_playlist (state_id);

CREATE INDEX FK_VIDEO_STREAM_LOCATOR_ON_STATE ON video_stream_locator (state_id);

CREATE INDEX FK_VIDEO_STREAM_LOCATOR_ON_VIDEO_FILE_FILE_ID ON video_stream_locator (video_file_file_id);

CREATE INDEX FKsm71y25lvi9xjp3gnqbdqxalm ON competition (name_id);

CREATE INDEX fk_country_locales_on_country ON country_locales (country_name);

CREATE INDEX fk_filserusecoo_on_file_server_user ON file_server_user_cookies (file_server_user_user_id);

CREATE INDEX fk_patkittemreltem_on_patternkittemplate ON pattern_kit_template_related_templates (pattern_kit_template_id);

CREATE INDEX fk_patternkittemplate_fields_on_pattern_kit_template ON pattern_kit_template_fields (pattern_kit_template_id);

CREATE INDEX fk_pladatsoupatkit_on_plaintext_data_source ON plaintext_data_source_pattern_kits (plaintext_data_source_data_source_id);

CREATE INDEX fk_pronamsyn_on_proper_name ON proper_name_synonyms (proper_name_id);

CREATE INDEX fk_taslisstatassta_on_task_list_state ON task_list_state_task_states (task_list_state_id);

CREATE INDEX fk_team_colors_on_team ON team_colors (team_id);

CREATE INDEX fk_vidfilsouvidfilpac_on_video_file_source ON video_file_source_video_file_packs (video_file_source_file_src_id);

CREATE INDEX fk_vidstrlocplastrloc_on_video_stream_locator_playlist ON video_stream_locator_playlist_stream_locators (video_stream_locator_playlist_id);

ALTER TABLE video_stream_locator_playlist
    ADD CONSTRAINT FK1u5lmbivs7ut9hp6786lvu14t FOREIGN KEY (file_source_file_src_id) REFERENCES video_file_source (file_src_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE competition
    ADD CONSTRAINT FK30vs730j9vnx89th8tlv94b7w FOREIGN KEY (country_name) REFERENCES country (name) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE event_file_sources
    ADD CONSTRAINT FK32qvdypbk57j8h2syo7flmh4q FOREIGN KEY (event_event_id) REFERENCES event (event_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE team
    ADD CONSTRAINT FK3rgkgnn6l57r7uu86s1yxxoqq FOREIGN KEY (name_id) REFERENCES proper_name (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_stream_locator
    ADD CONSTRAINT FK41kj2to96u3estg7wuym1t7x2 FOREIGN KEY (state_id) REFERENCES task_state (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE match_game
    ADD CONSTRAINT FK45wbruxytdwgevr6j7uiygni6 FOREIGN KEY (away_team_id) REFERENCES team (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_stream_locator_playlist_stream_locators
    ADD CONSTRAINT FK4v1q4jsdry74em6b60ynuhrjl FOREIGN KEY (video_stream_locator_playlist_id) REFERENCES video_stream_locator_playlist (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE highlight
    ADD CONSTRAINT FK5j6u1t55mjfa3yqwbivjonw2g FOREIGN KEY (event_id) REFERENCES event (event_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE event_file_sources
    ADD CONSTRAINT FK6r97wciosdj9xv6gbesqr0d7b FOREIGN KEY (file_sources_file_src_id) REFERENCES video_file_source (file_src_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE competition
    ADD CONSTRAINT FK6rf31h39fut85b1ip4s7qblf1 FOREIGN KEY (emblem_id) REFERENCES artwork_collection (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE match_game
    ADD CONSTRAINT FK6snshxj2fp0u6m8w0wh28q4hc FOREIGN KEY (event_id) REFERENCES event (event_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_stream_locator
    ADD CONSTRAINT FK7jfepb9e9tqug0ir6t9xbggod FOREIGN KEY (video_file_file_id) REFERENCES video_file (file_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE event
    ADD CONSTRAINT FK85f41n4jedyyklje848aiaet3 FOREIGN KEY (artwork_id) REFERENCES artwork (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE task_list_state
    ADD CONSTRAINT FK9m9bvje33opw6cgjcxjflj3rf FOREIGN KEY (id) REFERENCES stream_job_state (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE event
    ADD CONSTRAINT FK_EVENT_ON_COMPETITION FOREIGN KEY (competition_id) REFERENCES competition (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE match_game
    ADD CONSTRAINT FK_MATCH_GAME_ON_HOME_TEAM FOREIGN KEY (home_team_id) REFERENCES team (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE plaintext_data_source
    ADD CONSTRAINT FK_PLAIN_TEXT_DATA_SOURCE_ON_DATA_SOURCE_ID FOREIGN KEY (data_source_id) REFERENCES data_source (data_source_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE task_state
    ADD CONSTRAINT FK_TASK_STATE_ON_ID FOREIGN KEY (id) REFERENCES stream_job_state (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE team
    ADD CONSTRAINT FK_TEAM_ON_COUNTRY_NAME FOREIGN KEY (country_name) REFERENCES country (name) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE team
    ADD CONSTRAINT FK_TEAM_ON_EMBLEM FOREIGN KEY (emblem_id) REFERENCES artwork_collection (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE competition
    ADD CONSTRAINT FKb9lw9c3l3nv89n2tfwoouvom7 FOREIGN KEY (fanart_id) REFERENCES artwork_collection (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE team
    ADD CONSTRAINT FKcp07ddjgud2g0sm7exgjt0b5q FOREIGN KEY (fanart_id) REFERENCES artwork_collection (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE plaintext_data_source_pattern_kits
    ADD CONSTRAINT FKe6ps13c9hodidxa0fg3ccomg8 FOREIGN KEY (pattern_kits_id) REFERENCES pattern_kit (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE pattern_kit_template_related_templates
    ADD CONSTRAINT FKg61g3brg68shwxdd9hccy29e8 FOREIGN KEY (pattern_kit_template_id) REFERENCES pattern_kit_template (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE single_stream_locator
    ADD CONSTRAINT FKh5w78f19u5e2dh2yw8dym0j3c FOREIGN KEY (stream_locator_id) REFERENCES video_stream_locator (stream_locator_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_stream_locator_playlist
    ADD CONSTRAINT FKhsoxov486f7wcux0fnpi6pq8n FOREIGN KEY (state_id) REFERENCES task_list_state (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE pattern_kit_template_fields
    ADD CONSTRAINT FKlrf4gep4xfkrhnwm6j4dka2uu FOREIGN KEY (pattern_kit_template_id) REFERENCES pattern_kit_template (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_file_pack_video_files
    ADD CONSTRAINT FKm3cibg22cya8tdm8vt7omwsvd FOREIGN KEY (video_file_pack_id) REFERENCES video_file_pack (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE country_locales
    ADD CONSTRAINT FKmhm2g8ao82m5x2vs1gc8h0kes FOREIGN KEY (country_name) REFERENCES country (name) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_file_source_video_file_packs
    ADD CONSTRAINT FKmuvre4ml6u44nmmpeg12gph62 FOREIGN KEY (video_file_packs_id) REFERENCES video_file_pack (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE task_list_state_task_states
    ADD CONSTRAINT FKno6l627k9ab0im6fypkpmbml1 FOREIGN KEY (task_list_state_id) REFERENCES task_list_state (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE competition
    ADD CONSTRAINT FKsm71y25lvi9xjp3gnqbdqxalm FOREIGN KEY (name_id) REFERENCES proper_name (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE plaintext_data_source_pattern_kits
    ADD CONSTRAINT FKtdk2i8onxouj5p6o4jipm0yr5 FOREIGN KEY (plaintext_data_source_data_source_id) REFERENCES plaintext_data_source (data_source_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE artwork_collection_collection
    ADD CONSTRAINT fk_artcolcol_on_artwork FOREIGN KEY (collection_id) REFERENCES artwork (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE artwork_collection_collection
    ADD CONSTRAINT fk_artcolcol_on_artwork_collection FOREIGN KEY (artwork_collection_id) REFERENCES artwork_collection (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE file_server_user_cookies
    ADD CONSTRAINT fk_filserusecoo_on_file_server_user FOREIGN KEY (file_server_user_user_id) REFERENCES file_server_user (user_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE file_server_user_cookies
    ADD CONSTRAINT fk_filserusecoo_on_secure_cookie FOREIGN KEY (cookies_id) REFERENCES secure_cookie (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE highlight_file_sources
    ADD CONSTRAINT fk_higfilsou_on_highlight FOREIGN KEY (highlight_event_id) REFERENCES highlight (event_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE highlight_file_sources
    ADD CONSTRAINT fk_higfilsou_on_video_file_source FOREIGN KEY (file_sources_file_src_id) REFERENCES video_file_source (file_src_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE match_game_file_sources
    ADD CONSTRAINT fk_matgamfilsou_on_match FOREIGN KEY (match_game_event_id) REFERENCES match_game (event_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE match_game_file_sources
    ADD CONSTRAINT fk_matgamfilsou_on_video_file_source FOREIGN KEY (file_sources_file_src_id) REFERENCES video_file_source (file_src_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE pattern_kit_template_related_templates
    ADD CONSTRAINT fk_patkittemreltem_on_relatedtemplates FOREIGN KEY (related_templates_id) REFERENCES pattern_kit_template (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE pattern_kit_fields
    ADD CONSTRAINT fk_patternkit_fields_on_pattern_kit FOREIGN KEY (pattern_kit_id) REFERENCES pattern_kit (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE proper_name_synonyms
    ADD CONSTRAINT fk_pronamsyn_on_proper_name FOREIGN KEY (proper_name_id) REFERENCES proper_name (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE task_list_state_task_states
    ADD CONSTRAINT fk_taslisstatassta_on_task_state FOREIGN KEY (task_states_id) REFERENCES task_state (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE team_colors
    ADD CONSTRAINT fk_team_colors_on_team FOREIGN KEY (team_id) REFERENCES team (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_file_pack_video_files
    ADD CONSTRAINT fk_vidfilpacvidfil_on_video_file FOREIGN KEY (video_files_file_id) REFERENCES video_file (file_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_file_source_video_file_packs
    ADD CONSTRAINT fk_vidfilsouvidfilpac_on_video_file_source FOREIGN KEY (video_file_source_file_src_id) REFERENCES video_file_source (file_src_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE video_stream_locator_playlist_stream_locators
    ADD CONSTRAINT fk_vidstrlocplastrloc_on_video_stream_locator FOREIGN KEY (stream_locators_stream_locator_id) REFERENCES video_stream_locator (stream_locator_id) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE proper_name_synonyms
    ADD CONSTRAINT proper_name_synonyms_ibfk_1 FOREIGN KEY (synonyms_id) REFERENCES synonym (id) ON UPDATE RESTRICT ON DELETE RESTRICT;