-- For databases created before this change: adds the column that stores staff's reasoning when a damage-fee dispute is decided.
ALTER TABLE public.damage_fee ADD COLUMN IF NOT EXISTS resolution_notes text;
