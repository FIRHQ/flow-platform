DROP SEQUENCE IF EXISTS flows_id_sequence;
CREATE SEQUENCE flows_id_sequence START 10000 INCREMENT 1;

CREATE TABLE flows
(
    id         BIGSERIAL PRIMARY KEY,
    parent_id  BIGINT                      NOT NULL,
    name       VARCHAR(200)                NOT NULL,
    type       VARCHAR(10)                 NOT NULL,
    variables  TEXT                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by VARCHAR(36),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by VARCHAR(36)
);