-- V3__add_price_to_branch_specialty.sql
ALTER TABLE branch_specialty
    ADD COLUMN price NUMERIC(10,2) NOT NULL DEFAULT 0.00;

ALTER TABLE branch_specialty
    ALTER COLUMN price DROP DEFAULT;