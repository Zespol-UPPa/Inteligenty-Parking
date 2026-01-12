--
-- PostgreSQL database dump
--

\restrict tGpet6Bjorfkc60NIVi5r2sBAqxQRWh2Qll14J1HHjBidN4HBvAecOBszOdFkVF

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-12-25 18:01:19

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
-- TOC entry 220 (class 1259 OID 25103)
-- Name: customer; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.customer (
    customer_id integer NOT NULL,
    first_name character varying(20) NOT NULL,
    last_name character varying(20) NOT NULL,
    ref_account_id integer NOT NULL
);


ALTER TABLE public.customer OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 25102)
-- Name: customer_customer_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.customer_customer_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.customer_customer_id_seq OWNER TO postgres;

--
-- TOC entry 4934 (class 0 OID 0)
-- Dependencies: 219
-- Name: customer_customer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.customer_customer_id_seq OWNED BY public.customer.customer_id;


--
-- TOC entry 222 (class 1259 OID 25114)
-- Name: vehicle; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.vehicle (
    vehicle_id integer NOT NULL,
    licence_plate character varying(15) NOT NULL,
    customer_id integer
);


ALTER TABLE public.vehicle OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 25113)
-- Name: vehicle_vehicle_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.vehicle_vehicle_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.vehicle_vehicle_id_seq OWNER TO postgres;

--
-- TOC entry 4935 (class 0 OID 0)
-- Dependencies: 221
-- Name: vehicle_vehicle_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.vehicle_vehicle_id_seq OWNED BY public.vehicle.vehicle_id;


--
-- TOC entry 224 (class 1259 OID 25124)
-- Name: wallet; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wallet (
    wallet_id integer NOT NULL,
    balance_minor numeric(10,2) NOT NULL,
    currency_code character varying(5) NOT NULL,
    customer_id integer CONSTRAINT wallet_ref_account_id_not_null NOT NULL
);


ALTER TABLE public.wallet OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 25123)
-- Name: wallet_wallet_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.wallet_wallet_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.wallet_wallet_id_seq OWNER TO postgres;

--
-- TOC entry 4936 (class 0 OID 0)
-- Dependencies: 223
-- Name: wallet_wallet_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.wallet_wallet_id_seq OWNED BY public.wallet.wallet_id;


--
-- TOC entry 4765 (class 2604 OID 25106)
-- Name: customer customer_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customer ALTER COLUMN customer_id SET DEFAULT nextval('public.customer_customer_id_seq'::regclass);


--
-- TOC entry 4766 (class 2604 OID 25117)
-- Name: vehicle vehicle_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vehicle ALTER COLUMN vehicle_id SET DEFAULT nextval('public.vehicle_vehicle_id_seq'::regclass);


--
-- TOC entry 4767 (class 2604 OID 25127)
-- Name: wallet wallet_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallet ALTER COLUMN wallet_id SET DEFAULT nextval('public.wallet_wallet_id_seq'::regclass);


--
-- TOC entry 4924 (class 0 OID 25103)
-- Dependencies: 220
-- Data for Name: customer; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.customer (customer_id, first_name, last_name, ref_account_id) FROM stdin;
1	Jan	Kowalski	16
2	Anna	Nowak	17
3	Piotr	Wiśniewski	18
4	Katarzyna	Wójcik	19
5	Paweł	Kaczmarek	20
6	Agnieszka	Mazur	21
7	Michał	Zieliński	22
8	Monika	Szymańska	23
9	Tomasz	Woźniak	24
10	Natalia	Dąbrowska	25
11	Marcin	Kozłowski	26
12	Karolina	Jankowska	27
13	Łukasz	Lewandowski	28
14	Magdalena	Piotrowska	29
15	Krzysztof	Grabowski	30
16	Aleksandra	Pawlak	31
17	Adam	Michalski	32
18	Dominika	Nowicka	33
19	Rafał	Król	34
20	Ewelina	Wieczorek	35
21	Mateusz	Jabłoński	36
22	Patrycja	Wróbel	37
23	Sebastian	Majewski	38
24	Julia	Olszewska	39
25	Bartosz	Stępień	40
26	Paulina	Jaworska	41
27	Damian	Malinowski	42
28	Weronika	Adamczyk	43
29	Kamil	Dudek	44
30	Justyna	Górska	45
31	Szymon	Pawłowski	46
32	Natalia	Walczak	47
33	Grzegorz	Sikora	48
34	Karolina	Kubiak	49
35	Artur	Czarnecki	50
36	Sandra	Bąk	51
37	Dariusz	Szewczyk	52
38	Iwona	Lis	53
39	Norbert	Milewski	54
40	Joanna	Kalinowska	55
41	Robert	Kurek	56
42	Emilia	Sadowska	57
43	Przemysław	Bednarek	58
44	Oliwia	Zalewska	59
45	Andrzej	Tomczak	60
46	Martyna	Borowska	61
47	Daniel	Chmielewski	62
48	Alicja	Urban	63
49	Filip	Sobczak	64
50	Laura	Makowska	65
\.


