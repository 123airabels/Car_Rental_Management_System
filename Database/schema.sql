--
-- PostgreSQL database dump
--

\restrict EoA2xkFwq80ElzPBPMHJ50cebLQQgJkHGFgVOeKYtIhQCyY5TYYzLYW6VDJSjGE

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_log (
    audit_id integer NOT NULL,
    staff_id integer NOT NULL,
    action_type character varying(100) NOT NULL,
    table_name character varying(100) NOT NULL,
    record_id integer,
    action_description text,
    action_datetime timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: audit_log_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.audit_log_audit_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: audit_log_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.audit_log_audit_id_seq OWNED BY public.audit_log.audit_id;


--
-- Name: cancellation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cancellation (
    cancellation_id integer NOT NULL,
    rental_id integer NOT NULL,
    cancelled_by_customer_id integer,
    cancelled_by_staff_id integer,
    cancellation_reason text NOT NULL,
    cancelled_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    refund_eligible boolean DEFAULT false NOT NULL,
    refund_amount numeric(10,2),
    refund_status character varying(30),
    processed_at timestamp without time zone,
    CONSTRAINT chk_cancellation_actor CHECK ((((cancelled_by_customer_id IS NOT NULL) AND (cancelled_by_staff_id IS NULL)) OR ((cancelled_by_customer_id IS NULL) AND (cancelled_by_staff_id IS NOT NULL)))),
    CONSTRAINT chk_refund_amount CHECK (((refund_amount IS NULL) OR (refund_amount >= (0)::numeric)))
);


--
-- Name: cancellation_cancellation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cancellation_cancellation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cancellation_cancellation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cancellation_cancellation_id_seq OWNED BY public.cancellation.cancellation_id;


--
-- Name: customer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer (
    customer_id integer NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    email character varying(255) NOT NULL,
    phone_number character varying(30) NOT NULL,
    address text,
    password_hash text NOT NULL,
    account_status character varying(30) DEFAULT 'Active'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: customer_customer_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_customer_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer_customer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customer_customer_id_seq OWNED BY public.customer.customer_id;


--
-- Name: damage_fee; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.damage_fee (
    damage_fee_id integer NOT NULL,
    damage_id integer NOT NULL,
    amount numeric(10,2) NOT NULL,
    assessment_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fee_status character varying(30) DEFAULT 'Pending Assessment'::character varying NOT NULL,
    dispute_reason text,
    resolved_at timestamp without time zone,
    resolution_notes text,
    CONSTRAINT chk_damage_fee_amount CHECK ((amount >= (0)::numeric))
);


--
-- Name: damage_fee_damage_fee_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.damage_fee_damage_fee_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: damage_fee_damage_fee_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.damage_fee_damage_fee_id_seq OWNED BY public.damage_fee.damage_fee_id;


--
-- Name: damage_incident; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.damage_incident (
    damage_id integer NOT NULL,
    inspection_id integer NOT NULL,
    damage_type character varying(100),
    description text NOT NULL,
    location_on_vehicle character varying(100),
    severity character varying(30),
    reported_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status character varying(30) DEFAULT 'Open'::character varying NOT NULL,
    resolved_at timestamp without time zone
);


--
-- Name: damage_incident_damage_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.damage_incident_damage_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: damage_incident_damage_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.damage_incident_damage_id_seq OWNED BY public.damage_incident.damage_id;


--
-- Name: driver_license; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.driver_license (
    license_id integer NOT NULL,
    customer_id integer NOT NULL,
    license_number character varying(100) NOT NULL,
    license_type character varying(50) NOT NULL,
    issue_date date NOT NULL,
    expiration_date date NOT NULL,
    license_front_photo text NOT NULL,
    license_back_photo text NOT NULL,
    verification_status character varying(30) DEFAULT 'Pending'::character varying NOT NULL,
    rejection_reason text,
    verified_at timestamp without time zone,
    verified_by_staff_id integer,
    CONSTRAINT chk_license_dates CHECK ((expiration_date > issue_date))
);


--
-- Name: driver_license_license_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.driver_license_license_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: driver_license_license_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.driver_license_license_id_seq OWNED BY public.driver_license.license_id;


--
-- Name: inspection_photo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inspection_photo (
    photo_id integer NOT NULL,
    inspection_id integer NOT NULL,
    photo_path text NOT NULL,
    photo_description text,
    uploaded_by_customer_id integer,
    uploaded_by_staff_id integer,
    uploaded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_photo_uploader CHECK ((((uploaded_by_customer_id IS NOT NULL) AND (uploaded_by_staff_id IS NULL)) OR ((uploaded_by_customer_id IS NULL) AND (uploaded_by_staff_id IS NOT NULL))))
);


--
-- Name: inspection_photo_photo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inspection_photo_photo_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inspection_photo_photo_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inspection_photo_photo_id_seq OWNED BY public.inspection_photo.photo_id;


--
-- Name: payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment (
    payment_id integer NOT NULL,
    rental_id integer,
    damage_fee_id integer,
    amount numeric(10,2) NOT NULL,
    payment_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    payment_method character varying(50),
    payment_status character varying(30) DEFAULT 'Pending'::character varying NOT NULL,
    reference_number character varying(100),
    receipt_number character varying(100),
    payment_code character varying(20),
    received_by integer,
    proof_path character varying(255),
    CONSTRAINT chk_payment_amount CHECK ((amount > (0)::numeric)),
    CONSTRAINT chk_payment_target CHECK ((((rental_id IS NOT NULL) AND (damage_fee_id IS NULL)) OR ((rental_id IS NULL) AND (damage_fee_id IS NOT NULL))))
);


--
-- Name: payment_payment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payment_payment_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payment_payment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payment_payment_id_seq OWNED BY public.payment.payment_id;


--
-- Name: rental; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rental (
    rental_id integer NOT NULL,
    customer_id integer NOT NULL,
    vehicle_id integer NOT NULL,
    pickup_datetime timestamp without time zone NOT NULL,
    return_datetime timestamp without time zone NOT NULL,
    actual_return_datetime timestamp without time zone,
    late_fee numeric(10,2) DEFAULT 0 NOT NULL,
    late_fee_status character varying(30),
    pickup_location text,
    return_location text,
    pickup_location_type character varying(30),
    return_location_type character varying(30),
    pickup_fee numeric(10,2) DEFAULT 0 NOT NULL,
    return_fee numeric(10,2) DEFAULT 0 NOT NULL,
    rental_status character varying(30) DEFAULT 'Pending'::character varying NOT NULL,
    estimated_cost numeric(10,2),
    final_cost numeric(10,2),
    rate_id integer NOT NULL,
    approved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_actual_return CHECK (((actual_return_datetime IS NULL) OR (actual_return_datetime >= pickup_datetime))),
    CONSTRAINT chk_estimated_cost CHECK (((estimated_cost IS NULL) OR (estimated_cost >= (0)::numeric))),
    CONSTRAINT chk_final_cost CHECK (((final_cost IS NULL) OR (final_cost >= (0)::numeric))),
    CONSTRAINT chk_late_fee CHECK ((late_fee >= (0)::numeric)),
    CONSTRAINT chk_pickup_fee CHECK ((pickup_fee >= (0)::numeric)),
    CONSTRAINT chk_rental_dates CHECK ((return_datetime > pickup_datetime)),
    CONSTRAINT chk_return_fee CHECK ((return_fee >= (0)::numeric))
);


--
-- Name: rental_rate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rental_rate (
    rate_id integer NOT NULL,
    category_id integer NOT NULL,
    rental_period_type character varying(30) NOT NULL,
    rate_amount numeric(10,2) NOT NULL,
    rate_status character varying(30) DEFAULT 'Active'::character varying NOT NULL,
    CONSTRAINT chk_rate_amount CHECK ((rate_amount >= (0)::numeric))
);


--
-- Name: rental_rate_rate_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rental_rate_rate_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rental_rate_rate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rental_rate_rate_id_seq OWNED BY public.rental_rate.rate_id;


--
-- Name: rental_rental_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rental_rental_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rental_rental_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rental_rental_id_seq OWNED BY public.rental.rental_id;


--
-- Name: rental_terms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rental_terms (
    terms_id integer NOT NULL,
    rental_id integer NOT NULL,
    terms_version character varying(50) NOT NULL,
    terms_text text NOT NULL,
    accepted boolean DEFAULT false NOT NULL,
    accepted_at timestamp without time zone,
    CONSTRAINT chk_terms_acceptance CHECK (((accepted = false) OR (accepted_at IS NOT NULL)))
);


--
-- Name: rental_terms_terms_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rental_terms_terms_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rental_terms_terms_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rental_terms_terms_id_seq OWNED BY public.rental_terms.terms_id;


--
-- Name: staff; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff (
    staff_id integer NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash text NOT NULL,
    role character varying(50) NOT NULL,
    account_status character varying(30) DEFAULT 'Active'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_staff_role CHECK (((role)::text = ANY ((ARRAY['Staff'::character varying, 'Finance'::character varying, 'Admin'::character varying, 'System Admin'::character varying])::text[])))
);


--
-- Name: staff_staff_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.staff_staff_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: staff_staff_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.staff_staff_id_seq OWNED BY public.staff.staff_id;


--
-- Name: vehicle; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vehicle (
    vehicle_id integer NOT NULL,
    category_id integer NOT NULL,
    plate_number character varying(30) NOT NULL,
    make character varying(100) NOT NULL,
    model character varying(100) NOT NULL,
    year integer,
    color character varying(50),
    seating_capacity integer,
    mileage numeric(12,2),
    fuel_type character varying(50),
    unit_rate_adjustment numeric(10,2) DEFAULT 0 NOT NULL,
    status character varying(30) DEFAULT 'Available'::character varying NOT NULL,
    CONSTRAINT chk_mileage CHECK (((mileage IS NULL) OR (mileage >= (0)::numeric))),
    CONSTRAINT chk_seating_capacity CHECK (((seating_capacity IS NULL) OR (seating_capacity > 0))),
    CONSTRAINT chk_vehicle_year CHECK (((year IS NULL) OR (year >= 1900)))
);


--
-- Name: vehicle_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vehicle_category (
    category_id integer NOT NULL,
    category_name character varying(100) NOT NULL,
    description text,
    category_status character varying(30) DEFAULT 'Active'::character varying NOT NULL
);


--
-- Name: vehicle_category_category_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vehicle_category_category_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vehicle_category_category_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vehicle_category_category_id_seq OWNED BY public.vehicle_category.category_id;


--
-- Name: vehicle_inspection; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vehicle_inspection (
    inspection_id integer NOT NULL,
    rental_id integer NOT NULL,
    inspection_type character varying(50) NOT NULL,
    inspection_datetime timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    mileage numeric(12,2),
    fuel_level character varying(30),
    condition_notes text,
    customer_notes text,
    performed_by_staff_id integer,
    performed_by_customer_id integer,
    CONSTRAINT chk_inspection_mileage CHECK (((mileage IS NULL) OR (mileage >= (0)::numeric))),
    CONSTRAINT chk_inspection_performer CHECK ((((performed_by_staff_id IS NOT NULL) AND (performed_by_customer_id IS NULL)) OR ((performed_by_staff_id IS NULL) AND (performed_by_customer_id IS NOT NULL))))
);


--
-- Name: vehicle_inspection_inspection_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vehicle_inspection_inspection_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vehicle_inspection_inspection_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vehicle_inspection_inspection_id_seq OWNED BY public.vehicle_inspection.inspection_id;


--
-- Name: vehicle_maintenance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vehicle_maintenance (
    maintenance_id integer NOT NULL,
    vehicle_id integer NOT NULL,
    maintenance_type character varying(100),
    description text,
    maintenance_date date NOT NULL,
    cost numeric(10,2),
    status character varying(30) DEFAULT 'Pending'::character varying NOT NULL,
    notes text,
    completed_date date,
    CONSTRAINT chk_maintenance_cost CHECK (((cost IS NULL) OR (cost >= (0)::numeric))),
    CONSTRAINT chk_maintenance_dates CHECK (((completed_date IS NULL) OR (completed_date >= maintenance_date)))
);


--
-- Name: vehicle_maintenance_maintenance_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vehicle_maintenance_maintenance_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vehicle_maintenance_maintenance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vehicle_maintenance_maintenance_id_seq OWNED BY public.vehicle_maintenance.maintenance_id;


--
-- Name: vehicle_vehicle_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vehicle_vehicle_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vehicle_vehicle_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vehicle_vehicle_id_seq OWNED BY public.vehicle.vehicle_id;


--
-- Name: audit_log audit_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log ALTER COLUMN audit_id SET DEFAULT nextval('public.audit_log_audit_id_seq'::regclass);


--
-- Name: cancellation cancellation_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cancellation ALTER COLUMN cancellation_id SET DEFAULT nextval('public.cancellation_cancellation_id_seq'::regclass);


--
-- Name: customer customer_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer ALTER COLUMN customer_id SET DEFAULT nextval('public.customer_customer_id_seq'::regclass);


--
-- Name: damage_fee damage_fee_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_fee ALTER COLUMN damage_fee_id SET DEFAULT nextval('public.damage_fee_damage_fee_id_seq'::regclass);


--
-- Name: damage_incident damage_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_incident ALTER COLUMN damage_id SET DEFAULT nextval('public.damage_incident_damage_id_seq'::regclass);


--
-- Name: driver_license license_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_license ALTER COLUMN license_id SET DEFAULT nextval('public.driver_license_license_id_seq'::regclass);


--
-- Name: inspection_photo photo_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_photo ALTER COLUMN photo_id SET DEFAULT nextval('public.inspection_photo_photo_id_seq'::regclass);


--
-- Name: payment payment_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment ALTER COLUMN payment_id SET DEFAULT nextval('public.payment_payment_id_seq'::regclass);


--
-- Name: rental rental_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental ALTER COLUMN rental_id SET DEFAULT nextval('public.rental_rental_id_seq'::regclass);


--
-- Name: rental_rate rate_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_rate ALTER COLUMN rate_id SET DEFAULT nextval('public.rental_rate_rate_id_seq'::regclass);


--
-- Name: rental_terms terms_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_terms ALTER COLUMN terms_id SET DEFAULT nextval('public.rental_terms_terms_id_seq'::regclass);


--
-- Name: staff staff_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff ALTER COLUMN staff_id SET DEFAULT nextval('public.staff_staff_id_seq'::regclass);


--
-- Name: vehicle vehicle_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle ALTER COLUMN vehicle_id SET DEFAULT nextval('public.vehicle_vehicle_id_seq'::regclass);


--
-- Name: vehicle_category category_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_category ALTER COLUMN category_id SET DEFAULT nextval('public.vehicle_category_category_id_seq'::regclass);


--
-- Name: vehicle_inspection inspection_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_inspection ALTER COLUMN inspection_id SET DEFAULT nextval('public.vehicle_inspection_inspection_id_seq'::regclass);


--
-- Name: vehicle_maintenance maintenance_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_maintenance ALTER COLUMN maintenance_id SET DEFAULT nextval('public.vehicle_maintenance_maintenance_id_seq'::regclass);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (audit_id);


--
-- Name: cancellation cancellation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cancellation
    ADD CONSTRAINT cancellation_pkey PRIMARY KEY (cancellation_id);


--
-- Name: cancellation cancellation_rental_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cancellation
    ADD CONSTRAINT cancellation_rental_id_key UNIQUE (rental_id);


--
-- Name: customer customer_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_email_key UNIQUE (email);


--
-- Name: customer customer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (customer_id);


--
-- Name: damage_fee damage_fee_damage_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_fee
    ADD CONSTRAINT damage_fee_damage_id_key UNIQUE (damage_id);


--
-- Name: damage_fee damage_fee_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_fee
    ADD CONSTRAINT damage_fee_pkey PRIMARY KEY (damage_fee_id);


--
-- Name: damage_incident damage_incident_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_incident
    ADD CONSTRAINT damage_incident_pkey PRIMARY KEY (damage_id);


--
-- Name: driver_license driver_license_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_license
    ADD CONSTRAINT driver_license_pkey PRIMARY KEY (license_id);


--
-- Name: inspection_photo inspection_photo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_photo
    ADD CONSTRAINT inspection_photo_pkey PRIMARY KEY (photo_id);


--
-- Name: payment payment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey PRIMARY KEY (payment_id);


--
-- Name: payment payment_payment_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_payment_code_key UNIQUE (payment_code);


--
-- Name: payment payment_received_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_received_by_fkey FOREIGN KEY (received_by) REFERENCES public.staff(staff_id);


--
-- Name: rental rental_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental
    ADD CONSTRAINT rental_pkey PRIMARY KEY (rental_id);


--
-- Name: rental_rate rental_rate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_rate
    ADD CONSTRAINT rental_rate_pkey PRIMARY KEY (rate_id);


--
-- Name: rental_terms rental_terms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_terms
    ADD CONSTRAINT rental_terms_pkey PRIMARY KEY (terms_id);


--
-- Name: rental_terms rental_terms_rental_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_terms
    ADD CONSTRAINT rental_terms_rental_id_key UNIQUE (rental_id);


--
-- Name: staff staff_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff
    ADD CONSTRAINT staff_email_key UNIQUE (email);


--
-- Name: staff staff_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff
    ADD CONSTRAINT staff_pkey PRIMARY KEY (staff_id);


--
-- Name: vehicle_category vehicle_category_category_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_category
    ADD CONSTRAINT vehicle_category_category_name_key UNIQUE (category_name);


--
-- Name: vehicle_category vehicle_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_category
    ADD CONSTRAINT vehicle_category_pkey PRIMARY KEY (category_id);


--
-- Name: vehicle_inspection vehicle_inspection_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_inspection
    ADD CONSTRAINT vehicle_inspection_pkey PRIMARY KEY (inspection_id);


--
-- Name: vehicle_maintenance vehicle_maintenance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_maintenance
    ADD CONSTRAINT vehicle_maintenance_pkey PRIMARY KEY (maintenance_id);


--
-- Name: vehicle vehicle_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle
    ADD CONSTRAINT vehicle_pkey PRIMARY KEY (vehicle_id);


--
-- Name: vehicle vehicle_plate_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle
    ADD CONSTRAINT vehicle_plate_number_key UNIQUE (plate_number);


--
-- Name: idx_audit_datetime; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_datetime ON public.audit_log USING btree (action_datetime);


--
-- Name: idx_audit_staff; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_staff ON public.audit_log USING btree (staff_id);


--
-- Name: idx_damage_fee_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_damage_fee_status ON public.damage_fee USING btree (fee_status);


--
-- Name: idx_license_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_license_customer ON public.driver_license USING btree (customer_id);


--
-- Name: idx_license_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_license_status ON public.driver_license USING btree (verification_status);


--
-- Name: idx_maintenance_vehicle; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_maintenance_vehicle ON public.vehicle_maintenance USING btree (vehicle_id);


--
-- Name: idx_payment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_status ON public.payment USING btree (payment_status);


--
-- Name: idx_rental_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rental_customer ON public.rental USING btree (customer_id);


--
-- Name: idx_rental_dates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rental_dates ON public.rental USING btree (pickup_datetime, return_datetime);


--
-- Name: idx_rental_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rental_status ON public.rental USING btree (rental_status);


--
-- Name: idx_rental_vehicle; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rental_vehicle ON public.rental USING btree (vehicle_id);


--
-- Name: idx_vehicle_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vehicle_status ON public.vehicle USING btree (status);


--
-- Name: audit_log fk_audit_staff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT fk_audit_staff FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id);


--
-- Name: cancellation fk_cancellation_rental; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cancellation
    ADD CONSTRAINT fk_cancellation_rental FOREIGN KEY (rental_id) REFERENCES public.rental(rental_id);


--
-- Name: cancellation fk_cancelled_by_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cancellation
    ADD CONSTRAINT fk_cancelled_by_customer FOREIGN KEY (cancelled_by_customer_id) REFERENCES public.customer(customer_id);


--
-- Name: cancellation fk_cancelled_by_staff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cancellation
    ADD CONSTRAINT fk_cancelled_by_staff FOREIGN KEY (cancelled_by_staff_id) REFERENCES public.staff(staff_id);


--
-- Name: damage_fee fk_damage_fee_damage; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_fee
    ADD CONSTRAINT fk_damage_fee_damage FOREIGN KEY (damage_id) REFERENCES public.damage_incident(damage_id);


--
-- Name: damage_incident fk_damage_inspection; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_incident
    ADD CONSTRAINT fk_damage_inspection FOREIGN KEY (inspection_id) REFERENCES public.vehicle_inspection(inspection_id);


--
-- Name: vehicle_inspection fk_inspection_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_inspection
    ADD CONSTRAINT fk_inspection_customer FOREIGN KEY (performed_by_customer_id) REFERENCES public.customer(customer_id);


--
-- Name: vehicle_inspection fk_inspection_rental; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_inspection
    ADD CONSTRAINT fk_inspection_rental FOREIGN KEY (rental_id) REFERENCES public.rental(rental_id);


--
-- Name: vehicle_inspection fk_inspection_staff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_inspection
    ADD CONSTRAINT fk_inspection_staff FOREIGN KEY (performed_by_staff_id) REFERENCES public.staff(staff_id);


--
-- Name: driver_license fk_license_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_license
    ADD CONSTRAINT fk_license_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);


