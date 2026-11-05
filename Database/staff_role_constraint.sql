-- Only the four application roles can be stored for a staff account. Without this, a typo or a bad insert could create a role
-- that matches no access rule. Run once on an existing database (new databases get this from schema.sql):
--   psql -U postgres -d car_rental_db -f staff_role_constraint.sql

ALTER TABLE public.staff DROP CONSTRAINT IF EXISTS chk_staff_role;
ALTER TABLE public.staff ADD CONSTRAINT chk_staff_role
    CHECK (role IN ('Staff', 'Finance', 'Admin', 'System Admin'));
