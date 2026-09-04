-- =========================================================
-- V1: Esquema inicial - Catálogos base + Usuarios + Pacientes
-- =========================================================

CREATE TABLE roles (
                       id          SERIAL PRIMARY KEY,
                       name        VARCHAR(50) NOT NULL UNIQUE,
                       active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE branches (
                          id              SERIAL PRIMARY KEY,
                          name            VARCHAR(100) NOT NULL,
                          address         VARCHAR(200),
                          phone           VARCHAR(20),
                          opening_hours   VARCHAR(100),
                          active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE specialties (
                             id              SERIAL PRIMARY KEY,
                             name            VARCHAR(100) NOT NULL,
                             description     VARCHAR(300),
                             active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE branch_specialty (
                                  id              SERIAL PRIMARY KEY,
                                  branch_id       INTEGER NOT NULL REFERENCES branches(id),
                                  specialty_id    INTEGER NOT NULL REFERENCES specialties(id),
                                  active          BOOLEAN NOT NULL DEFAULT TRUE,
                                  UNIQUE (branch_id, specialty_id)
);

CREATE TABLE internal_users (
                                id                      SERIAL PRIMARY KEY,
                                username                VARCHAR(9) NOT NULL UNIQUE,
                                password                VARCHAR(255) NOT NULL,
                                full_name               VARCHAR(100) NOT NULL,
                                nit                     VARCHAR(9),
                                dpi                     VARCHAR(13),
                                phone                   VARCHAR(8),
                                role_id                 INTEGER NOT NULL REFERENCES roles(id),
                                branch_id               INTEGER REFERENCES branches(id),
                                specialty_id            INTEGER REFERENCES specialties(id),
                                active                  BOOLEAN NOT NULL DEFAULT TRUE,
                                failed_login_attempts   INTEGER NOT NULL DEFAULT 0,
                                locked_until            TIMESTAMP,
                                created_at              TIMESTAMP NOT NULL DEFAULT now(),
                                updated_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE patients (
                          id                      SERIAL PRIMARY KEY,
                          dpi                     VARCHAR(13) NOT NULL UNIQUE,
                          full_name               VARCHAR(100) NOT NULL,
                          phone                   VARCHAR(8) NOT NULL,
                          email                   VARCHAR(150) NOT NULL UNIQUE,
                          insurance_number        VARCHAR(50),
                          username                VARCHAR(9) NOT NULL UNIQUE,
                          password                VARCHAR(255) NOT NULL,
                          active                  BOOLEAN NOT NULL DEFAULT TRUE,
                          failed_login_attempts   INTEGER NOT NULL DEFAULT 0,
                          locked_until            TIMESTAMP,
                          created_at              TIMESTAMP NOT NULL DEFAULT now(),
                          updated_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_patients_dpi ON patients(dpi);
CREATE INDEX idx_internal_users_username ON internal_users(username);
CREATE INDEX idx_internal_users_dpi ON internal_users(dpi);