--
-- Name: driver_license fk_license_verified_staff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.driver_license
    ADD CONSTRAINT fk_license_verified_staff FOREIGN KEY (verified_by_staff_id) REFERENCES public.staff(staff_id);


--
-- Name: vehicle_maintenance fk_maintenance_vehicle; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle_maintenance
    ADD CONSTRAINT fk_maintenance_vehicle FOREIGN KEY (vehicle_id) REFERENCES public.vehicle(vehicle_id);


--
-- Name: payment fk_payment_damage_fee; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fk_payment_damage_fee FOREIGN KEY (damage_fee_id) REFERENCES public.damage_fee(damage_fee_id);


--
-- Name: payment fk_payment_rental; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fk_payment_rental FOREIGN KEY (rental_id) REFERENCES public.rental(rental_id);


--
-- Name: inspection_photo fk_photo_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_photo
    ADD CONSTRAINT fk_photo_customer FOREIGN KEY (uploaded_by_customer_id) REFERENCES public.customer(customer_id);


--
-- Name: inspection_photo fk_photo_inspection; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_photo
    ADD CONSTRAINT fk_photo_inspection FOREIGN KEY (inspection_id) REFERENCES public.vehicle_inspection(inspection_id);


--
-- Name: inspection_photo fk_photo_staff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inspection_photo
    ADD CONSTRAINT fk_photo_staff FOREIGN KEY (uploaded_by_staff_id) REFERENCES public.staff(staff_id);


