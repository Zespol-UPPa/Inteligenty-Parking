--
-- PostgreSQL database dump
--

\restrict ltDAAQbCYAfFdKb1mLx8rl6gm05V8fOsm9V95jdv83YfK0MIbtstZ9Pdb5zJYtM

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-12-25 18:00:12

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
-- TOC entry 220 (class 1259 OID 25183)
-- Name: admin; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.admin (
    admin_id integer NOT NULL,
    first_name character varying(20) NOT NULL,
    last_name character varying(20) NOT NULL,
    phone_number character varying(15),
    pesel_number character varying(11) NOT NULL,
    ref_account_id integer NOT NULL,
    ref_company_id integer NOT NULL
);


ALTER TABLE public.admin OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 25182)
-- Name: admin_admin_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.admin_admin_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.admin_admin_id_seq OWNER TO postgres;

--
-- TOC entry 4912 (class 0 OID 0)
-- Dependencies: 219
-- Name: admin_admin_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.admin_admin_id_seq OWNED BY public.admin.admin_id;


--
-- TOC entry 4755 (class 2604 OID 25186)
-- Name: admin admin_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin ALTER COLUMN admin_id SET DEFAULT nextval('public.admin_admin_id_seq'::regclass);


--
-- TOC entry 4906 (class 0 OID 25183)
-- Dependencies: 220
-- Data for Name: admin; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.admin (admin_id, first_name, last_name, phone_number, pesel_number, ref_account_id, ref_company_id) FROM stdin;
1	Anna	Kowalska	600123001	90010112345	1	1
2	Piotr	Nowak	600123002	89050523456	2	1
3	Katarzyna	Wiśniewska	600123003	92080834567	3	1
4	Michał	Zieliński	600123004	87030345678	4	1
5	Bartosz	Glik	600123005	87222345678	5	1
\.


--
-- TOC entry 4913 (class 0 OID 0)
-- Dependencies: 219
-- Name: admin_admin_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.admin_admin_id_seq', 5, true);


--
-- TOC entry 4757 (class 2606 OID 25194)
-- Name: admin admin_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.admin
    ADD CONSTRAINT admin_pkey PRIMARY KEY (admin_id);


-- Completed on 2025-12-25 18:00:14

--
-- PostgreSQL database dump complete
--

\unrestrict ltDAAQbCYAfFdKb1mLx8rl6gm05V8fOsm9V95jdv83YfK0MIbtstZ9Pdb5zJYtM

