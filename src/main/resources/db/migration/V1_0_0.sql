CREATE TABLE IF NOT EXISTS artwork
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    file       VARCHAR(255)          NULL,
    file_size  BIGINT                NULL,
    media_type VARCHAR(255)          NULL,
    width      INT                   NOT NULL,
    height     INT                   NOT NULL,
    created    datetime              NULL,
    modified   datetime              NULL,
    CONSTRAINT pk_artwork PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS artwork_collection
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    selected_index INT                   NOT NULL,
    CONSTRAINT pk_artworkcollection PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS artwork_collection_collection
(
    artwork_collection_id BIGINT NOT NULL,
    collection_id         BIGINT NOT NULL,
    CONSTRAINT pk_artworkcollection_collection PRIMARY KEY (artwork_collection_id, collection_id)
);
CREATE TABLE IF NOT EXISTS competition
(
    id           char(36)     NOT NULL,
    country_name VARCHAR(255) NULL,
    emblem_id    BIGINT       NULL,
    fanart_id    BIGINT       NULL,
    CONSTRAINT pk_competition PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS country
(
    name           VARCHAR(255) NOT NULL,
    flag_file_name VARCHAR(255) NULL,
    CONSTRAINT pk_country PRIMARY KEY (name)
);
CREATE TABLE IF NOT EXISTS country_locales
(
    country_name VARCHAR(255) NOT NULL,
    locales      VARCHAR(255) NULL
);
CREATE TABLE IF NOT EXISTS data_source
(
    data_source_id char(36) NOT NULL,
    plugin_id      char(36) NULL,
    enabled        BIT(1)   NOT NULL,
    CONSTRAINT pk_datasource PRIMARY KEY (data_source_id)
);
CREATE TABLE IF NOT EXISTS event
(
    event_id       char(36)     NOT NULL,
    competition_id char(36)     NULL,
    date           datetime     NULL,
    artwork_id     BIGINT       NULL,
    start_date     date         NULL,
    end_date       date         NULL,
    title          VARCHAR(255) NULL,
    fixture_number INT          NULL,
    CONSTRAINT pk_event PRIMARY KEY (event_id)
);
CREATE TABLE IF NOT EXISTS event_file_sources
(
    event_event_id           char(36) NOT NULL,
    file_sources_file_src_id char(36) NOT NULL,
    CONSTRAINT pk_event_filesources PRIMARY KEY (event_event_id, file_sources_file_src_id)
);
CREATE TABLE IF NOT EXISTS file_server_user
(
    user_id   char(36) NOT NULL,
    username  TEXT     NULL,
    password  TEXT     NULL,
    email     TEXT     NULL,
    logged_in BIT(1)   NOT NULL,
    server_id char(36) NULL,
    CONSTRAINT pk_fileserveruser PRIMARY KEY (user_id)
);
CREATE TABLE IF NOT EXISTS file_server_user_cookies
(
    file_server_user_user_id char(36) NOT NULL,
    cookies_id               BIGINT   NOT NULL
);
CREATE TABLE IF NOT EXISTS highlight
(
    event_id char(36) NOT NULL,
    CONSTRAINT pk_highlight PRIMARY KEY (event_id)
);
CREATE TABLE IF NOT EXISTS highlight_file_sources
(
    highlight_event_id       char(36) NOT NULL,
    file_sources_file_src_id char(36) NOT NULL,
    CONSTRAINT pk_event_filesources PRIMARY KEY (highlight_event_id, file_sources_file_src_id)
);
CREATE TABLE IF NOT EXISTS match_game
(
    event_id     char(36) NOT NULL,
    home_team_id char(36) NOT NULL,
    away_team_id char(36) NOT NULL,
    CONSTRAINT pk_matchgame PRIMARY KEY (event_id)
);
CREATE TABLE IF NOT EXISTS match_game_file_sources
(
    match_game_event_id      char(36) NOT NULL,
    file_sources_file_src_id char(36) NOT NULL,
    CONSTRAINT pk_event_filesources PRIMARY KEY (match_game_event_id, file_sources_file_src_id)
);
CREATE TABLE IF NOT EXISTS pattern_kit
(
    id      BIGINT AUTO_INCREMENT NOT NULL,
    pattern LONGTEXT              NULL,
    CONSTRAINT pk_patternkit PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS pattern_kit_fields
(
    pattern_kit_id BIGINT       NOT NULL,
    fields         VARCHAR(255) NULL,
    fields_key     INT          NOT NULL,
    CONSTRAINT pk_patternkit_fields PRIMARY KEY (pattern_kit_id, fields_key)
);
CREATE TABLE IF NOT EXISTS pattern_kit_template
(
    id BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT pk_patternkittemplate PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS pattern_kit_template_fields
(
    pattern_kit_template_id BIGINT   NOT NULL,
    field_name              LONGTEXT NULL,
    required                BIT(1)   NULL
);
CREATE TABLE IF NOT EXISTS pattern_kit_template_related_templates
(
    pattern_kit_template_id BIGINT NOT NULL,
    related_templates_id    BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS plaintext_data_source
(
    data_source_id char(36) NOT NULL,
    CONSTRAINT pk_plaintextdatasource PRIMARY KEY (data_source_id)
);
CREATE TABLE IF NOT EXISTS plaintext_data_source_pattern_kits
(
    plaintext_data_source_data_source_id char(36) NOT NULL,
    pattern_kits_id                      BIGINT   NOT NULL
);
CREATE TABLE IF NOT EXISTS proper_name
(
    id BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT pk_propername PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS proper_name_synonyms
(
    proper_name_id BIGINT NOT NULL,
    synonyms_id    BIGINT NOT NULL,
    CONSTRAINT pk_propername_synonyms PRIMARY KEY (proper_name_id, synonyms_id)
);
CREATE TABLE IF NOT EXISTS restore_point
(
    id                     char(36)     NOT NULL,
    backup_archive         VARCHAR(255) NULL,
    timestamp              BIGINT       NULL,
    filesize               BIGINT       NULL,
    event_count            INT          NULL,
    competition_count      INT          NULL,
    team_count             INT          NULL,
    data_source_count      INT          NULL,
    file_server_user_count INT          NULL,
    CONSTRAINT pk_restorepoint PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS secure_cookie
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    cookie_value LONGTEXT              NULL,
    max_age      BIGINT                NULL,
    domain       VARCHAR(255)          NULL,
    `path`       VARCHAR(255)          NULL,
    secure       BIT(1)                NOT NULL,
    http_only    BIT(1)                NOT NULL,
    same_site    VARCHAR(255)          NULL,
    CONSTRAINT pk_securecookie PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS settings
(
    id                       BIGINT AUTO_INCREMENT NOT NULL,
    log_filename             VARCHAR(255)          NULL,
    artwork_storage_location VARCHAR(255)          NULL,
    video_storage_location   VARCHAR(255)          NULL,
    backup_location          VARCHAR(255)          NULL,
    refresh_events           VARCHAR(255)          NULL,
    prune_videos             VARCHAR(255)          NULL,
    video_expired_days       INT                   NOT NULL,
    CONSTRAINT pk_settings PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS single_stream_locator
(
    stream_locator_id BIGINT NOT NULL,
    CONSTRAINT pk_singlestreamlocator PRIMARY KEY (stream_locator_id)
);
CREATE TABLE IF NOT EXISTS stream_job_state
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    status           INT                   NULL,
    completion_ratio DOUBLE                NULL,
    error            LONGTEXT              NULL,
    CONSTRAINT pk_streamjobstate PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS synonym
(
    id BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT pk_synonym PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS task_list_state
(
    id BIGINT NOT NULL,
    CONSTRAINT pk_taskliststate PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS task_list_state_task_states
(
    task_list_state_id BIGINT NOT NULL,
    task_states_id     BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS task_state
(
    id BIGINT NOT NULL,
    CONSTRAINT pk_taskstate PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS team
(
    id           char(36)     NOT NULL,
    country_name VARCHAR(255) NULL,
    emblem_id    BIGINT       NULL,
    fanart_id    BIGINT       NULL,
    CONSTRAINT pk_team PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS team_colors
(
    team_id char(36)      NOT NULL,
    red     INT DEFAULT 0 NOT NULL,
    green   INT DEFAULT 0 NOT NULL,
    blue    INT DEFAULT 0 NOT NULL,
    alpha   INT DEFAULT 0 NOT NULL
);
CREATE TABLE IF NOT EXISTS video_file
(
    file_id        char(36)     NOT NULL,
    external_url   VARCHAR(255) NULL,
    title          INT          NULL,
    internal_url   LONGTEXT     NULL,
    metadata       LONGTEXT     NULL,
    last_refreshed BIGINT       NULL,
    CONSTRAINT pk_videofile PRIMARY KEY (file_id)
);
CREATE TABLE IF NOT EXISTS video_file_pack
(
    id char(36) NOT NULL,
    CONSTRAINT pk_videofilepack PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS video_file_pack_video_files
(
    video_file_pack_id  char(36) NOT NULL,
    pack_id             INT      NOT NULL,
    video_files_file_id char(36) NOT NULL,
    CONSTRAINT pk_videofilepack_videofiles PRIMARY KEY (video_file_pack_id, pack_id)
);
CREATE TABLE IF NOT EXISTS video_file_source
(
    file_src_id          char(36)     NOT NULL,
    channel              VARCHAR(255) NULL,
    source               VARCHAR(255) NULL,
    approximate_duration VARCHAR(255) NULL,
    languages            VARCHAR(255) NULL,
    resolution           INT          NULL,
    media_container      VARCHAR(255) NULL,
    video_codec          VARCHAR(255) NULL,
    audio_codec          VARCHAR(255) NULL,
    video_bitrate        BIGINT       NULL,
    audio_bitrate        BIGINT       NULL,
    filesize             BIGINT       NULL,
    framerate            INT          NOT NULL,
    audio_channels       VARCHAR(255) NULL,
    CONSTRAINT pk_videofilesource PRIMARY KEY (file_src_id)
);
CREATE TABLE IF NOT EXISTS video_file_source_video_file_packs
(
    video_file_source_file_src_id char(36) NOT NULL,
    video_file_packs_id           char(36) NOT NULL
);
CREATE TABLE IF NOT EXISTS video_stream_locator
(
    stream_locator_id  BIGINT       NOT NULL,
    playlist_path      VARCHAR(255) NULL,
    video_file_file_id char(36)     NULL,
    state_id           BIGINT       NULL,
    CONSTRAINT pk_videostreamlocator PRIMARY KEY (stream_locator_id)
);
CREATE TABLE IF NOT EXISTS video_stream_locator_playlist
(
    id       BIGINT NOT NULL,
    state_id BIGINT NULL,
    CONSTRAINT pk_videostreamlocatorplaylist PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS video_stream_locator_playlist_stream_locators
(
    video_stream_locator_playlist_id  BIGINT NOT NULL,
    stream_locators_stream_locator_id BIGINT NOT NULL
);
ALTER TABLE artwork_collection_collection
    ADD CONSTRAINT uc_artwork_collection_collection_collection UNIQUE (collection_id);
ALTER TABLE event_file_sources
    ADD CONSTRAINT uc_event_file_sources_filesources_filesrcid UNIQUE (file_sources_file_src_id);
ALTER TABLE file_server_user_cookies
    ADD CONSTRAINT uc_file_server_user_cookies_cookies UNIQUE (cookies_id);
ALTER TABLE highlight_file_sources
    ADD CONSTRAINT uc_highlight_file_sources_filesources_filesrcid UNIQUE (file_sources_file_src_id);
ALTER TABLE match_game_file_sources
    ADD CONSTRAINT uc_match_game_file_sources_filesources_filesrcid UNIQUE (file_sources_file_src_id);
ALTER TABLE pattern_kit_template_related_templates
    ADD CONSTRAINT uc_pattern_kit_template_related_templates_relatedtemplates UNIQUE (related_templates_id);
ALTER TABLE plaintext_data_source_pattern_kits
    ADD CONSTRAINT uc_plaintext_data_source_pattern_kits_patternkits UNIQUE (pattern_kits_id);
ALTER TABLE proper_name_synonyms
    ADD CONSTRAINT uc_proper_name_synonyms_synonyms UNIQUE (synonyms_id);
ALTER TABLE task_list_state_task_states
    ADD CONSTRAINT uc_task_list_state_task_states_taskstates UNIQUE (task_states_id);
ALTER TABLE video_file_pack_video_files
    ADD CONSTRAINT uc_video_file_pack_video_files_videofiles_fileid UNIQUE (video_files_file_id);
ALTER TABLE video_file_source_video_file_packs
    ADD CONSTRAINT uc_video_file_source_video_file_packs_videofilepacks UNIQUE (video_file_packs_id);
ALTER TABLE video_stream_locator_playlist_stream_locators
    ADD CONSTRAINT uc_videostreamlocatorplayliststre_streamlocatorsstreamlocatorid UNIQUE (stream_locators_stream_locator_id);
ALTER TABLE competition
    ADD CONSTRAINT FK_COMPETITION_ON_COUNTRY_NAME FOREIGN KEY (country_name) REFERENCES country (name);
ALTER TABLE competition
    ADD CONSTRAINT FK_COMPETITION_ON_EMBLEM FOREIGN KEY (emblem_id) REFERENCES artwork_collection (id);
ALTER TABLE competition
    ADD CONSTRAINT FK_COMPETITION_ON_FANART FOREIGN KEY (fanart_id) REFERENCES artwork_collection (id);
ALTER TABLE event
    ADD CONSTRAINT FK_EVENT_ON_ARTWORK FOREIGN KEY (artwork_id) REFERENCES artwork (id);
ALTER TABLE event
    ADD CONSTRAINT FK_EVENT_ON_COMPETITION FOREIGN KEY (competition_id) REFERENCES competition (id);
ALTER TABLE highlight
    ADD CONSTRAINT FK_HIGHLIGHT_ON_EVENTID FOREIGN KEY (event_id) REFERENCES event (event_id);
ALTER TABLE match_game
    ADD CONSTRAINT FK_MATCHGAME_ON_AWAYTEAM FOREIGN KEY (away_team_id) REFERENCES team (id);
ALTER TABLE match_game
    ADD CONSTRAINT FK_MATCHGAME_ON_EVENTID FOREIGN KEY (event_id) REFERENCES event (event_id);
ALTER TABLE match_game
    ADD CONSTRAINT FK_MATCHGAME_ON_HOMETEAM FOREIGN KEY (home_team_id) REFERENCES team (id);
ALTER TABLE plaintext_data_source
    ADD CONSTRAINT FK_PLAINTEXTDATASOURCE_ON_DATASOURCEID FOREIGN KEY (data_source_id) REFERENCES data_source (data_source_id);
ALTER TABLE single_stream_locator
    ADD CONSTRAINT FK_SINGLESTREAMLOCATOR_ON_STREAMLOCATORID FOREIGN KEY (stream_locator_id) REFERENCES video_stream_locator (stream_locator_id);
ALTER TABLE task_list_state
    ADD CONSTRAINT FK_TASKLISTSTATE_ON_ID FOREIGN KEY (id) REFERENCES stream_job_state (id);
ALTER TABLE task_state
    ADD CONSTRAINT FK_TASKSTATE_ON_ID FOREIGN KEY (id) REFERENCES stream_job_state (id);
ALTER TABLE team
    ADD CONSTRAINT FK_TEAM_ON_COUNTRY_NAME FOREIGN KEY (country_name) REFERENCES country (name);
ALTER TABLE team
    ADD CONSTRAINT FK_TEAM_ON_EMBLEM FOREIGN KEY (emblem_id) REFERENCES artwork_collection (id);
ALTER TABLE team
    ADD CONSTRAINT FK_TEAM_ON_FANART FOREIGN KEY (fanart_id) REFERENCES artwork_collection (id);
ALTER TABLE video_stream_locator_playlist
    ADD CONSTRAINT FK_VIDEOSTREAMLOCATORPLAYLIST_ON_STATE FOREIGN KEY (state_id) REFERENCES task_list_state (id);
ALTER TABLE video_stream_locator
    ADD CONSTRAINT FK_VIDEOSTREAMLOCATOR_ON_STATE FOREIGN KEY (state_id) REFERENCES task_state (id);
ALTER TABLE video_stream_locator
    ADD CONSTRAINT FK_VIDEOSTREAMLOCATOR_ON_VIDEOFILE_FILEID FOREIGN KEY (video_file_file_id) REFERENCES video_file (file_id);
ALTER TABLE artwork_collection_collection
    ADD CONSTRAINT fk_artcolcol_on_artwork FOREIGN KEY (collection_id) REFERENCES artwork (id);
ALTER TABLE artwork_collection_collection
    ADD CONSTRAINT fk_artcolcol_on_artwork_collection FOREIGN KEY (artwork_collection_id) REFERENCES artwork_collection (id);
ALTER TABLE country_locales
    ADD CONSTRAINT fk_country_locales_on_country FOREIGN KEY (country_name) REFERENCES country (name);
ALTER TABLE event_file_sources
    ADD CONSTRAINT fk_evefilsou_on_event FOREIGN KEY (event_event_id) REFERENCES event (event_id);
ALTER TABLE event_file_sources
    ADD CONSTRAINT fk_evefilsou_on_video_file_source FOREIGN KEY (file_sources_file_src_id) REFERENCES video_file_source (file_src_id);
ALTER TABLE file_server_user_cookies
    ADD CONSTRAINT fk_filserusecoo_on_file_server_user FOREIGN KEY (file_server_user_user_id) REFERENCES file_server_user (user_id);
ALTER TABLE file_server_user_cookies
    ADD CONSTRAINT fk_filserusecoo_on_secure_cookie FOREIGN KEY (cookies_id) REFERENCES secure_cookie (id);
ALTER TABLE highlight_file_sources
    ADD CONSTRAINT fk_higfilsou_on_highlight FOREIGN KEY (highlight_event_id) REFERENCES highlight (event_id);
ALTER TABLE highlight_file_sources
    ADD CONSTRAINT fk_higfilsou_on_video_file_source FOREIGN KEY (file_sources_file_src_id) REFERENCES video_file_source (file_src_id);
ALTER TABLE match_game_file_sources
    ADD CONSTRAINT fk_matgamfilsou_on_match FOREIGN KEY (match_game_event_id) REFERENCES match_game (event_id);
ALTER TABLE match_game_file_sources
    ADD CONSTRAINT fk_matgamfilsou_on_video_file_source FOREIGN KEY (file_sources_file_src_id) REFERENCES video_file_source (file_src_id);
ALTER TABLE pattern_kit_template_related_templates
    ADD CONSTRAINT fk_patkittemreltem_on_patternkittemplate FOREIGN KEY (pattern_kit_template_id) REFERENCES pattern_kit_template (id);
ALTER TABLE pattern_kit_template_related_templates
    ADD CONSTRAINT fk_patkittemreltem_on_relatedtemplates FOREIGN KEY (related_templates_id) REFERENCES pattern_kit_template (id);
ALTER TABLE pattern_kit_fields
    ADD CONSTRAINT fk_patternkit_fields_on_pattern_kit FOREIGN KEY (pattern_kit_id) REFERENCES pattern_kit (id);
ALTER TABLE pattern_kit_template_fields
    ADD CONSTRAINT fk_patternkittemplate_fields_on_pattern_kit_template FOREIGN KEY (pattern_kit_template_id) REFERENCES pattern_kit_template (id);
ALTER TABLE plaintext_data_source_pattern_kits
    ADD CONSTRAINT fk_pladatsoupatkit_on_pattern_kit FOREIGN KEY (pattern_kits_id) REFERENCES pattern_kit (id);
ALTER TABLE plaintext_data_source_pattern_kits
    ADD CONSTRAINT fk_pladatsoupatkit_on_plaintext_data_source FOREIGN KEY (plaintext_data_source_data_source_id) REFERENCES plaintext_data_source (data_source_id);
ALTER TABLE proper_name_synonyms
    ADD CONSTRAINT fk_pronamsyn_on_proper_name FOREIGN KEY (proper_name_id) REFERENCES proper_name (id);
ALTER TABLE proper_name_synonyms
    ADD CONSTRAINT fk_pronamsyn_on_synonym FOREIGN KEY (synonyms_id) REFERENCES synonym (id);
ALTER TABLE task_list_state_task_states
    ADD CONSTRAINT fk_taslisstatassta_on_task_list_state FOREIGN KEY (task_list_state_id) REFERENCES task_list_state (id);
ALTER TABLE task_list_state_task_states
    ADD CONSTRAINT fk_taslisstatassta_on_task_state FOREIGN KEY (task_states_id) REFERENCES task_state (id);
ALTER TABLE team_colors
    ADD CONSTRAINT fk_team_colors_on_team FOREIGN KEY (team_id) REFERENCES team (id);
ALTER TABLE video_file_pack_video_files
    ADD CONSTRAINT fk_vidfilpacvidfil_on_video_file FOREIGN KEY (video_files_file_id) REFERENCES video_file (file_id);
ALTER TABLE video_file_pack_video_files
    ADD CONSTRAINT fk_vidfilpacvidfil_on_video_file_pack FOREIGN KEY (video_file_pack_id) REFERENCES video_file_pack (id);
ALTER TABLE video_file_source_video_file_packs
    ADD CONSTRAINT fk_vidfilsouvidfilpac_on_video_file_pack FOREIGN KEY (video_file_packs_id) REFERENCES video_file_pack (id);
ALTER TABLE video_file_source_video_file_packs
    ADD CONSTRAINT fk_vidfilsouvidfilpac_on_video_file_source FOREIGN KEY (video_file_source_file_src_id) REFERENCES video_file_source (file_src_id);
ALTER TABLE video_stream_locator_playlist_stream_locators
    ADD CONSTRAINT fk_vidstrlocplastrloc_on_video_stream_locator FOREIGN KEY (stream_locators_stream_locator_id) REFERENCES video_stream_locator (stream_locator_id);
ALTER TABLE video_stream_locator_playlist_stream_locators
    ADD CONSTRAINT fk_vidstrlocplastrloc_on_video_stream_locator_playlist FOREIGN KEY (video_stream_locator_playlist_id) REFERENCES video_stream_locator_playlist (id);