--
-- TOC entry 4926 (class 0 OID 25114)
-- Dependencies: 222
-- Data for Name: vehicle; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.vehicle (vehicle_id, licence_plate, customer_id) FROM stdin;
1	KR1A1111	1
2	KR2A1111	2
3	KR3A1111	3
4	KR4A1111	4
5	KR5A1111	5
6	KR1B2222	6
7	KR2B2222	7
8	KR3B2222	8
9	KR4B2222	9
10	KR5B2222	10
11	KR1C3333	11
12	KR2C3333	12
13	KR3C3333	13
14	KR4C3333	14
15	KR5C3333	15
16	KR1D4444	16
17	KR2D4444	\N
18	KR3D4444	17
19	KR4D4444	18
20	KR5D4444	19
21	KR1E5555	20
22	KR2E5555	21
23	KR3E5555	22
24	KR4E5555	\N
25	KR5E5555	23
\.


--
-- TOC entry 4928 (class 0 OID 25124)
-- Dependencies: 224
-- Data for Name: wallet; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wallet (wallet_id, balance_minor, currency_code, customer_id) FROM stdin;
1	120.00	PLN	1
2	85.50	PLN	2
3	200.00	PLN	3
4	45.00	PLN	4
5	150.00	PLN	5
6	60.00	PLN	6
7	95.00	PLN	7
8	180.00	PLN	8
9	30.00	PLN	9
10	210.00	PLN	10
11	75.00	PLN	11
12	160.00	PLN	12
13	90.00	PLN	13
14	55.00	PLN	14
15	130.00	PLN	15
16	170.00	PLN	16
17	40.00	PLN	17
18	220.00	PLN	18
19	65.00	PLN	19
20	140.00	PLN	20
21	100.00	PLN	21
22	80.00	PLN	22
23	155.00	PLN	23
24	35.00	PLN	24
25	190.00	PLN	25
26	70.00	PLN	26
27	165.00	PLN	27
28	50.00	PLN	28
29	125.00	PLN	29
30	205.00	PLN	30
31	95.00	PLN	31
32	60.00	PLN	32
33	175.00	PLN	33
34	85.00	PLN	34
35	145.00	PLN	35
36	110.00	PLN	36
37	55.00	PLN	37
38	185.00	PLN	38
39	90.00	PLN	39
40	160.00	PLN	40
41	70.00	PLN	41
42	200.00	PLN	42
43	45.00	PLN	43
44	135.00	PLN	44
45	95.00	PLN	45
46	155.00	PLN	46
47	80.00	PLN	47
48	170.00	PLN	48
49	60.00	PLN	49
50	210.00	PLN	50
\.


--
-- TOC entry 4937 (class 0 OID 0)
-- Dependencies: 219
-- Name: customer_customer_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.customer_customer_id_seq', 50, true);


--
-- TOC entry 4938 (class 0 OID 0)
-- Dependencies: 221
-- Name: vehicle_vehicle_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

-- Synchronizuj sekwencję z maksymalnym ID w tabeli
SELECT pg_catalog.setval('public.vehicle_vehicle_id_seq', COALESCE((SELECT MAX(vehicle_id) FROM vehicle), 0), true);

-- Migracja: upewnij się, że sekwencja jest zawsze zsynchronizowana (dla istniejących baz)
DO $$ 
BEGIN
    PERFORM pg_catalog.setval(
        'public.vehicle_vehicle_id_seq', 
        COALESCE((SELECT MAX(vehicle_id) FROM public.vehicle), 0), 
        true
    );
END $$;

--
-- TOC entry 4939 (class 0 OID 0)
-- Dependencies: 223
-- Name: wallet_wallet_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.wallet_wallet_id_seq', 50, true);


--
-- TOC entry 4769 (class 2606 OID 25112)
-- Name: customer customer_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (customer_id);


--
-- TOC entry 4771 (class 2606 OID 25122)
-- Name: vehicle vehicle_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vehicle
    ADD CONSTRAINT vehicle_pkey PRIMARY KEY (vehicle_id);


--
-- TOC entry 4773 (class 2606 OID 25133)
-- Name: wallet wallet_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallet
    ADD CONSTRAINT wallet_pkey PRIMARY KEY (wallet_id);


--
-- TOC entry 4774 (class 2606 OID 25198)
-- Name: vehicle fk_child_parent; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vehicle
    ADD CONSTRAINT fk_child_parent FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);


--
-- TOC entry 4775 (class 2606 OID 25203)
-- Name: wallet fk_wallet_customer; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallet
    ADD CONSTRAINT fk_wallet_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);


-- Completed on 2025-12-25 18:01:21

--
-- PostgreSQL database dump complete
--

\unrestrict tGpet6Bjorfkc60NIVi5r2sBAqxQRWh2Qll14J1HHjBidN4HBvAecOBszOdFkVF

