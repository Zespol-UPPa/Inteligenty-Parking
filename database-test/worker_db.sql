--
-- PostgreSQL database dump
--

\restrict yUwI7UuySheH1wuYapxfnPmJoyRDqbx5fcZRk7le7ntxv4Xs8gcS6EezSGlm6h5

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-12-25 18:03:40

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
-- TOC entry 4912 (class 0 OID 0)
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
-- TOC entry 4906 (class 0 OID 25162)
-- Dependencies: 220
-- Data for Name: worker; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.worker (worker_id, first_name, last_name, phone_number, pesel_number, ref_account_id, ref_company_id, ref_parking_id) FROM stdin;
1	Ewa	Mazur	600200002	92020222222	6	1	1
2	Tomasz	Wójcik	600200003	93030333333	7	1	2
3	Natalia	Krawczyk	600200004	94040444444	8	1	2
4	Paweł	Kaczmarek	600200005	95050555555	9	1	3
5	Karolina	Piotrowska	600200006	96060666666	10	1	3
6	Mateusz	Grabowski	600200007	97070777777	11	1	4
7	Alicja	Zając	600200008	98080888888	12	1	4
8	Kamil	Król	600200009	99090999999	13	1	5
9	Monika	Jankowska	600200010	00010100000	14	1	5
10	Jan	Kowalczyk	600200001	91010111111	15	1	1
\.


--
-- TOC entry 4913 (class 0 OID 0)
-- Dependencies: 219
-- Name: worker_worker_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.worker_worker_id_seq', 10, true);


--
-- TOC entry 4757 (class 2606 OID 25173)
-- Name: worker worker_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.worker
    ADD CONSTRAINT worker_pkey PRIMARY KEY (worker_id);


-- Completed on 2025-12-25 18:03:42

--
-- PostgreSQL database dump complete
--

\unrestrict yUwI7UuySheH1wuYapxfnPmJoyRDqbx5fcZRk7le7ntxv4Xs8gcS6EezSGlm6h5

