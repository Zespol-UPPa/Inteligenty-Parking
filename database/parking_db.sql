--
-- PostgreSQL database dump
--

\restrict bukvjwadz5R9Khsp7CWQABtU20YTZPeGbxioMdkunmgVcJcLV2aWMj4dOsZ8uKK

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-11-20 20:58:10

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

--
-- TOC entry 870 (class 1247 OID 24969)
-- Name: payment_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.payment_status AS ENUM (
    'Session',
    'Unpaid',
    'Paid'
);


ALTER TYPE public.payment_status OWNER TO postgres;

--
-- TOC entry 879 (class 1247 OID 25019)
-- Name: reservation_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.reservation_status AS ENUM (
    'Paid',
    'Used',
    'Expired',
    'Cancelled'
);


ALTER TYPE public.reservation_status OWNER TO postgres;

--
-- TOC entry 864 (class 1247 OID 24951)
-- Name: spot_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.spot_status AS ENUM (
    'Available',
    'Unavailable'
);


ALTER TYPE public.spot_status OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 220 (class 1259 OID 24940)
-- Name: parking_location; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.parking_location (
    parking_id integer CONSTRAINT parking_location_id_parking_not_null NOT NULL,
    name_parking character varying(100) NOT NULL,
    address_line character varying(100) NOT NULL,
    ref_company_id integer NOT NULL
);


ALTER TABLE public.parking_location OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 24939)
-- Name: parking_location_id_parking_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.parking_location_id_parking_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.parking_location_id_parking_seq OWNER TO postgres;

--
-- TOC entry 4960 (class 0 OID 0)
-- Dependencies: 219
-- Name: parking_location_id_parking_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.parking_location_id_parking_seq OWNED BY public.parking_location.parking_id;


--
-- TOC entry 226 (class 1259 OID 25001)
-- Name: parking_pricing; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.parking_pricing (
    pricing_id integer NOT NULL,
    curency_code character varying(5) NOT NULL,
    rate_per_min integer NOT NULL,
    free_minutes integer NOT NULL,
    rounding_step_min integer NOT NULL,
    reservation_fee_minor integer NOT NULL,
    parking_id integer
);


ALTER TABLE public.parking_pricing OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 25000)
-- Name: parking_pricing_pricing_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.parking_pricing_pricing_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.parking_pricing_pricing_id_seq OWNER TO postgres;

--
-- TOC entry 4961 (class 0 OID 0)
-- Dependencies: 225
-- Name: parking_pricing_pricing_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.parking_pricing_pricing_id_seq OWNED BY public.parking_pricing.pricing_id;


--
-- TOC entry 224 (class 1259 OID 24976)
-- Name: parking_session; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.parking_session (
    session_id integer NOT NULL,
    entry_time timestamp without time zone NOT NULL,
    exit_time timestamp without time zone,
    price_total_minor numeric(10,2),
    payment_status public.payment_status DEFAULT 'Session'::public.payment_status NOT NULL,
    parking_id integer NOT NULL,
    spot_id integer NOT NULL,
    ref_vehicle_id integer CONSTRAINT parking_session_vehicle_id_not_null NOT NULL,
    ref_account_id integer CONSTRAINT parking_session_id_account_not_null NOT NULL
);


ALTER TABLE public.parking_session OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 24975)
-- Name: parking_session_session_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.parking_session_session_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.parking_session_session_id_seq OWNER TO postgres;

--
-- TOC entry 4962 (class 0 OID 0)
-- Dependencies: 223
-- Name: parking_session_session_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.parking_session_session_id_seq OWNED BY public.parking_session.session_id;


--
-- TOC entry 222 (class 1259 OID 24956)
-- Name: parking_spot; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.parking_spot (
    spot_id integer CONSTRAINT parking_spot_id_spot_not_null NOT NULL,
    code character varying(10) NOT NULL,
    floor_lvl integer NOT NULL,
    to_reserved boolean DEFAULT false NOT NULL,
    type public.spot_status DEFAULT 'Available'::public.spot_status NOT NULL,
    id_parking integer NOT NULL
);


ALTER TABLE public.parking_spot OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 24955)
-- Name: parking_spot_id_spot_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.parking_spot_id_spot_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.parking_spot_id_spot_seq OWNER TO postgres;

--
-- TOC entry 4963 (class 0 OID 0)
-- Dependencies: 221
-- Name: parking_spot_id_spot_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.parking_spot_id_spot_seq OWNED BY public.parking_spot.spot_id;


