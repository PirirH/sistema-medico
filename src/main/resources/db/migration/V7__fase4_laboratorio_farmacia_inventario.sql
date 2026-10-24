-- =========================================================
-- V7: Fase 4 - Laboratorio (CU-09, CU-10), recetas y farmacia (CU-11), bitácora de inventario (CU-15)
-- =========================================================

CREATE TABLE laboratories (
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    description  VARCHAR(500),
    active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE lab_exam (
    id               SERIAL PRIMARY KEY,
    name             VARCHAR(200)  NOT NULL,
    description      VARCHAR(500),
    default_amount   NUMERIC(10,2) NOT NULL,
    reference_range  VARCHAR(100),
    unit             VARCHAR(50),
    laboratory_id    INTEGER NOT NULL REFERENCES laboratories(id),
    active           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE medicine (
    id             SERIAL PRIMARY KEY,
    name           VARCHAR(200)  NOT NULL,
    description    VARCHAR(500)  NOT NULL,
    default_price  NUMERIC(10,2) NOT NULL,
    unit           VARCHAR(50)   NOT NULL,
    controlled     BOOLEAN       NOT NULL DEFAULT FALSE,
    minimum_stock  INTEGER,
    active         BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE medicine_inventory (
    id             SERIAL PRIMARY KEY,
    medicine_id    INTEGER NOT NULL REFERENCES medicine(id),
    branch_id      INTEGER NOT NULL REFERENCES branches(id),
    current_stock  INTEGER NOT NULL DEFAULT 0 CHECK (current_stock >= 0),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    version        BIGINT  NOT NULL DEFAULT 0,
    UNIQUE (medicine_id, branch_id)
);

CREATE TABLE inventory_movement (
    id                SERIAL PRIMARY KEY,
    movement_type     VARCHAR(20)   NOT NULL,
    medicine_id       INTEGER       NOT NULL REFERENCES medicine(id),
    branch_id         INTEGER       NOT NULL REFERENCES branches(id),
    quantity          INTEGER       NOT NULL,
    previous_stock    INTEGER       NOT NULL,
    new_stock         INTEGER       NOT NULL,
    unit_cost         NUMERIC(10,2),
    reference_number  VARCHAR(50),
    notes             VARCHAR(500),
    performed_by      INTEGER REFERENCES internal_users(id),
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_inventory_movement_created ON inventory_movement(created_at);
CREATE INDEX idx_inventory_movement_medicine ON inventory_movement(medicine_id, branch_id);

CREATE TABLE lab_order (
    id             SERIAL PRIMARY KEY,
    order_number   VARCHAR(20)   NOT NULL UNIQUE,
    appointment_id INTEGER       NOT NULL REFERENCES appointment(id),
    patient_id     INTEGER       NOT NULL REFERENCES patients(id),
    doctor_id      INTEGER       NOT NULL REFERENCES internal_users(id),
    branch_id      INTEGER       NOT NULL REFERENCES branches(id),
    status         VARCHAR(20)   NOT NULL,
    external       BOOLEAN       NOT NULL DEFAULT FALSE,
    total_amount   NUMERIC(10,2) NOT NULL,
    notes          VARCHAR(1000),
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    version        BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_lab_order_patient ON lab_order(patient_id);
CREATE INDEX idx_lab_order_status ON lab_order(status);

CREATE TABLE lab_order_item (
    id             SERIAL PRIMARY KEY,
    order_id       INTEGER       NOT NULL REFERENCES lab_order(id),
    exam_id        INTEGER       NOT NULL REFERENCES lab_exam(id),
    amount         NUMERIC(10,2) NOT NULL,
    result_value   VARCHAR(200),
    result_unit    VARCHAR(50),
    result_date    DATE,
    out_of_range   BOOLEAN       NOT NULL DEFAULT FALSE,
    result_notes   VARCHAR(500),
    published      BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at   TIMESTAMP,
    resulted_by    INTEGER REFERENCES internal_users(id),
    published_by   INTEGER REFERENCES internal_users(id)
);
CREATE INDEX idx_lab_order_item_order ON lab_order_item(order_id);

CREATE TABLE lab_payment (
    id                  SERIAL PRIMARY KEY,
    transaction_number  VARCHAR(30)   NOT NULL UNIQUE,
    lab_order_id        INTEGER       NOT NULL REFERENCES lab_order(id),
    method              VARCHAR(20)   NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    amount              NUMERIC(10,2) NOT NULL,
    card_brand          VARCHAR(20),
    card_last_four      VARCHAR(4),
    amount_received     NUMERIC(10,2),
    change_given        NUMERIC(10,2),
    processed_by        INTEGER REFERENCES internal_users(id),
    paid_at             TIMESTAMP     NOT NULL DEFAULT now(),
    created_at          TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_lab_payment_order ON lab_payment(lab_order_id);

CREATE TABLE prescription (
    id                   SERIAL PRIMARY KEY,
    prescription_number  VARCHAR(20)  NOT NULL UNIQUE,
    appointment_id       INTEGER      NOT NULL REFERENCES appointment(id),
    consultation_id      INTEGER      REFERENCES consultation(id),
    patient_id           INTEGER      NOT NULL REFERENCES patients(id),
    doctor_id            INTEGER      NOT NULL REFERENCES internal_users(id),
    branch_id            INTEGER      NOT NULL REFERENCES branches(id),
    status               VARCHAR(20)  NOT NULL,
    notes                VARCHAR(1000),
    created_at           TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_prescription_status ON prescription(status, created_at);

CREATE TABLE prescription_item (
    id               SERIAL PRIMARY KEY,
    prescription_id  INTEGER      NOT NULL REFERENCES prescription(id),
    medicine_id      INTEGER      NOT NULL REFERENCES medicine(id),
    dose             VARCHAR(100) NOT NULL,
    frequency        VARCHAR(100) NOT NULL,
    duration         VARCHAR(100) NOT NULL,
    instructions     VARCHAR(500),
    quantity         INTEGER      NOT NULL DEFAULT 1
);

CREATE TABLE dispensation (
    id                   SERIAL PRIMARY KEY,
    dispensation_number  VARCHAR(20)   NOT NULL UNIQUE,
    prescription_id      INTEGER       NOT NULL REFERENCES prescription(id),
    branch_id            INTEGER       NOT NULL REFERENCES branches(id),
    pharmacist_id        INTEGER       NOT NULL REFERENCES internal_users(id),
    total_amount         NUMERIC(10,2) NOT NULL,
    status               VARCHAR(20)   NOT NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE dispensation_item (
    id                   SERIAL PRIMARY KEY,
    dispensation_id      INTEGER       NOT NULL REFERENCES dispensation(id),
    medicine_id          INTEGER       NOT NULL REFERENCES medicine(id),
    quantity             INTEGER       NOT NULL,
    unit_price           NUMERIC(10,2) NOT NULL,
    substituted          BOOLEAN       NOT NULL DEFAULT FALSE,
    substitution_reason  VARCHAR(300),
    substitute_name      VARCHAR(200)
);

-- ---------------------------------------------------------
-- Datos semilla para poder probar
-- ---------------------------------------------------------
INSERT INTO laboratories (name, description) VALUES
    ('Laboratorio Clínico Central', 'Análisis clínicos de rutina y especializados');

INSERT INTO lab_exam (name, description, default_amount, reference_range, unit, laboratory_id)
SELECT e.name, e.description, e.amount, e.ref, e.unit, l.id
FROM laboratories l,
     (VALUES
        ('Hematología completa',      'Conteo de células sanguíneas',        85.00, 'Varios',     ''),
        ('Glucosa en ayunas',         'Nivel de glucosa en sangre',          35.00, '70 - 100',   'mg/dL'),
        ('Colesterol total',          'Perfil lipídico básico',              45.00, '< 200',      'mg/dL'),
        ('Triglicéridos',             'Perfil lipídico básico',              50.00, '< 150',      'mg/dL'),
        ('Creatinina',                'Función renal',                       55.00, '0.6 - 1.3',  'mg/dL'),
        ('Examen general de orina',   'Análisis físico, químico y sedimento',30.00, 'Normal',     ''),
        ('TSH',                       'Hormona estimulante de tiroides',    120.00, '0.4 - 4.0',  'mUI/L'),
        ('Urocultivo',                'Cultivo de orina',                    95.00, 'Negativo',   '')
     ) AS e(name, description, amount, ref, unit)
WHERE l.name = 'Laboratorio Clínico Central';

INSERT INTO medicine (name, description, default_price, unit, controlled, minimum_stock) VALUES
    ('Paracetamol 500 mg',   'Analgésico y antipirético',                    1.50, 'tableta',  FALSE, 50),
    ('Ibuprofeno 400 mg',    'Antiinflamatorio no esteroideo',               2.00, 'tableta',  FALSE, 50),
    ('Amoxicilina 500 mg',   'Antibiótico de amplio espectro',               3.50, 'cápsula',  FALSE, 40),
    ('Azitromicina 500 mg',  'Antibiótico macrólido',                       12.00, 'tableta',  FALSE, 20),
    ('Loratadina 10 mg',     'Antihistamínico para alergias',                2.50, 'tableta',  FALSE, 30),
    ('Omeprazol 20 mg',      'Inhibidor de la bomba de protones',            3.00, 'cápsula',  FALSE, 40),
    ('Metformina 850 mg',    'Antidiabético oral',                           2.00, 'tableta',  FALSE, 40),
    ('Losartán 50 mg',       'Antihipertensivo',                             2.75, 'tableta',  FALSE, 40),
    ('Salbutamol inhalador', 'Broncodilatador de rescate',                  65.00, 'unidad',   FALSE, 10),
    ('Diazepam 5 mg',        'Ansiolítico - medicamento controlado',         4.00, 'tableta',  TRUE,  20),
    ('Tramadol 50 mg',       'Analgésico opioide - medicamento controlado',  5.00, 'cápsula',  TRUE,  20);

-- Inventario inicial en todas las sucursales (los controlados con menos existencias)
INSERT INTO medicine_inventory (medicine_id, branch_id, current_stock)
SELECT m.id, b.id, CASE WHEN m.controlled THEN 30 ELSE 200 END
FROM medicine m CROSS JOIN branches b;
