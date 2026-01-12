--
-- PostgreSQL database dump
--

\restrict xGAIqDBI4thJl5Blk6nQBPBxflN3NbAYvqNKimAhImpUGwGx9j9ZDLxTFyKqYhg

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-12-25 17:59:15

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
-- TOC entry 4929 (class 0 OID 0)
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
-- TOC entry 4930 (class 0 OID 0)
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
-- TOC entry 4921 (class 0 OID 25060)
-- Dependencies: 220
-- Data for Name: account; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.account (account_id, email, password_hash, role_account, is_active) FROM stdin;
1	admin1@smartparking.pl	\N	Admin	t
2	admin2@smartparking.pl	\N	Admin	t
3	admin3@smartparking.pl	\N	Admin	t
4	admin4@smartparking.pl	\N	Admin	t
5	admin5@smartparking.pl	\N	Admin	t
6	worker1@smartparking.pl	\N	Worker	t
7	worker2@smartparking.pl	\N	Worker	t
8	worker3@smartparking.pl	\N	Worker	t
9	worker4@smartparking.pl	\N	Worker	t
10	worker5@smartparking.pl	\N	Worker	t
11	worker6@smartparking.pl	\N	Worker	t
12	worker7@smartparking.pl	\N	Worker	t
13	worker8@smartparking.pl	\N	Worker	t
14	worker9@smartparking.pl	\N	Worker	t
15	worker10@smartparking.pl	\N	Worker	t
16	user01@smartparking.pl	\N	User	t
17	user02@smartparking.pl	\N	User	t
18	user03@smartparking.pl	\N	User	t
19	user04@smartparking.pl	\N	User	t
20	user05@smartparking.pl	\N	User	t
21	user06@smartparking.pl	\N	User	t
22	user07@smartparking.pl	\N	User	t
23	user08@smartparking.pl	\N	User	t
24	user09@smartparking.pl	\N	User	t
25	user10@smartparking.pl	\N	User	t
26	user11@smartparking.pl	\N	User	t
27	user12@smartparking.pl	\N	User	t
28	user13@smartparking.pl	\N	User	t
29	user14@smartparking.pl	\N	User	t
30	user15@smartparking.pl	\N	User	t
31	user16@smartparking.pl	\N	User	t
32	user17@smartparking.pl	\N	User	t
33	user18@smartparking.pl	\N	User	t
34	user19@smartparking.pl	\N	User	t
35	user20@smartparking.pl	\N	User	t
36	user21@smartparking.pl	\N	User	t
37	user22@smartparking.pl	\N	User	t
38	user23@smartparking.pl	\N	User	t
39	user24@smartparking.pl	\N	User	t
40	user25@smartparking.pl	\N	User	t
41	user26@smartparking.pl	\N	User	t
42	user27@smartparking.pl	\N	User	t
43	user28@smartparking.pl	\N	User	t
44	user29@smartparking.pl	\N	User	t
45	user30@smartparking.pl	\N	User	t
46	user31@smartparking.pl	\N	User	t
47	user32@smartparking.pl	\N	User	t
48	user33@smartparking.pl	\N	User	t
49	user34@smartparking.pl	\N	User	t
50	user35@smartparking.pl	\N	User	t
51	user36@smartparking.pl	\N	User	t
52	user37@smartparking.pl	\N	User	t
53	user38@smartparking.pl	\N	User	t
54	user39@smartparking.pl	\N	User	t
55	user40@smartparking.pl	\N	User	t
56	user41@smartparking.pl	\N	User	t
57	user42@smartparking.pl	\N	User	t
58	user43@smartparking.pl	\N	User	t
59	user44@smartparking.pl	\N	User	t
60	user45@smartparking.pl	\N	User	t
61	user46@smartparking.pl	\N	User	t
62	user47@smartparking.pl	\N	User	t
63	user48@smartparking.pl	\N	User	t
64	user49@smartparking.pl	\N	User	t
65	user50@smartparking.pl	\N	User	t
\.


--
-- TOC entry 4923 (class 0 OID 25074)
-- Dependencies: 222
-- Data for Name: login_code; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.login_code (code_id, code, account_id, is_used) FROM stdin;
1	LC-1-1b831b	1	t
2	LC-2-6b8667	2	t
3	LC-3-1ecea4	3	t
4	LC-4-5dd74a	4	t
5	LC-5-57cb9d	5	t
6	LC-6-9da1be	6	t
7	LC-7-c199d0	7	t
8	LC-8-68c07d	8	t
9	LC-9-21fb92	9	t
10	LC-10-72a520	10	t
11	LC-11-b46e46	11	t
12	LC-12-7b28cc	12	t
13	LC-13-7cd6d5	13	t
14	LC-14-eeafb4	14	f
15	LC-15-d62fd1	15	f
\.


--
-- TOC entry 4931 (class 0 OID 0)
-- Dependencies: 219
-- Name: account_account_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

-- Synchronizuj sekwencję z maksymalnym ID w tabeli
SELECT pg_catalog.setval('public.account_account_id_seq', COALESCE((SELECT MAX(account_id) FROM account), 0), true);

--
-- TOC entry 4932 (class 0 OID 0)
-- Dependencies: 221
-- Name: login_code_code_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.login_code_code_id_seq', 15, true);


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


-- Completed on 2025-12-25 17:59:17

--
-- PostgreSQL database dump complete
--

\unrestrict xGAIqDBI4thJl5Blk6nQBPBxflN3NbAYvqNKimAhImpUGwGx9j9ZDLxTFyKqYhg

