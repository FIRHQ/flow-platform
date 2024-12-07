DROP SEQUENCE IF EXISTS groups_id_sequence;
CREATE SEQUENCE groups_id_sequence START 10000 INCREMENT 1;

CREATE TABLE groups
(
    id         BIGSERIAL PRIMARY KEY,
    parent_id  BIGINT                      NOT NULL,
    name       VARCHAR(200) UNIQUE         NOT NULL,
    variables  TEXT                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by VARCHAR(200),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by VARCHAR(200)
);

DROP SEQUENCE IF EXISTS flows_id_sequence;
CREATE SEQUENCE flows_id_sequence START 10000 INCREMENT 1;

CREATE TABLE flows
(
    id             BIGSERIAL PRIMARY KEY,
    parent_id      BIGINT                      NOT NULL,
    name           VARCHAR(200) UNIQUE         NOT NULL,
    variables      TEXT                        NOT NULL,
    git_link       TEXT,
    max_queue_time INT                         NOT NULL,
    step_timeout   INT                         NOT NULL,
    cron           VARCHAR(100),
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by     VARCHAR(200),
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by     VARCHAR(200)
);