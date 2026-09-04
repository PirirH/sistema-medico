-- =========================================================
-- V2: Datos semilla mínimos para probar el Portal de Acceso
-- =========================================================

INSERT INTO roles (name) VALUES
                             ('Medico'), ('Enfermero'), ('Recepcionista'), ('Cajero'),
                             ('Laboratorista'), ('Farmaceutico'), ('Administrador');

INSERT INTO branches (name, address, phone, opening_hours) VALUES
                                                               ('Sede Central', '5a Avenida 10-25, Zona 1, Guatemala', '2234-5678', 'Lunes a Viernes 07:00 - 18:00'),
                                                               ('Sede Norte', '3a Calle 5-40, Zona 18, Guatemala', '2234-1122', 'Lunes a Sábado 08:00 - 17:00');

INSERT INTO specialties (name, description) VALUES
                                                ('Medicina General', 'Consulta médica general y chequeos de rutina'),
                                                ('Pediatría', 'Atención médica especializada en niños y adolescentes'),
                                                ('Ginecología', 'Salud reproductiva y ginecológica de la mujer'),
                                                ('Cardiología', 'Diagnóstico y tratamiento de enfermedades del corazón');

INSERT INTO branch_specialty (branch_id, specialty_id)
SELECT b.id, s.id FROM branches b, specialties s
WHERE b.name = 'Sede Central';

INSERT INTO branch_specialty (branch_id, specialty_id)
SELECT b.id, s.id FROM branches b, specialties s
WHERE b.name = 'Sede Norte' AND s.name IN ('Medicina General', 'Pediatría');