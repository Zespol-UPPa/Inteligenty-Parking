--
-- PostgreSQL database dump
--

\restrict np4xP1qr7lGEpvJAJk4An8AcKVJ67a2WA1B4w4JyCBDiAVFNZKhe3LO6s86eFTs

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-11-20 21:00:12

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
-- TOC entry 220 (class 1259 OID 25162)
-- Name: worker; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.worker (
    worker_id integer NOT NULL,
    first_name character varying(20) NOT NULL,
    last_name character varying(20) NOT NULL,
    phone_number character varying(15),
    pesel_number character varying(11) NOT NULL,
    ref_account_id integer NOT NULL,
    ref_company_id integer NOT NULL,
    ref_parking_id integer NOT NULL
);


ALTER TABLE public.worker OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 25161)
-- Name: worker_worker_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.worker_worker_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.worker_worker_id_seq OWNER TO postgres;

--
-- TOC entry 4910 (class 0 OID 0)
-- Dependencies: 219
-- Name: worker_worker_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.worker_worker_id_seq OWNED BY public.worker.worker_id;


--
-- TOC entry 4755 (class 2604 OID 25165)
-- Name: worker worker_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.worker ALTER COLUMN worker_id SET DEFAULT nextval('public.worker_worker_id_seq'::regclass);


--
-- TOC entry 4757 (class 2606 OID 25173)
-- Name: worker worker_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.worker
    ADD CONSTRAINT worker_pkey PRIMARY KEY (worker_id);


-- Completed on 2025-11-20 21:00:14

--
-- PostgreSQL database dump complete
--

\unrestrict np4xP1qr7lGEpvJAJk4An8AcKVJ67a2WA1B4w4JyCBDiAVFNZKhe3LO6s86eFTs

