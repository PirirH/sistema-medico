-- Fase 5: mantenimiento de usuarios, auditoría y agenda médica

ALTER TABLE internal_users ADD COLUMN email VARCHAR(150);
ALTER TABLE internal_users ADD COLUMN insurance_number VARCHAR(50);
ALTER TABLE internal_users ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX uq_internal_users_email ON internal_users (lower(email)) WHERE email IS NOT NULL;

CREATE TABLE audit_log (
    id            SERIAL PRIMARY KEY,
    action        VARCHAR(50)  NOT NULL,
    entity        VARCHAR(50)  NOT NULL,
    entity_id     INTEGER,
    detail        VARCHAR(500),
    performed_by  INTEGER REFERENCES internal_users(id),
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);

CREATE TABLE agenda_event (
    id           SERIAL PRIMARY KEY,
    doctor_id    INTEGER      NOT NULL REFERENCES internal_users(id),
    title        VARCHAR(150) NOT NULL,
    description  VARCHAR(500),
    start_at     TIMESTAMP    NOT NULL,
    end_at       TIMESTAMP    NOT NULL,
    event_type   VARCHAR(20)  NOT NULL,
    all_day      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_agenda_event_doctor ON agenda_event(doctor_id, start_at);

CREATE TABLE agenda_task (
    id           SERIAL PRIMARY KEY,
    doctor_id    INTEGER      NOT NULL REFERENCES internal_users(id),
    title        VARCHAR(150) NOT NULL,
    description  VARCHAR(1000),
    priority     VARCHAR(10)  NOT NULL,
    due_date     DATE         NOT NULL,
    completed    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_agenda_task_doctor ON agenda_task(doctor_id, due_date);
