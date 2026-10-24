-- V4__create_appointment_table.sql
CREATE TABLE appointment (
                             id                      SERIAL PRIMARY KEY,
                             appointment_number      VARCHAR(20)   NOT NULL UNIQUE,
                             patient_id              INTEGER       NOT NULL REFERENCES patients(id),
                             doctor_id               INTEGER       NOT NULL REFERENCES internal_users(id),
                             branch_specialty_id     INTEGER       NOT NULL REFERENCES branch_specialty(id),
                             scheduled_at            TIMESTAMP     NOT NULL,
                             duration_minutes        INTEGER       NOT NULL,
                             status                  VARCHAR(30)   NOT NULL,
                             channel                 VARCHAR(20)   NOT NULL,
                             priority                VARCHAR(20)   NOT NULL DEFAULT 'NORMAL',
                             reason                  VARCHAR(2000) NOT NULL,
                             total_amount            NUMERIC(10,2) NOT NULL,
                             reservation_expires_at  TIMESTAMP,
                             arrival_time            TIMESTAMP,
                             version                 BIGINT        NOT NULL DEFAULT 0,
                             created_at              TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_appointment_patient ON appointment(patient_id);
CREATE INDEX idx_appointment_doctor_schedule ON appointment(doctor_id, scheduled_at);
CREATE INDEX idx_appointment_status_expiry ON appointment(status, reservation_expires_at);