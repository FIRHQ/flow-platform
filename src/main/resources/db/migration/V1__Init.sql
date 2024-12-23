-- flow related tables --

CREATE TABLE flows
(
    id         BIGSERIAL PRIMARY KEY,
    type       VARCHAR(10)                 NOT NULL,
    parent_id  BIGINT                      NOT NULL,
    name       VARCHAR(100) UNIQUE         NOT NULL,
    variables  json                        NOT NULL,
    git_link   json,
    yaml       TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by BIGINT,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by BIGINT
);

DROP SEQUENCE IF EXISTS flows_id_sequence;
CREATE SEQUENCE flows_id_sequence START 10000 INCREMENT 1 OWNED BY flows.id;

CREATE TABLE flows_yaml
(
    id         BIGINT PRIMARY KEY,
    yaml       TEXT                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by BIGINT,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by BIGINT
);

CREATE TABLE flows_user
(
    flow_id    BIGINT                      NOT NULL,
    user_id    BIGINT                      NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by BIGINT,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by BIGINT,
    PRIMARY KEY (flow_id, user_id)
);

-- build related tables --

CREATE TABLE build
(
    id             BIGSERIAL PRIMARY KEY,
    flow_id        BIGINT                      NOT NULL,
    build_date     INTEGER                     NOT NULL,
    build_sequence BIGINT                      NOT NULL,
    build_alias    varchar(50)                 NOT NULL,
    trigger        varchar(20)                 NOT NULL,
    status         varchar(20)                 NOT NULL,
    agent_tags     varchar(20)[]               NOT NULL,
    commit_hash    varchar(40),
    agent_id       BIGINT,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by     BIGINT,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by     BIGINT,
    UNIQUE (flow_id, build_date, build_sequence)
);

DROP SEQUENCE IF EXISTS build_id_sequence;
CREATE SEQUENCE build_id_sequence START 10000 INCREMENT 1 OWNED BY build.id;

-- trigger function to create build_date, build_sequence and build_alias on insert
CREATE OR REPLACE FUNCTION auto_build_sequence() RETURNS trigger AS
$build_sequence$
BEGIN
    SELECT to_char(current_date, 'YYYYMMDD')::INTEGER INTO NEW.build_date;

    SELECT COALESCE(MAX(build_sequence) + 1, 1)
    INTO NEW.build_sequence
    FROM "build"
    WHERE flow_id = NEW.flow_id
      AND build_date = NEW.build_date;

    SELECT concat(NEW.build_date, '.', NEW.build_sequence) INTO NEW.build_alias;

    RETURN NEW;
END;
$build_sequence$ LANGUAGE plpgsql;

-- add trigger on insert
CREATE TRIGGER build_sequence_trigger
    BEFORE INSERT
    ON "build"
    FOR EACH ROW
EXECUTE PROCEDURE auto_build_sequence();

CREATE TABLE build_yaml
(
    id         BIGINT PRIMARY KEY,
    variables  json                        NOT NULL,
    yaml       TEXT                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by BIGINT,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by BIGINT
);