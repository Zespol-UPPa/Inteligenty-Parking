--
-- PostgreSQL database dump
--

\restrict 0IC5BO4bjqFcGPVmdjsCIepgQX5Orakfet1A2lhObIGGggeJwfK191OMHfoSzuC

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-12-25 18:03:07

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
-- TOC entry 859 (class 1247 OID 25209)
-- Name: activity_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.activity_type AS ENUM (
    'deposit',
    'reservation',
    'parking'
);


ALTER TYPE public.activity_type OWNER TO postgres;

--
-- TOC entry 853 (class 1247 OID 25136)
-- Name: status_paid; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.status_paid AS ENUM (
    'Pending',
    'Paid',
    'Failed',
    'Cancelled'
);


ALTER TYPE public.status_paid OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 220 (class 1259 OID 25146)
-- Name: virtual_payment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.virtual_payment (
    payment_id integer CONSTRAINT virtual_payment_id_payment_not_null NOT NULL,
    amount_minor integer NOT NULL,
    currency_code character varying(5) NOT NULL,
    status_paid public.status_paid DEFAULT 'Pending'::public.status_paid NOT NULL,
    date_transaction timestamp without time zone NOT NULL,
    ref_account_id integer,
    ref_session_id integer CONSTRAINT virtual_payment_id_session_not_null NOT NULL,
    activity public.activity_type
);


ALTER TABLE public.virtual_payment OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 25145)
-- Name: virtual_payment_id_payment_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.virtual_payment_id_payment_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.virtual_payment_id_payment_seq OWNER TO postgres;

--
-- TOC entry 4919 (class 0 OID 0)
-- Dependencies: 219
-- Name: virtual_payment_id_payment_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.virtual_payment_id_payment_seq OWNED BY public.virtual_payment.payment_id;


--
-- TOC entry 4761 (class 2604 OID 25149)
-- Name: virtual_payment payment_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.virtual_payment ALTER COLUMN payment_id SET DEFAULT nextval('public.virtual_payment_id_payment_seq'::regclass);


--
-- TOC entry 4913 (class 0 OID 25146)
-- Dependencies: 220
-- Data for Name: virtual_payment; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.virtual_payment (payment_id, amount_minor, currency_code, status_paid, date_transaction, ref_account_id, ref_session_id, activity) FROM stdin;
1	1100	PLN	Paid	2025-12-20 10:02:00	16	1	parking
2	1210	PLN	Paid	2025-12-20 10:07:00	17	2	parking
3	1100	PLN	Paid	2025-12-20 10:04:10	18	3	parking
4	1440	PLN	Paid	2025-12-20 10:12:40	19	4	parking
5	1495	PLN	Paid	2025-12-20 10:10:00	20	5	parking
6	1200	PLN	Paid	2025-12-20 10:19:30	21	6	parking
7	1300	PLN	Paid	2025-12-20 10:30:10	23	7	parking
8	1320	PLN	Paid	2025-12-20 10:23:00	22	8	parking
9	1000	PLN	Paid	2025-12-20 07:00:10	16	1	reservation
10	1000	PLN	Paid	2025-12-26 18:00:10	16	1	reservation
\.


--
-- TOC entry 4920 (class 0 OID 0)
-- Dependencies: 219
-- Name: virtual_payment_id_payment_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.virtual_payment_id_payment_seq', 10, true);


--
-- TOC entry 4764 (class 2606 OID 25159)
-- Name: virtual_payment virtual_payment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.virtual_payment
    ADD CONSTRAINT virtual_payment_pkey PRIMARY KEY (payment_id);


-- Completed on 2025-12-25 18:03:09

--
-- PostgreSQL database dump complete
--

\unrestrict 0IC5BO4bjqFcGPVmdjsCIepgQX5Orakfet1A2lhObIGGggeJwfK191OMHfoSzuC

