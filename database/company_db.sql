--
-- PostgreSQL database dump
--

\restrict 6N1DEEO3dyt0BZ9lZRXm6ObrxGMFNNjeE4uOmZhIcZZJViyqoJKsrym79wgiZrn

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-11-20 20:56:22

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
-- TOC entry 220 (class 1259 OID 24928)
-- Name: company; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.company (
    company_id integer CONSTRAINT company_id_company_not_null NOT NULL,
    name_company character varying(30) NOT NULL,
    address character varying(100) NOT NULL,
    tax_id character varying(30) NOT NULL
);


ALTER TABLE public.company OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 24927)
-- Name: company_id_company_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.company_id_company_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.company_id_company_seq OWNER TO postgres;

--
-- TOC entry 4910 (class 0 OID 0)
-- Dependencies: 219
-- Name: company_id_company_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.company_id_company_seq OWNED BY public.company.company_id;


--
-- TOC entry 4755 (class 2604 OID 24931)
-- Name: company company_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company ALTER COLUMN company_id SET DEFAULT nextval('public.company_id_company_seq'::regclass);


--
-- TOC entry 4757 (class 2606 OID 24937)
-- Name: company company_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_pkey PRIMARY KEY (company_id);


-- Completed on 2025-11-20 20:56:24

--
-- PostgreSQL database dump complete
--

\unrestrict 6N1DEEO3dyt0BZ9lZRXm6ObrxGMFNNjeE4uOmZhIcZZJViyqoJKsrym79wgiZrn