--
-- TOC entry 228 (class 1259 OID 25028)
-- Name: reservation_spot; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reservation_spot (
    reservation_id integer NOT NULL,
    valid_until timestamp without time zone NOT NULL,
    status_reservation public.reservation_status DEFAULT 'Paid'::public.reservation_status NOT NULL,
    spot_id integer NOT NULL,
    parking_id integer NOT NULL,
    ref_account_id integer CONSTRAINT reservation_spot_account_id_not_null NOT NULL
);


ALTER TABLE public.reservation_spot OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 25027)
-- Name: reservation_spot_reservation_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.reservation_spot_reservation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.reservation_spot_reservation_id_seq OWNER TO postgres;

--
-- TOC entry 4964 (class 0 OID 0)
-- Dependencies: 227
-- Name: reservation_spot_reservation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.reservation_spot_reservation_id_seq OWNED BY public.reservation_spot.reservation_id;


--
-- TOC entry 4784 (class 2604 OID 24943)
-- Name: parking_location parking_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_location ALTER COLUMN parking_id SET DEFAULT nextval('public.parking_location_id_parking_seq'::regclass);


--
-- TOC entry 4790 (class 2604 OID 25004)
-- Name: parking_pricing pricing_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_pricing ALTER COLUMN pricing_id SET DEFAULT nextval('public.parking_pricing_pricing_id_seq'::regclass);


--
-- TOC entry 4788 (class 2604 OID 24979)
-- Name: parking_session session_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session ALTER COLUMN session_id SET DEFAULT nextval('public.parking_session_session_id_seq'::regclass);


--
-- TOC entry 4785 (class 2604 OID 24959)
-- Name: parking_spot spot_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_spot ALTER COLUMN spot_id SET DEFAULT nextval('public.parking_spot_id_spot_seq'::regclass);


--
-- TOC entry 4791 (class 2604 OID 25031)
-- Name: reservation_spot reservation_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reservation_spot ALTER COLUMN reservation_id SET DEFAULT nextval('public.reservation_spot_reservation_id_seq'::regclass);


--
-- TOC entry 4794 (class 2606 OID 24949)
-- Name: parking_location parking_location_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_location
    ADD CONSTRAINT parking_location_pkey PRIMARY KEY (parking_id);


--
-- TOC entry 4800 (class 2606 OID 25012)
-- Name: parking_pricing parking_pricing_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_pricing
    ADD CONSTRAINT parking_pricing_pkey PRIMARY KEY (pricing_id);


--
-- TOC entry 4798 (class 2606 OID 24989)
-- Name: parking_session parking_session_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session
    ADD CONSTRAINT parking_session_pkey PRIMARY KEY (session_id);


--
-- TOC entry 4796 (class 2606 OID 24967)
-- Name: parking_spot parking_spot_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_spot
    ADD CONSTRAINT parking_spot_pkey PRIMARY KEY (spot_id);


--
-- TOC entry 4802 (class 2606 OID 25040)
-- Name: reservation_spot reservation_spot_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reservation_spot
    ADD CONSTRAINT reservation_spot_pkey PRIMARY KEY (reservation_id);


--
-- TOC entry 4805 (class 2606 OID 25013)
-- Name: parking_pricing parking_pricing_parking_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_pricing
    ADD CONSTRAINT parking_pricing_parking_id_fkey FOREIGN KEY (parking_id) REFERENCES public.parking_location(parking_id);


--
-- TOC entry 4803 (class 2606 OID 24990)
-- Name: parking_session parking_session_parking_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session
    ADD CONSTRAINT parking_session_parking_id_fkey FOREIGN KEY (parking_id) REFERENCES public.parking_location(parking_id);


--
-- TOC entry 4804 (class 2606 OID 24995)
-- Name: parking_session parking_session_spot_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session
    ADD CONSTRAINT parking_session_spot_id_fkey FOREIGN KEY (spot_id) REFERENCES public.parking_spot(spot_id);


--
-- TOC entry 4806 (class 2606 OID 25046)
-- Name: reservation_spot reservation_spot_parking_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reservation_spot
    ADD CONSTRAINT reservation_spot_parking_id_fkey FOREIGN KEY (parking_id) REFERENCES public.parking_location(parking_id);


--
-- TOC entry 4807 (class 2606 OID 25041)
-- Name: reservation_spot reservation_spot_spot_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reservation_spot
    ADD CONSTRAINT reservation_spot_spot_id_fkey FOREIGN KEY (spot_id) REFERENCES public.parking_spot(spot_id);


-- Completed on 2025-11-20 20:58:11

--
-- PostgreSQL database dump complete
--

\unrestrict bukvjwadz5R9Khsp7CWQABtU20YTZPeGbxioMdkunmgVcJcLV2aWMj4dOsZ8uKK

