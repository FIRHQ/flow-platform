DROP SEQUENCE IF EXISTS flows_id_sequence;
CREATE SEQUENCE flows_id_sequence START 10000 INCREMENT 1;

CREATE TABLE flows
(
    id         BIGSERIAL PRIMARY KEY,
    type       VARCHAR(10)                 NOT NULL,
    parent_id  BIGINT                      NOT NULL,
    name       VARCHAR(100) UNIQUE         NOT NULL,
    variables  TEXT                        NOT NULL,
    git_link   TEXT,
    yaml       TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by VARCHAR(200),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by VARCHAR(200)
);

CREATE TABLE flows_yaml
(
    id         BIGINT PRIMARY KEY,
    yaml       TEXT                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by VARCHAR(200),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by VARCHAR(200)
);