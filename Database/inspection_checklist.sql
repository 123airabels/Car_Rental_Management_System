-- Condition checklist for pickup and return inspections (dents, broken lights, scratches, ...).
-- The application also creates this table by itself at startup (see DataSeeder); run this file only if you prefer to do it by hand.
CREATE TABLE IF NOT EXISTS inspection_item (
    item_id serial PRIMARY KEY,
    inspection_id integer NOT NULL REFERENCES vehicle_inspection(inspection_id) ON DELETE CASCADE,
    item_name character varying(60) NOT NULL,
    item_condition character varying(30) NOT NULL,
    notes text,
    CONSTRAINT inspection_item_unique UNIQUE (inspection_id, item_name)
);
