--
-- PostgreSQL database dump
--

\restrict dGD6nT1ltUdeE00SjF8MXiEmV5dccJl1iF5o5edKQF3hEnEvPVIizIgAtqe5og5

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-11-20 20:57:26

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
-- TOC entry 4919 (class 0 OID 0)
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
-- TOC entry 4920 (class 0 OID 0)
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


-- Completed on 2025-11-20 20:57:28

--
-- PostgreSQL database dump complete
--

\unrestrict dGD6nT1ltUdeE00SjF8MXiEmV5dccJl1iF5o5edKQF3hEnEvPVIizIgAtqe5og5

