--
-- PostgreSQL database dump
--

\restrict 9NSykAsXDpse4SVqIyy277gnRDsWuFQimYvd0blPZwG1jkDSDKiwfEtquVl4qz2

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-11-20 20:54:57

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
-- TOC entry 855 (class 1247 OID 25053)
-- Name: role_account; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.role_account AS ENUM (
    'User',
    'Worker',
    'Admin'
);


ALTER TYPE public.role_account OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 220 (class 1259 OID 25060)
-- Name: account; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.account (
    account_id integer NOT NULL,
    email character varying(100) NOT NULL,
    password_hash character varying(250),
    role_account public.role_account DEFAULT 'User'::public.role_account NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);


ALTER TABLE public.account OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 25059)
-- Name: account_account_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.account_account_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.account_account_id_seq OWNER TO postgres;

--
-- TOC entry 4925 (class 0 OID 0)
-- Dependencies: 219
-- Name: account_account_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.account_account_id_seq OWNED BY public.account.account_id;


--
-- TOC entry 222 (class 1259 OID 25074)
-- Name: login_code; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.login_code (
    code_id integer NOT NULL,
    code character varying(15) NOT NULL,
    account_id integer,
    is_used boolean DEFAULT false NOT NULL
);


ALTER TABLE public.login_code OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 25073)
-- Name: login_code_code_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.login_code_code_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.login_code_code_id_seq OWNER TO postgres;

--
-- TOC entry 4926 (class 0 OID 0)
-- Dependencies: 221
-- Name: login_code_code_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.login_code_code_id_seq OWNED BY public.login_code.code_id;


--
-- TOC entry 4763 (class 2604 OID 25063)
-- Name: account account_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account ALTER COLUMN account_id SET DEFAULT nextval('public.account_account_id_seq'::regclass);


--
-- TOC entry 4766 (class 2604 OID 25077)
-- Name: login_code code_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.login_code ALTER COLUMN code_id SET DEFAULT nextval('public.login_code_code_id_seq'::regclass);


--
-- TOC entry 4769 (class 2606 OID 25072)
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (account_id);


--
-- TOC entry 4771 (class 2606 OID 25083)
-- Name: login_code login_code_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.login_code
    ADD CONSTRAINT login_code_pkey PRIMARY KEY (code_id);


--
-- TOC entry 4772 (class 2606 OID 25084)
-- Name: login_code login_code_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.login_code
    ADD CONSTRAINT login_code_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(account_id);

--
-- TOC entry (class 1259 OID)
-- Name: verification_token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.verification_token (
    token_id integer NOT NULL,
    token character varying(64) NOT NULL,
    account_id integer NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    is_used boolean DEFAULT false NOT NULL
);

ALTER TABLE public.verification_token OWNER TO postgres;

--
-- TOC entry (class 1259 OID)
-- Name: verification_token_token_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.verification_token_token_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.verification_token_token_id_seq OWNER TO postgres;

--
-- TOC entry
-- Name: verification_token_token_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.verification_token_token_id_seq OWNED BY public.verification_token.token_id;

--
-- TOC entry (class 2604 OID)
-- Name: verification_token token_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.verification_token ALTER COLUMN token_id SET DEFAULT nextval('public.verification_token_token_id_seq'::regclass);

--
-- TOC entry (class 2606 OID)
-- Name: verification_token verification_token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.verification_token
    ADD CONSTRAINT verification_token_pkey PRIMARY KEY (token_id);

--
-- TOC entry (class 2606 OID)
-- Name: verification_token verification_token_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.verification_token
    ADD CONSTRAINT verification_token_token_key UNIQUE (token);

--
-- TOC entry (class 2606 OID)
-- Name: verification_token verification_token_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.verification_token
    ADD CONSTRAINT verification_token_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(account_id);

--
-- TOC entry
-- Name: idx_verification_token_token; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_verification_token_token ON public.verification_token(token);

--
-- TOC entry
-- Name: idx_verification_token_account_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_verification_token_account_id ON public.verification_token(account_id);


-- Completed on 2025-11-20 20:54:59

--
-- PostgreSQL database dump complete
--

\unrestrict 9NSykAsXDpse4SVqIyy277gnRDsWuFQimYvd0blPZwG1jkDSDKiwfEtquVl4qz2

