--
-- PostgreSQL database dump
--

\restrict 19ybi77AiecGkSeaL2MaaXqS6xABKhq9H6WjnPWQFxdC1GETrZNPoCw1v46s9JQ

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-12-25 18:02:31

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
-- TOC entry 4970 (class 0 OID 0)
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
-- TOC entry 4971 (class 0 OID 0)
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
-- TOC entry 4972 (class 0 OID 0)
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
-- TOC entry 4973 (class 0 OID 0)
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
-- TOC entry 4974 (class 0 OID 0)
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
-- TOC entry 4956 (class 0 OID 24940)
-- Dependencies: 220
-- Data for Name: parking_location; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.parking_location (parking_id, name_parking, address_line, ref_company_id) FROM stdin;
1	SmartParking – Bióro	ul. Krajowa 12, 30-001 Kraków	1
2	SmartParking – Galeria	ul. Handlowa 5, 30-002 Kraków	1
3	SmartParking – Dworzec	ul. Kolejowa 3, 30-003 Kraków	1
4	SmartParking – Lotnisko	ul. Lotnicza 1, 32-083 Balice	1
5	SmartParking – Strefa Biznesu	ul. Przemysłowa 20, 30-004 Kraków	1
\.


--
-- TOC entry 4962 (class 0 OID 25001)
-- Dependencies: 226
-- Data for Name: parking_pricing; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.parking_pricing (pricing_id, curency_code, rate_per_min, free_minutes, rounding_step_min, reservation_fee_minor, parking_id) FROM stdin;
1	PLN	10	15	5	1000	1
2	PLN	11	15	5	1000	2
3	PLN	10	10	5	1500	3
4	PLN	12	10	5	1500	4
5	PLN	13	10	5	900	5
\.


--
-- TOC entry 4960 (class 0 OID 24976)
-- Dependencies: 224
-- Data for Name: parking_session; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.parking_session (session_id, entry_time, exit_time, price_total_minor, payment_status, parking_id, spot_id, ref_vehicle_id, ref_account_id) FROM stdin;
2	2025-12-20 08:02:15	2025-12-20 10:06:05	12.10	Paid	2	11	2	17
3	2025-12-20 08:03:50	2025-12-20 10:03:40	11.00	Paid	3	21	3	18
4	2025-12-20 08:05:05	2025-12-20 10:12:10	14.40	Paid	4	31	4	19
5	2025-12-20 08:06:42	2025-12-20 10:09:22	14.95	Paid	5	41	5	20
6	2025-12-20 08:08:10	2025-12-20 10:18:55	12.00	Paid	1	2	6	21
7	2025-12-20 08:09:55	2025-12-20 10:29:40	13.00	Paid	3	22	8	23
8	2025-12-20 08:11:20	2025-12-20 10:22:30	13.20	Paid	2	12	7	22
11	2025-12-20 08:15:33	\N	\N	Session	2	13	12	27
12	2025-12-20 08:17:02	\N	\N	Session	1	3	11	26
13	2025-12-20 08:18:40	\N	\N	Session	5	43	15	30
14	2025-12-20 08:20:11	\N	\N	Session	3	23	13	28
15	2025-12-20 08:21:57	\N	\N	Session	4	33	14	29
16	2025-12-20 08:23:25	\N	\N	Session	1	5	16	31
17	2025-12-20 08:24:50	\N	\N	Session	2	14	17	65
18	2025-12-20 08:26:18	\N	\N	Session	4	35	19	33
19	2025-12-20 08:27:44	\N	\N	Session	3	24	18	32
20	2025-12-20 08:29:10	\N	\N	Session	5	44	20	34
21	2025-12-20 08:30:55	\N	\N	Session	4	36	24	65
22	2025-12-20 08:32:21	\N	\N	Session	1	6	21	35
23	2025-12-20 08:33:49	\N	\N	Session	3	25	23	37
24	2025-12-20 08:35:10	\N	\N	Session	5	45	25	38
25	2025-12-20 08:36:40	\N	\N	Session	2	15	22	36
1	2025-12-20 08:00:40	2025-12-20 10:01:15	11.00	Paid	1	4	1	16
9	2025-12-20 08:12:48	2025-12-20 10:33:15	17.55	Unpaid	5	42	10	25
10	2025-12-20 08:14:05	2025-12-20 10:26:05	15.00	Unpaid	4	32	9	24
\.


