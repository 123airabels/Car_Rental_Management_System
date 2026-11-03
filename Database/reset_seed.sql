-- Wipes ALL data (keeps the tables) and reloads the sample data.
-- Run from the Database folder:
--   psql -U postgres -d car_rental_db -f reset_seed.sql
-- DEVELOPMENT ONLY: this deletes every row in every table.
BEGIN;
TRUNCATE TABLE
    public.audit_log, public.cancellation, public.damage_fee, public.damage_incident,
    public.driver_license, public.inspection_photo, public.payment, public.rental,
    public.rental_rate, public.rental_terms, public.staff, public.customer,
    public.vehicle, public.vehicle_category, public.vehicle_inspection,
    public.vehicle_maintenance
RESTART IDENTITY CASCADE;
COMMIT;

\i seed.sql
