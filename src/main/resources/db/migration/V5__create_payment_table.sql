-- V5__create_payment_table.sql
CREATE TABLE payment (
                         id                  SERIAL PRIMARY KEY,
                         transaction_number  VARCHAR(30)   NOT NULL UNIQUE,
                         appointment_id      INTEGER       NOT NULL REFERENCES appointment(id),
                         method              VARCHAR(20)   NOT NULL,
                         status              VARCHAR(20)   NOT NULL,
                         rejection_reason    VARCHAR(30),
                         amount              NUMERIC(10,2) NOT NULL,
                         card_holder_name    VARCHAR(100),
                         card_last_four      VARCHAR(4),
                         card_brand          VARCHAR(20),
                         amount_received     NUMERIC(10,2),
                         change_given        NUMERIC(10,2),
                         processed_by        INTEGER REFERENCES internal_users(id),
                         paid_at             TIMESTAMP     NOT NULL DEFAULT now(),
                         created_at          TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_appointment ON payment(appointment_id);
CREATE INDEX idx_payment_transaction_number ON payment(transaction_number);