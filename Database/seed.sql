
BEGIN;

-- 1. VEHICLE CATEGORIES
INSERT INTO public.vehicle_category
    (category_name, description, category_status)
SELECT v.category_name, v.description, 'Active'
FROM (VALUES
    ('Sedan',  'Comfortable cars for everyday travel'),
    ('SUV',    'Sport utility vehicles for family trips'),
    ('Van',    'Vehicles with more passenger capacity'),
    ('Pickup', 'Vehicles suitable for carrying cargo')
) AS v(category_name, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.vehicle_category c
    WHERE c.category_name = v.category_name
);

-- 2. DAILY RENTAL RATES
INSERT INTO public.rental_rate
    (category_id, rental_period_type, rate_amount, rate_status)
SELECT c.category_id, v.rental_period_type, v.rate_amount, 'Active'
FROM (VALUES
    ('Sedan',  'Daily', 1500.00::numeric),
    ('SUV',    'Daily', 2500.00::numeric),
    ('Van',    'Daily', 3000.00::numeric),
    ('Pickup', 'Daily', 2800.00::numeric)
) AS v(category_name, rental_period_type, rate_amount)
JOIN public.vehicle_category c
    ON c.category_name = v.category_name
WHERE NOT EXISTS (
    SELECT 1
    FROM public.rental_rate r
    WHERE r.category_id = c.category_id
      AND r.rental_period_type = v.rental_period_type
);

-- 3. SAMPLE VEHICLES
INSERT INTO public.vehicle
    (category_id, plate_number, make, model, year,
     color, seating_capacity, mileage, fuel_type,
     unit_rate_adjustment, status)
SELECT
    c.category_id, v.plate_number, v.make, v.model, v.year,
    v.color, v.seating_capacity, v.mileage, v.fuel_type,
    v.unit_rate_adjustment, 'Available'
FROM (VALUES
    ('Sedan',  'ABC-1234', 'Toyota',   'Vios',     2023, 'White',  5, 18000::numeric, 'Gasoline', 0::numeric),
    ('Sedan',  'ABC-1235', 'Honda',    'City',     2024, 'Silver', 5, 12000::numeric, 'Gasoline', 0::numeric),
    ('SUV',    'DEF-2345', 'Toyota',   'Fortuner', 2023, 'Black',  7, 22000::numeric, 'Diesel',   0::numeric),
    ('SUV',    'DEF-2346', 'Mitsubishi','Montero', 2022, 'Gray',   7, 30000::numeric, 'Diesel',   0::numeric),
    ('Van',    'GHI-3456', 'Toyota',   'HiAce',    2024, 'White', 12, 15000::numeric, 'Diesel',   0::numeric),
    ('Van',    'GHI-3457', 'Nissan',   'Urvan',    2023, 'Silver',12, 21000::numeric, 'Diesel',   0::numeric),
    ('Pickup', 'JKL-4567', 'Toyota',   'Hilux',    2024, 'White',  5, 14000::numeric, 'Diesel',   0::numeric),
    ('Pickup', 'JKL-4568', 'Ford',     'Ranger',   2023, 'Blue',   5, 19000::numeric, 'Diesel',   0::numeric)
) AS v(category_name, plate_number, make, model, year,
       color, seating_capacity, mileage, fuel_type,
       unit_rate_adjustment)
JOIN public.vehicle_category c
    ON c.category_name = v.category_name
WHERE NOT EXISTS (
    SELECT 1
    FROM public.vehicle existing
    WHERE existing.plate_number = v.plate_number
);

COMMIT;