--
-- Name: rental fk_rental_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental
    ADD CONSTRAINT fk_rental_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);


--
-- Name: rental fk_rental_rate; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental
    ADD CONSTRAINT fk_rental_rate FOREIGN KEY (rate_id) REFERENCES public.rental_rate(rate_id);


--
-- Name: rental_rate fk_rental_rate_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_rate
    ADD CONSTRAINT fk_rental_rate_category FOREIGN KEY (category_id) REFERENCES public.vehicle_category(category_id);


--
-- Name: rental fk_rental_vehicle; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental
    ADD CONSTRAINT fk_rental_vehicle FOREIGN KEY (vehicle_id) REFERENCES public.vehicle(vehicle_id);


--
-- Name: rental_terms fk_terms_rental; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_terms
    ADD CONSTRAINT fk_terms_rental FOREIGN KEY (rental_id) REFERENCES public.rental(rental_id);


--
-- Name: vehicle fk_vehicle_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicle
    ADD CONSTRAINT fk_vehicle_category FOREIGN KEY (category_id) REFERENCES public.vehicle_category(category_id);


--
-- PostgreSQL database dump complete
--

\unrestrict EoA2xkFwq80ElzPBPMHJ50cebLQQgJkHGFgVOeKYtIhQCyY5TYYzLYW6VDJSjGE

-- Condition checklist of an inspection (also created automatically at startup)
CREATE TABLE IF NOT EXISTS inspection_item (
    item_id serial PRIMARY KEY,
    inspection_id integer NOT NULL REFERENCES vehicle_inspection(inspection_id) ON DELETE CASCADE,
    item_name character varying(60) NOT NULL,
    item_condition character varying(30) NOT NULL,
    notes text,
    CONSTRAINT inspection_item_unique UNIQUE (inspection_id, item_name)
);
