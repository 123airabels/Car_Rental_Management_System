-- Adds a unique payment code to every payment, and records which staff member received it.
-- The customer gets the code when they submit a payment and shows it to the staff; staff look it up, confirm, and issue the receipt.
--
-- Run once on an existing database (new databases get this from schema.sql):
--   psql -U postgres -d car_rental_db -f payment_codes.sql

ALTER TABLE public.payment ADD COLUMN IF NOT EXISTS payment_code character varying(20);
ALTER TABLE public.payment ADD COLUMN IF NOT EXISTS received_by integer REFERENCES public.staff(staff_id);

-- Give existing payments a code (PAY- + 6 characters from an unambiguous alphabet: no 0/O/1/I/L).
UPDATE public.payment p
SET payment_code = 'PAY-' || (
    SELECT string_agg(substr('ABCDEFGHJKMNPQRSTUVWXYZ23456789', 1 + floor(random() * 31)::int, 1), '')
    FROM generate_series(1, 6) WHERE p.payment_id IS NOT NULL)
WHERE payment_code IS NULL;

DO $$ BEGIN
    ALTER TABLE public.payment ADD CONSTRAINT payment_payment_code_key UNIQUE (payment_code);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL; END $$;
