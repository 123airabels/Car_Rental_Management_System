-- Adds the column that stores the customer's proof-of-payment screenshot (relative path under app.upload-dir).
-- Run once on an existing database (new databases get this from schema.sql; the app also adds it on start-up):
--   psql -U postgres -d car_rental_db -f payment_proof.sql

ALTER TABLE public.payment ADD COLUMN IF NOT EXISTS proof_path character varying(255);