--
-- TOC entry 4958 (class 0 OID 24956)
-- Dependencies: 222
-- Data for Name: parking_spot; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.parking_spot (spot_id, code, floor_lvl, to_reserved, type, id_parking) FROM stdin;
1	A1	0	f	Available	1
2	A2	0	f	Available	1
3	A3	0	f	Available	1
4	A4	0	t	Available	1
5	B1	0	f	Available	1
6	B2	0	f	Available	1
7	B3	0	f	Available	1
8	C1	1	f	Available	1
9	C2	1	f	Available	1
10	C3	1	f	Available	1
11	A1	0	f	Available	2
12	A2	0	f	Available	2
13	A3	0	f	Available	2
14	B1	0	f	Available	2
15	B2	0	f	Available	2
16	B3	0	f	Available	2
17	B4	0	t	Available	2
18	C1	1	f	Available	2
19	C2	1	f	Available	2
20	C3	1	f	Available	2
21	A1	0	f	Available	3
22	A2	0	f	Available	3
23	A3	0	f	Available	3
24	B1	0	f	Available	3
25	B2	0	f	Available	3
26	B3	0	f	Available	3
27	C1	0	f	Available	3
28	C2	0	f	Available	3
29	C3	0	f	Available	3
30	C4	0	f	Available	3
31	A1	0	f	Available	4
32	A2	0	f	Available	4
33	A3	0	f	Available	4
34	A4	0	f	Available	4
35	B1	0	f	Available	4
36	B2	0	f	Available	4
37	B3	0	f	Available	4
38	C1	0	f	Available	4
39	C2	0	f	Available	4
40	C3	0	f	Available	4
41	A1	0	f	Available	5
42	A2	0	f	Available	5
43	A3	0	f	Available	5
44	B1	0	f	Available	5
45	B2	0	f	Available	5
46	B3	0	f	Available	5
47	B4	0	t	Available	5
48	C1	1	f	Available	5
49	C2	1	f	Available	5
50	C3	1	f	Available	5
\.


--
-- TOC entry 4964 (class 0 OID 25028)
-- Dependencies: 228
-- Data for Name: reservation_spot; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.reservation_spot (reservation_id, valid_until, status_reservation, spot_id, parking_id, ref_account_id) FROM stdin;
2	2025-12-20 12:00:00	Expired	4	1	21
3	2025-12-20 14:00:00	Cancelled	47	5	22
4	2025-12-26 18:00:00	Paid	4	1	16
1	2025-12-20 07:00:00	Used	4	1	16
\.


--
-- TOC entry 4975 (class 0 OID 0)
-- Dependencies: 219
-- Name: parking_location_id_parking_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.parking_location_id_parking_seq', 5, true);


--
-- TOC entry 4976 (class 0 OID 0)
-- Dependencies: 225
-- Name: parking_pricing_pricing_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.parking_pricing_pricing_id_seq', 5, true);


--
-- TOC entry 4977 (class 0 OID 0)
-- Dependencies: 223
-- Name: parking_session_session_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.parking_session_session_id_seq', 25, true);


--
-- TOC entry 4978 (class 0 OID 0)
-- Dependencies: 221
-- Name: parking_spot_id_spot_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.parking_spot_id_spot_seq', 50, true);


--
-- TOC entry 4979 (class 0 OID 0)
-- Dependencies: 227
-- Name: reservation_spot_reservation_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.reservation_spot_reservation_id_seq', 4, true);


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


-- Completed on 2025-12-25 18:02:33

--
-- PostgreSQL database dump complete
--

\unrestrict 19ybi77AiecGkSeaL2MaaXqS6xABKhq9H6WjnPWQFxdC1GETrZNPoCw1v46s9JQ

