-- Makes the DATABASE refuse two bookings for the same car on overlapping dates.
-- The application already checks this (and locks the vehicle row while it does), so this is the safety net
-- if anything ever bypasses the API or two requests slip through at the same instant.
--
-- Run once, after schema.sql:
--   psql -U postgres -d car_rental_db -f booking_conflict_guard.sql
--
-- Needs the btree_gist extension (ships with PostgreSQL; creating it needs a superuser such as postgres).

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Step 1 - find existing clashes. This must return 0 rows before step 2 can succeed.
-- (Fix or cancel one booking of each pair first.)
SELECT a.rental_id AS booking_a, b.rental_id AS booking_b, a.vehicle_id
FROM public.rental a
JOIN public.rental b ON a.vehicle_id = b.vehicle_id AND a.rental_id < b.rental_id
WHERE a.rental_status IN ('Pending', 'Approved', 'Active')
  AND b.rental_status IN ('Pending', 'Approved', 'Active')
  AND a.pickup_datetime < b.return_datetime AND b.pickup_datetime < a.return_datetime;

-- Step 2 - the rule. Only bookings that hold the car count; Rejected, Cancelled and Completed ones are ignored.
-- tsrange is half-open, so a car returned at midnight on the 10th can be picked up at midnight on the 10th.
ALTER TABLE public.rental
    ADD CONSTRAINT excl_rental_no_overlap
    EXCLUDE USING gist (
        vehicle_id WITH =,
        tsrange(pickup_datetime, return_datetime) WITH &&
    )
    WHERE (rental_status IN ('Pending', 'Approved', 'Active'));
