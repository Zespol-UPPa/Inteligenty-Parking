--
-- PostgreSQL database dump
--

\restrict 2l8pbHTY5hHGoX8fs6FcZaY59XzwLOlP6MQkQhoX51euV6qfbdfDtBdD7TTMTWt

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-11-20 20:58:53

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
    ref_account_id integer CONSTRAINT virtual_payment_id_account_not_null NOT NULL,
    ref_session_id integer CONSTRAINT virtual_payment_id_session_not_null NOT NULL
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
-- TOC entry 4914 (class 0 OID 0)
-- Dependencies: 219
-- Name: virtual_payment_id_payment_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.virtual_payment_id_payment_seq OWNED BY public.virtual_payment.payment_id;


--
-- TOC entry 4758 (class 2604 OID 25149)
-- Name: virtual_payment payment_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.virtual_payment ALTER COLUMN payment_id SET DEFAULT nextval('public.virtual_payment_id_payment_seq'::regclass);


--
-- TOC entry 4761 (class 2606 OID 25159)
-- Name: virtual_payment virtual_payment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.virtual_payment
    ADD CONSTRAINT virtual_payment_pkey PRIMARY KEY (payment_id);


-- Completed on 2025-11-20 20:58:55

--
-- PostgreSQL database dump complete
--

\unrestrict 2l8pbHTY5hHGoX8fs6FcZaY59XzwLOlP6MQkQhoX51euV6qfbdfDtBdD7TTMTWt

