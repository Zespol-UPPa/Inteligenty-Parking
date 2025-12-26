--
-- PostgreSQL database dump
--

\restrict zvDPnxbVuColFT4OnB2Mr3BeIzMnQxIGDTmTKGQ33wU4XME40T4beVP05C5mptE

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-12-25 18:01:56

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
-- TOC entry 220 (class 1259 OID 16737)
-- Name: camera; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.camera (
    camera_id integer NOT NULL,
    camera_code character varying(10) NOT NULL,
    ref_parking_id integer NOT NULL
);


ALTER TABLE public.camera OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 16736)
-- Name: camera_camera_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.camera_camera_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.camera_camera_id_seq OWNER TO postgres;

--
-- TOC entry 4923 (class 0 OID 0)
-- Dependencies: 219
-- Name: camera_camera_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.camera_camera_id_seq OWNED BY public.camera.camera_id;


--
-- TOC entry 222 (class 1259 OID 16747)
-- Name: plate_read; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.plate_read (
    read_id integer CONSTRAINT plate_read_id_read_not_null NOT NULL,
    camera_id integer NOT NULL,
    raw_plate character varying(11) NOT NULL,
    event_time timestamp without time zone NOT NULL
);


ALTER TABLE public.plate_read OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 16746)
-- Name: plate_read_id_read_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.plate_read_id_read_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.plate_read_id_read_seq OWNER TO postgres;

--
-- TOC entry 4924 (class 0 OID 0)
-- Dependencies: 221
-- Name: plate_read_id_read_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.plate_read_id_read_seq OWNED BY public.plate_read.read_id;


--
-- TOC entry 4760 (class 2604 OID 16740)
-- Name: camera camera_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.camera ALTER COLUMN camera_id SET DEFAULT nextval('public.camera_camera_id_seq'::regclass);


--
-- TOC entry 4761 (class 2604 OID 16750)
-- Name: plate_read read_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plate_read ALTER COLUMN read_id SET DEFAULT nextval('public.plate_read_id_read_seq'::regclass);


--
-- TOC entry 4915 (class 0 OID 16737)
-- Dependencies: 220
-- Data for Name: camera; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.camera (camera_id, camera_code, ref_parking_id) FROM stdin;
1	P1-IN	1
2	P1-OUT	1
3	P2-IN	2
4	P2-OUT	2
5	P3-IN	3
6	P3-OUT	3
7	P4-IN	4
8	P4-OUT	4
9	P5-IN	5
10	P5-OUT	5
\.


--
-- TOC entry 4917 (class 0 OID 16747)
-- Dependencies: 222
-- Data for Name: plate_read; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.plate_read (read_id, camera_id, raw_plate, event_time) FROM stdin;
1	1	KR1A1111	2025-12-20 08:00:40
2	3	KR2A1111	2025-12-20 08:02:15
3	5	KR3A1111	2025-12-20 08:03:50
4	7	KR4A1111	2025-12-20 08:05:05
5	9	KR5A1111	2025-12-20 08:06:42
6	1	KR1B2222	2025-12-20 08:08:10
7	5	KR3B2222	2025-12-20 08:09:55
8	3	KR2B2222	2025-12-20 08:11:20
9	9	KR5B2222	2025-12-20 08:12:48
10	7	KR4B2222	2025-12-20 08:14:05
11	3	KR2C3333	2025-12-20 08:15:33
12	1	KR1C3333	2025-12-20 08:17:02
13	9	KR5C3333	2025-12-20 08:18:40
14	5	KR3C3333	2025-12-20 08:20:11
15	7	KR4C3333	2025-12-20 08:21:57
16	1	KR1D4444	2025-12-20 08:23:25
17	3	KR2D4444	2025-12-20 08:24:50
18	7	KR4D4444	2025-12-20 08:26:18
19	5	KR3D4444	2025-12-20 08:27:44
20	9	KR5D4444	2025-12-20 08:29:10
21	7	KR4E5555	2025-12-20 08:30:55
22	1	KR1E5555	2025-12-20 08:32:21
23	5	KR3E5555	2025-12-20 08:33:49
24	9	KR5E5555	2025-12-20 08:35:10
25	3	KR2E5555	2025-12-20 08:36:40
26	2	KR1A1111	2025-12-20 10:01:15
27	6	KR3A1111	2025-12-20 10:03:40
28	4	KR2A1111	2025-12-20 10:06:05
29	10	KR5A1111	2025-12-20 10:09:22
30	8	KR4A1111	2025-12-20 10:12:10
31	2	KR1B2222	2025-12-20 10:18:55
32	4	KR2B2222	2025-12-20 10:22:30
33	8	KR4B2222	2025-12-20 10:26:05
34	6	KR3B2222	2025-12-20 10:29:40
35	10	KR5B2222	2025-12-20 10:33:15
\.


--
-- TOC entry 4925 (class 0 OID 0)
-- Dependencies: 219
-- Name: camera_camera_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.camera_camera_id_seq', 10, true);


--
-- TOC entry 4926 (class 0 OID 0)
-- Dependencies: 221
-- Name: plate_read_id_read_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.plate_read_id_read_seq', 35, true);


--
-- TOC entry 4763 (class 2606 OID 16745)
-- Name: camera camera_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.camera
    ADD CONSTRAINT camera_pkey PRIMARY KEY (camera_id);


--
-- TOC entry 4765 (class 2606 OID 16756)
-- Name: plate_read plate_read_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plate_read
    ADD CONSTRAINT plate_read_pkey PRIMARY KEY (read_id);


--
-- TOC entry 4766 (class 2606 OID 16757)
-- Name: plate_read plate_read_camera_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plate_read
    ADD CONSTRAINT plate_read_camera_id_fkey FOREIGN KEY (camera_id) REFERENCES public.camera(camera_id);


-- Completed on 2025-12-25 18:01:58

--
-- PostgreSQL database dump complete
--

\unrestrict zvDPnxbVuColFT4OnB2Mr3BeIzMnQxIGDTmTKGQ33wU4XME40T4beVP05C5mptE

