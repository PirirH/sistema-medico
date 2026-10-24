-- =========================================================
-- V6: Fase 3 - Signos vitales (CU-07), consulta médica (CU-08), seguimiento (CU-12)
-- =========================================================

CREATE TABLE cie10 (
    code         VARCHAR(10)  PRIMARY KEY,
    description  VARCHAR(300) NOT NULL
);

CREATE TABLE vital_signs (
    id              SERIAL PRIMARY KEY,
    appointment_id  INTEGER       NOT NULL UNIQUE REFERENCES appointment(id),
    recorded_by     INTEGER       NOT NULL REFERENCES internal_users(id),
    systolic        INTEGER       NOT NULL,
    diastolic       INTEGER       NOT NULL,
    temperature     NUMERIC(3,1)  NOT NULL,
    weight          NUMERIC(5,2)  NOT NULL,
    height          NUMERIC(5,2)  NOT NULL,
    heart_rate      INTEGER       NOT NULL,
    emergency       BOOLEAN       NOT NULL DEFAULT FALSE,
    alerts          VARCHAR(500),
    recorded_at     TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE consultation (
    id                SERIAL PRIMARY KEY,
    appointment_id    INTEGER        NOT NULL UNIQUE REFERENCES appointment(id),
    visit_reason      VARCHAR(2000)  NOT NULL,
    clinical_findings VARCHAR(5000),
    cie10_code        VARCHAR(10)    REFERENCES cie10(code),
    diagnosis         VARCHAR(5000),
    treatment_plan    VARCHAR(5000),
    notes             VARCHAR(2000),
    status            VARCHAR(20)    NOT NULL,
    started_at        TIMESTAMP      NOT NULL DEFAULT now(),
    finished_at       TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT now(),
    version           BIGINT         NOT NULL DEFAULT 0
);

ALTER TABLE appointment
    ADD COLUMN follow_up_type          VARCHAR(30),
    ADD COLUMN parent_consultation_id  INTEGER REFERENCES consultation(id),
    ADD COLUMN reminder_sent           BOOLEAN NOT NULL DEFAULT FALSE;

-- Colas de enfermería y médico (filtran por estado y hora de llegada)
CREATE INDEX idx_appointment_status_arrival ON appointment(status, arrival_time);
-- Recordatorios de seguimiento
CREATE INDEX idx_appointment_followup ON appointment(follow_up_type, reminder_sent, scheduled_at);

INSERT INTO cie10 (code, description) VALUES
    ('A09', 'Diarrea y gastroenteritis de presunto origen infeccioso'),
    ('A90', 'Dengue clásico'),
    ('B34.9', 'Infección viral, no especificada'),
    ('D50.9', 'Anemia por deficiencia de hierro, sin otra especificación'),
    ('E03.9', 'Hipotiroidismo, no especificado'),
    ('E11.9', 'Diabetes mellitus tipo 2, sin complicaciones'),
    ('E55.9', 'Deficiencia de vitamina D, no especificada'),
    ('E66.9', 'Obesidad, no especificada'),
    ('E78.5', 'Hiperlipidemia, no especificada'),
    ('F32.9', 'Episodio depresivo, no especificado'),
    ('F41.9', 'Trastorno de ansiedad, no especificado'),
    ('G43.9', 'Migraña, sin otra especificación'),
    ('G44.2', 'Cefalea tensional'),
    ('H10.9', 'Conjuntivitis, no especificada'),
    ('H66.9', 'Otitis media, no especificada'),
    ('I10', 'Hipertensión esencial (primaria)'),
    ('I20.9', 'Angina de pecho, no especificada'),
    ('I25.9', 'Enfermedad isquémica crónica del corazón, no especificada'),
    ('I48', 'Fibrilación y aleteo auriculares'),
    ('I50.9', 'Insuficiencia cardíaca, no especificada'),
    ('J00', 'Rinofaringitis aguda (resfriado común)'),
    ('J02.9', 'Faringitis aguda, no especificada'),
    ('J03.9', 'Amigdalitis aguda, no especificada'),
    ('J06.9', 'Infección aguda de las vías respiratorias superiores, no especificada'),
    ('J11.1', 'Influenza con otras manifestaciones respiratorias, virus no identificado'),
    ('J18.9', 'Neumonía, no especificada'),
    ('J20.9', 'Bronquitis aguda, no especificada'),
    ('J30.9', 'Rinitis alérgica, no especificada'),
    ('J45.9', 'Asma, no especificada'),
    ('K21.9', 'Enfermedad del reflujo gastroesofágico sin esofagitis'),
    ('K29.7', 'Gastritis, no especificada'),
    ('K30', 'Dispepsia'),
    ('K59.0', 'Estreñimiento'),
    ('L20.9', 'Dermatitis atópica, no especificada'),
    ('L30.9', 'Dermatitis, no especificada'),
    ('L70.0', 'Acné vulgar'),
    ('M25.5', 'Dolor en articulación'),
    ('M54.5', 'Lumbago (dolor lumbar bajo)'),
    ('M79.1', 'Mialgia'),
    ('N39.0', 'Infección de vías urinarias, sitio no especificado'),
    ('N76.0', 'Vaginitis aguda'),
    ('N92.6', 'Menstruación irregular, no especificada'),
    ('R05', 'Tos'),
    ('R07.4', 'Dolor torácico, no especificado'),
    ('R10.4', 'Otros dolores abdominales y los no especificados'),
    ('R11', 'Náusea y vómito'),
    ('R42', 'Mareo y desvanecimiento'),
    ('R50.9', 'Fiebre, no especificada'),
    ('R51', 'Cefalea'),
    ('R53', 'Malestar y fatiga'),
    ('S93.4', 'Esguince y torcedura del tobillo'),
    ('T78.4', 'Alergia, no especificada'),
    ('Z00.0', 'Examen médico general'),
    ('Z00.1', 'Control de salud de rutina del niño'),
    ('Z23', 'Necesidad de inmunización contra enfermedad bacteriana única'),
    ('Z34.9', 'Supervisión de embarazo normal, no especificada');
