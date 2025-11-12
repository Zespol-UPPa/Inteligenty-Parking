--
-- PostgreSQL database dump
--

\restrict OjSOeAvHdvaBDe78aT83JMTR1RsELgQGIC3VkYNpRlNp3Qsh2e1TAxJx7RU3b4n

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-11-12 12:09:17

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
-- TOC entry 926 (class 1247 OID 16646)
-- Name: status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.status AS ENUM (
    'Available',
    'Booked',
    'N/A'
);


ALTER TYPE public.status OWNER TO postgres;

--
-- TOC entry 917 (class 1247 OID 16564)
-- Name: status_paid; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.status_paid AS ENUM (
    'Pending',
    'Paid',
    'Failed',
    'Cancelled',
    'Session'
);


ALTER TYPE public.status_paid OWNER TO postgres;

--
-- TOC entry 929 (class 1247 OID 16659)
-- Name: status_reservation; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.status_reservation AS ENUM (
    'Reserved',
    'Canceled',
    'Paid'
);


ALTER TYPE public.status_reservation OWNER TO postgres;

--
-- TOC entry 248 (class 1255 OID 16684)
-- Name: fn_has_free_spot(integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_has_free_spot(p_camera_id integer) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE 
    v_exists boolean;
    v_parking_id integer;
BEGIN
    SELECT id_parking INTO v_parking_id
    FROM plate_read
    WHERE camera_id = p_camera_id
    ORDER BY event_time DESC
    LIMIT 1;

    IF v_parking_id IS NULL THEN
        RAISE EXCEPTION 'Nie znaleziono parkingu dla kamery %', p_camera_id;
    END IF;

    SELECT EXISTS(
        SELECT 1
        FROM v_parking_spot_status
        WHERE id_parking = v_parking_id
          AND is_free = true
          AND to_reserved = false
    ) INTO v_exists;

    RETURN v_exists;
END;
$$;


ALTER FUNCTION public.fn_has_free_spot(p_camera_id integer) OWNER TO postgres;

--
-- TOC entry 247 (class 1255 OID 16683)
-- Name: fn_has_free_spot(integer, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_has_free_spot(p_camera_id integer, p_parking_id integer) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
Declare v_exists boolean;
Begin
	Select EXISTS(
		Select 1
		From v_parking_spot_status
		Join plate_read On camera_id =p_camera_id
		Where id_parking = p_parking_id And is_free = true And to_reserved = false
	) INTO v_exists;
	return v_exists;
END;
$$;


ALTER FUNCTION public.fn_has_free_spot(p_camera_id integer, p_parking_id integer) OWNER TO postgres;

--
-- TOC entry 249 (class 1255 OID 16708)
-- Name: fn_has_unpaid(character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_has_unpaid(p_licence_plate character varying) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_account_id integer;
    v_exists     boolean;
BEGIN
    SELECT v.id_account
    INTO v_account_id
    FROM vehicle v
    WHERE v.licence_plate = p_licence_plate
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    IF v_account_id IS NULL THEN
        RETURN FALSE;
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM v_unpaid_sessions vus
        WHERE vus.id_account_effective = v_account_id
    )
    INTO v_exists;
    RETURN v_exists;
END;
$$;


ALTER FUNCTION public.fn_has_unpaid(p_licence_plate character varying) OWNER TO postgres;

--
-- TOC entry 263 (class 1255 OID 16713)
-- Name: fn_pick_random_free_spot(integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_pick_random_free_spot(p_parking_id integer) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_id_spot integer;
BEGIN
    SELECT id_spot
    INTO v_id_spot
    FROM public.v_parking_spot_status
    WHERE id_parking = p_parking_id
      AND is_free = true
      AND to_reserved = false
    ORDER BY random()
    LIMIT 1;

    IF v_id_spot IS NULL THEN
        RAISE EXCEPTION 'Brak wolnych miejsc na parkingu %', p_parking_id;
    END IF;

    RETURN v_id_spot;
END;
$$;


ALTER FUNCTION public.fn_pick_random_free_spot(p_parking_id integer) OWNER TO postgres;

--
-- TOC entry 262 (class 1255 OID 16734)
-- Name: pr_add_vehicle(integer, character varying); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.pr_add_vehicle(IN p_id_account integer, IN p_licence_plate character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_plate_norm        varchar(15);
    v_id_vehicle        integer;
    v_existing_account  integer;
BEGIN
    v_plate_norm := upper(trim(p_licence_plate));

    SELECT id_vehicle, id_account
    INTO v_id_vehicle, v_existing_account
    FROM public.vehicle
    WHERE licence_plate = v_plate_norm;

    IF v_id_vehicle IS NULL THEN
        INSERT INTO public.vehicle(licence_plate, id_account)
        VALUES (v_plate_norm, p_id_account);

    ELSIF v_existing_account IS NULL THEN
        UPDATE public.vehicle
        SET id_account = p_id_account
        WHERE id_vehicle = v_id_vehicle;

    ELSIF v_existing_account = p_id_account THEN
        RAISE NOTICE 'Pojazd z tablicą % jest już przypisany do konta %', v_plate_norm, p_id_account;

    ELSE
        RAISE EXCEPTION 'Pojazd z tablicą % jest już przypisany do innego konta (id_account = %)',
            v_plate_norm, v_existing_account;
    END IF;
END;
$$;


ALTER PROCEDURE public.pr_add_vehicle(IN p_id_account integer, IN p_licence_plate character varying) OWNER TO postgres;

--
-- TOC entry 260 (class 1255 OID 16733)
-- Name: pr_register_account(character varying, character varying, character varying, character varying, character varying); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.pr_register_account(IN p_email character varying, IN p_password_hash character varying, IN p_first_name character varying, IN p_last_name character varying, IN p_phone_number character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_id_account integer;
BEGIN
    INSERT INTO public.account(email, password_hash)
    VALUES (p_email, p_password_hash)
    RETURNING id_account INTO v_id_account;

    INSERT INTO public.customer(first_name, last_name, phone_number, id_account)
    VALUES (p_first_name, p_last_name, p_phone_number, v_id_account);

    INSERT INTO public.wallet(balance_minor, currency_code, id_account)
    VALUES (0.00, 'PLN', v_id_account);
END;
$$;


ALTER PROCEDURE public.pr_register_account(IN p_email character varying, IN p_password_hash character varying, IN p_first_name character varying, IN p_last_name character varying, IN p_phone_number character varying) OWNER TO postgres;

--
-- TOC entry 264 (class 1255 OID 16712)
-- Name: pr_register_entry_by_camera(character varying, integer, character varying, timestamp without time zone); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.pr_register_entry_by_camera(IN p_name_parking character varying, IN p_camera_id integer, IN p_licence_plate character varying, IN p_event_time timestamp without time zone DEFAULT now())
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_parking_id  integer;
    v_vehicle_id  integer;
    v_plate_norm  varchar(15);
	v_id_spot     integer;
BEGIN
    v_plate_norm := upper(trim(p_licence_plate));

    SELECT pl.id_parking
    INTO v_parking_id
    FROM public.parking_location pl
    WHERE pl.name_parking = p_name_parking
    LIMIT 1;

	v_id_spot := public.fn_pick_random_free_spot(v_parking_id);
	
    INSERT INTO public.plate_read (id_parking, camera_id, licence_plate, event_time)
    VALUES (v_parking_id, p_camera_id, v_plate_norm, p_event_time);

    SELECT v.id_vehicle
    INTO v_vehicle_id
    FROM public.vehicle v
    WHERE v.licence_plate = v_plate_norm;

    IF NOT FOUND THEN
        BEGIN
            INSERT INTO public.vehicle (licence_plate)
            VALUES (v_plate_norm)
            RETURNING id_vehicle INTO v_vehicle_id;
        EXCEPTION
            WHEN unique_violation THEN
                SELECT v.id_vehicle
                INTO v_vehicle_id
                FROM public.vehicle v
                WHERE v.licence_plate = v_plate_norm;
        END;
    END IF;

    INSERT INTO public.parking_session (entry_time, id_parking, id_vehicle, id_spot)
    VALUES (p_event_time, v_parking_id, v_vehicle_id, v_id_spot);

END;
$$;


ALTER PROCEDURE public.pr_register_entry_by_camera(IN p_name_parking character varying, IN p_camera_id integer, IN p_licence_plate character varying, IN p_event_time timestamp without time zone) OWNER TO postgres;

--
-- TOC entry 265 (class 1255 OID 16715)
-- Name: pr_register_exit_by_camera(character varying, integer, character varying, timestamp with time zone); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.pr_register_exit_by_camera(IN p_name_parking character varying, IN p_camera_id integer, IN p_licence_plate character varying, IN p_event_time timestamp with time zone DEFAULT now())
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_parking_id        integer;
    v_vehicle_id        integer;
    v_account_id        integer;
    v_session_id        integer;
    v_entry_time        timestamp;
    v_minutes_total     integer;
    v_minutes_billable  integer;
    v_rate_per_min      integer;
    v_free_minutes      integer;
    v_round_step        integer;
    v_price             numeric(10,2);
    v_plate_norm        varchar(15);
BEGIN
    -- 1. Normalizacja tablicy
    v_plate_norm := upper(trim(p_licence_plate));

    -- 2. Szukamy parkingu po nazwie
    SELECT id_parking
      INTO v_parking_id
      FROM public.parking_location
      WHERE name_parking = p_name_parking
      LIMIT 1;

    IF v_parking_id IS NULL THEN
        RAISE EXCEPTION 'Nie znaleziono parkingu o nazwie %', p_name_parking;
    END IF;

    -- 3. Szukamy pojazdu + konta
    SELECT id_vehicle, id_account
      INTO v_vehicle_id, v_account_id
      FROM public.vehicle
      WHERE licence_plate = v_plate_norm
      LIMIT 1;

    IF v_vehicle_id IS NULL THEN
        RAISE EXCEPTION 'Nie znaleziono pojazdu o tablicy %', v_plate_norm;
    END IF;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'Pojazd % nie ma przypisanego konta - wyjazd zablokowany', v_plate_norm;
    END IF;

    -- 5. Szukamy aktywnej sesji (exit_time IS NULL)
    SELECT id_session, entry_time
      INTO v_session_id, v_entry_time
      FROM public.parking_session
      WHERE id_parking = v_parking_id
        AND id_vehicle = v_vehicle_id
        AND exit_time IS NULL
      ORDER BY entry_time DESC
      LIMIT 1;

    IF v_session_id IS NULL THEN
        RAISE EXCEPTION 'Brak aktywnej sesji dla pojazdu % na parkingu %', v_plate_norm, p_name_parking;
    END IF;

    -- 6. Liczymy czas postoju w minutach (zaokrąglenie w górę)
    SELECT Floor(EXTRACT(EPOCH FROM (p_event_time - v_entry_time)) / 60.0)
      INTO v_minutes_total;

    IF v_minutes_total < 0 THEN
        RAISE EXCEPTION 'Czas wyjazdu % jest wcześniejszy niż czas wjazdu %', p_event_time, v_entry_time;
    END IF;

    -- 7. Pobieramy cennik dla parkingu
    SELECT rate_per_min, free_minutes, rounding_step_min
      INTO v_rate_per_min, v_free_minutes, v_round_step
      FROM public.pricing
      WHERE id_parking = v_parking_id
      ORDER BY id_pricing DESC
      LIMIT 1;

    IF v_rate_per_min IS NULL THEN
        RAISE EXCEPTION 'Brak cennika dla parkingu % (id=%)', p_name_parking, v_parking_id;
    END IF;

	RAISE NOTICE 'Parking: %, Vehicle: %, Rate: %',
    v_parking_id, v_vehicle_id, v_rate_per_min;
    -- 8. Wyliczamy minuty płatne
    IF v_minutes_total <= v_free_minutes THEN
        v_minutes_billable := 0;
    ELSE
        v_minutes_billable := v_minutes_total - v_free_minutes;

        -- zaokrąglenie w górę do skoku rounding_step_min
        v_minutes_billable :=
            CEIL(v_minutes_billable::numeric / v_round_step) * v_round_step;
		RAISE NOTICE 'Minuty: %, Free Minuty: % , Finalne Minuty: %',
    		v_minutes_total, v_free_minutes, v_minutes_billable;
    END IF;

    -- 9. Liczymy cenę
    -- Zakładam, że rate_per_min to kwota w groszach,
    -- a price_total_minor jest w zł z dwoma miejscami po przecinku.
    v_price := (v_minutes_billable * v_rate_per_min) / 100.0;

	RAISE NOTICE 'Kwota: %',
    v_price;

    -- 10. Aktualizujemy sesję
    UPDATE public.parking_session
       SET exit_time        = p_event_time,
           price_total_minor = v_price,
           payment_status    = 'Pending',
           id_account        = COALESCE(id_account, v_account_id)
     WHERE id_session = v_session_id;

    -- Tu możesz ewentualnie dopisać logikę wirtualnej płatności / portfela

END;
$$;


ALTER PROCEDURE public.pr_register_exit_by_camera(IN p_name_parking character varying, IN p_camera_id integer, IN p_licence_plate character varying, IN p_event_time timestamp with time zone) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 224 (class 1259 OID 16410)
-- Name: account; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.account (
    id_account integer NOT NULL,
    email character varying(100) NOT NULL,
    password_hash character varying(250) NOT NULL
);


ALTER TABLE public.account OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 16409)
-- Name: account_id_account_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.account_id_account_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.account_id_account_seq OWNER TO postgres;

--
-- TOC entry 5085 (class 0 OID 0)
-- Dependencies: 223
-- Name: account_id_account_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.account_id_account_seq OWNED BY public.account.id_account;


--
-- TOC entry 228 (class 1259 OID 16436)
-- Name: company; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.company (
    id_company integer NOT NULL,
    name_company character varying(80) NOT NULL,
    address character varying(100) NOT NULL,
    tax_id character varying(32) NOT NULL
);


ALTER TABLE public.company OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 16435)
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
-- TOC entry 5086 (class 0 OID 0)
-- Dependencies: 227
-- Name: company_id_company_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.company_id_company_seq OWNED BY public.company.id_company;


--
-- TOC entry 226 (class 1259 OID 16418)
-- Name: customer; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.customer (
    id_customer integer NOT NULL,
    first_name character varying(20) NOT NULL,
    last_name character varying(20) NOT NULL,
    phone_number character varying(15),
    id_account integer NOT NULL
);


ALTER TABLE public.customer OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 16417)
-- Name: customer_id_customer_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.customer_id_customer_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.customer_id_customer_seq OWNER TO postgres;

--
-- TOC entry 5087 (class 0 OID 0)
-- Dependencies: 225
-- Name: customer_id_customer_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.customer_id_customer_seq OWNED BY public.customer.id_customer;


--
-- TOC entry 230 (class 1259 OID 16444)
-- Name: parking_location; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.parking_location (
    id_parking integer NOT NULL,
    name_parking character varying(100) NOT NULL,
    address_line character varying(100) NOT NULL,
    id_company integer NOT NULL
);


ALTER TABLE public.parking_location OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 16443)
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
-- TOC entry 5088 (class 0 OID 0)
-- Dependencies: 229
-- Name: parking_location_id_parking_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.parking_location_id_parking_seq OWNED BY public.parking_location.id_parking;


--
-- TOC entry 234 (class 1259 OID 16512)
-- Name: parking_session; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.parking_session (
    id_session integer NOT NULL,
    entry_time timestamp without time zone DEFAULT now() NOT NULL,
    exit_time timestamp without time zone,
    price_total_minor numeric(10,2),
    id_parking integer,
    id_spot integer,
    id_vehicle integer,
    payment_status public.status_paid DEFAULT 'Session'::public.status_paid,
    id_account integer
);


ALTER TABLE public.parking_session OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 16511)
-- Name: parking_session_id_session_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.parking_session_id_session_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.parking_session_id_session_seq OWNER TO postgres;

--
-- TOC entry 5089 (class 0 OID 0)
-- Dependencies: 233
-- Name: parking_session_id_session_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.parking_session_id_session_seq OWNED BY public.parking_session.id_session;


--
-- TOC entry 232 (class 1259 OID 16490)
-- Name: parking_spot; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.parking_spot (
    id_spot integer NOT NULL,
    code character varying(10) NOT NULL,
    floor_lvl integer NOT NULL,
    to_reserved boolean NOT NULL,
    type public.status NOT NULL,
    id_parking integer
);


ALTER TABLE public.parking_spot OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 16489)
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
-- TOC entry 5090 (class 0 OID 0)
-- Dependencies: 231
-- Name: parking_spot_id_spot_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.parking_spot_id_spot_seq OWNED BY public.parking_spot.id_spot;


--
-- TOC entry 220 (class 1259 OID 16388)
-- Name: plate_read; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.plate_read (
    id_read integer NOT NULL,
    id_parking integer NOT NULL,
    camera_id integer NOT NULL,
    licence_plate character varying(15) NOT NULL,
    event_time timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.plate_read OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 16387)
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
-- TOC entry 5091 (class 0 OID 0)
-- Dependencies: 219
-- Name: plate_read_id_read_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.plate_read_id_read_seq OWNED BY public.plate_read.id_read;


--
-- TOC entry 238 (class 1259 OID 16547)
-- Name: pricing; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.pricing (
    id_pricing integer NOT NULL,
    curency_code character varying(5) NOT NULL,
    rate_per_min integer NOT NULL,
    free_minutes integer NOT NULL,
    rounding_step_min integer NOT NULL,
    id_parking integer,
    reservation_fee_minor integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.pricing OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 16546)
-- Name: pricing_id_pricing_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.pricing_id_pricing_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pricing_id_pricing_seq OWNER TO postgres;

--
-- TOC entry 5092 (class 0 OID 0)
-- Dependencies: 237
-- Name: pricing_id_pricing_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.pricing_id_pricing_seq OWNED BY public.pricing.id_pricing;


--
-- TOC entry 242 (class 1259 OID 16598)
-- Name: reservation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reservation (
    id_reservation integer NOT NULL,
    start_time timestamp without time zone DEFAULT now(),
    end_time timestamp without time zone,
    type public.status_reservation DEFAULT 'Reserved'::public.status_reservation,
    id_spot integer,
    id_account integer,
    id_parking integer
);


ALTER TABLE public.reservation OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 16597)
-- Name: reservation_id_reservation_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.reservation_id_reservation_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.reservation_id_reservation_seq OWNER TO postgres;

--
-- TOC entry 5093 (class 0 OID 0)
-- Dependencies: 241
-- Name: reservation_id_reservation_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.reservation_id_reservation_seq OWNED BY public.reservation.id_reservation;


--
-- TOC entry 243 (class 1259 OID 16670)
-- Name: v_parking_spot_status; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_parking_spot_status AS
 SELECT psp.id_parking,
    psp.id_spot,
    psp.code,
    psp.floor_lvl,
    psp.to_reserved,
    psp.type AS spot_status_enum,
        CASE
            WHEN (ps.id_session IS NOT NULL) THEN true
            ELSE false
        END AS is_occupied,
        CASE
            WHEN (r.id_reservation IS NOT NULL) THEN true
            ELSE false
        END AS is_reservation_now,
        CASE
            WHEN ((ps.id_session IS NULL) AND (r.id_reservation IS NULL)) THEN true
            ELSE false
        END AS is_free
   FROM ((public.parking_spot psp
     LEFT JOIN public.parking_session ps ON (((ps.id_spot = psp.id_spot) AND (ps.exit_time IS NULL))))
     LEFT JOIN public.reservation r ON (((r.id_spot = psp.id_spot) AND (r.type = 'Reserved'::public.status_reservation) AND ((now() >= r.start_time) AND (now() <= COALESCE((r.end_time)::timestamp with time zone, now()))))));


ALTER VIEW public.v_parking_spot_status OWNER TO postgres;

--
-- TOC entry 245 (class 1259 OID 16718)
-- Name: v_parking_overview; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_parking_overview AS
 SELECT pl.id_parking,
    pl.name_parking,
    pl.address_line,
    count(*) AS spots_total,
    count(*) FILTER (WHERE v.is_free) AS spots_free,
    count(*) FILTER (WHERE v.is_occupied) AS spots_occupied,
    count(*) FILTER (WHERE v.is_reservation_now) AS spots_reserved_now
   FROM (public.v_parking_spot_status v
     JOIN public.parking_location pl USING (id_parking))
  GROUP BY pl.id_parking, pl.name_parking, pl.address_line;


ALTER VIEW public.v_parking_overview OWNER TO postgres;

--
-- TOC entry 246 (class 1259 OID 16723)
-- Name: v_parking_traffic_avg; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_parking_traffic_avg AS
 WITH per_day AS (
         SELECT ps.id_parking,
            date(ps.entry_time) AS day,
            (EXTRACT(dow FROM ps.entry_time))::integer AS dow,
            (EXTRACT(hour FROM ps.entry_time))::integer AS hour,
            count(*) AS entries_count
           FROM public.parking_session ps
          GROUP BY ps.id_parking, (date(ps.entry_time)), (EXTRACT(dow FROM ps.entry_time)), (EXTRACT(hour FROM ps.entry_time))
        )
 SELECT id_parking,
    dow,
    hour,
    (avg(entries_count))::numeric(10,2) AS avg_entries
   FROM per_day
  GROUP BY id_parking, dow, hour;


ALTER VIEW public.v_parking_traffic_avg OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 16401)
-- Name: vehicle; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.vehicle (
    id_vehicle integer NOT NULL,
    licence_plate character varying(15) NOT NULL,
    id_account integer
);


ALTER TABLE public.vehicle OWNER TO postgres;

--
-- TOC entry 244 (class 1259 OID 16703)
-- Name: v_unpaid_sessions; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_unpaid_sessions AS
 SELECT s.id_session,
    s.id_parking,
    s.id_spot,
    s.id_vehicle,
    v.licence_plate,
    COALESCE(s.id_account, v.id_account) AS id_account_effective,
    s.entry_time,
    s.exit_time,
    s.price_total_minor,
    s.payment_status
   FROM ((public.parking_session s
     LEFT JOIN public.vehicle v ON ((v.id_vehicle = s.id_vehicle)))
     LEFT JOIN public.account a ON ((a.id_account = COALESCE(s.id_account, v.id_account))))
  WHERE ((s.exit_time IS NOT NULL) AND (s.payment_status <> 'Paid'::public.status_paid));


ALTER VIEW public.v_unpaid_sessions OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 16400)
-- Name: vehicle_id_vehicle_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.vehicle_id_vehicle_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.vehicle_id_vehicle_seq OWNER TO postgres;

--
-- TOC entry 5094 (class 0 OID 0)
-- Dependencies: 221
-- Name: vehicle_id_vehicle_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.vehicle_id_vehicle_seq OWNED BY public.vehicle.id_vehicle;


--
-- TOC entry 240 (class 1259 OID 16579)
-- Name: virtual_payment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.virtual_payment (
    id_payment integer CONSTRAINT virtual_parking_id_payment_not_null NOT NULL,
    amount_minor integer CONSTRAINT virtual_parking_amount_minor_not_null NOT NULL,
    currency_code character varying(5) CONSTRAINT virtual_parking_currency_code_not_null NOT NULL,
    type public.status_paid DEFAULT 'Pending'::public.status_paid,
    date timestamp without time zone DEFAULT now(),
    id_account integer NOT NULL,
    id_session integer NOT NULL
);


ALTER TABLE public.virtual_payment OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 16578)
-- Name: virtual_parking_id_payment_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.virtual_parking_id_payment_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.virtual_parking_id_payment_seq OWNER TO postgres;

--
-- TOC entry 5095 (class 0 OID 0)
-- Dependencies: 239
-- Name: virtual_parking_id_payment_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.virtual_parking_id_payment_seq OWNED BY public.virtual_payment.id_payment;


--
-- TOC entry 236 (class 1259 OID 16537)
-- Name: wallet; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wallet (
    id_wallet integer NOT NULL,
    balance_minor numeric(10,2) NOT NULL,
    currency_code character varying(5) NOT NULL,
    id_account integer
);


ALTER TABLE public.wallet OWNER TO postgres;

--
-- TOC entry 235 (class 1259 OID 16536)
-- Name: wallet_id_wallet_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.wallet_id_wallet_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.wallet_id_wallet_seq OWNER TO postgres;

--
-- TOC entry 5096 (class 0 OID 0)
-- Dependencies: 235
-- Name: wallet_id_wallet_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.wallet_id_wallet_seq OWNED BY public.wallet.id_wallet;


--
-- TOC entry 4846 (class 2604 OID 16413)
-- Name: account id_account; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account ALTER COLUMN id_account SET DEFAULT nextval('public.account_id_account_seq'::regclass);


--
-- TOC entry 4848 (class 2604 OID 16439)
-- Name: company id_company; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company ALTER COLUMN id_company SET DEFAULT nextval('public.company_id_company_seq'::regclass);


--
-- TOC entry 4847 (class 2604 OID 16421)
-- Name: customer id_customer; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customer ALTER COLUMN id_customer SET DEFAULT nextval('public.customer_id_customer_seq'::regclass);


--
-- TOC entry 4849 (class 2604 OID 16447)
-- Name: parking_location id_parking; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_location ALTER COLUMN id_parking SET DEFAULT nextval('public.parking_location_id_parking_seq'::regclass);


--
-- TOC entry 4851 (class 2604 OID 16515)
-- Name: parking_session id_session; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session ALTER COLUMN id_session SET DEFAULT nextval('public.parking_session_id_session_seq'::regclass);


--
-- TOC entry 4850 (class 2604 OID 16493)
-- Name: parking_spot id_spot; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_spot ALTER COLUMN id_spot SET DEFAULT nextval('public.parking_spot_id_spot_seq'::regclass);


--
-- TOC entry 4843 (class 2604 OID 16391)
-- Name: plate_read id_read; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plate_read ALTER COLUMN id_read SET DEFAULT nextval('public.plate_read_id_read_seq'::regclass);


--
-- TOC entry 4855 (class 2604 OID 16550)
-- Name: pricing id_pricing; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pricing ALTER COLUMN id_pricing SET DEFAULT nextval('public.pricing_id_pricing_seq'::regclass);


--
-- TOC entry 4860 (class 2604 OID 16601)
-- Name: reservation id_reservation; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reservation ALTER COLUMN id_reservation SET DEFAULT nextval('public.reservation_id_reservation_seq'::regclass);


--
-- TOC entry 4845 (class 2604 OID 16404)
-- Name: vehicle id_vehicle; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vehicle ALTER COLUMN id_vehicle SET DEFAULT nextval('public.vehicle_id_vehicle_seq'::regclass);


--
-- TOC entry 4857 (class 2604 OID 16582)
-- Name: virtual_payment id_payment; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.virtual_payment ALTER COLUMN id_payment SET DEFAULT nextval('public.virtual_parking_id_payment_seq'::regclass);


--
-- TOC entry 4854 (class 2604 OID 16540)
-- Name: wallet id_wallet; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallet ALTER COLUMN id_wallet SET DEFAULT nextval('public.wallet_id_wallet_seq'::regclass);


--
-- TOC entry 5061 (class 0 OID 16410)
-- Dependencies: 224
-- Data for Name: account; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.account (id_account, email, password_hash) FROM stdin;
1	client1@example.com	hash_client1
2	admin1@example.com	hash_admin1
7	am@gmail.com	123
\.


--
-- TOC entry 5065 (class 0 OID 16436)
-- Dependencies: 228
-- Data for Name: company; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.company (id_company, name_company, address, tax_id) FROM stdin;
1	SmartPark Sp. z o.o.	ul. Parkingowa 1, Kraków	PL1234567890
\.


--
-- TOC entry 5063 (class 0 OID 16418)
-- Dependencies: 226
-- Data for Name: customer; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.customer (id_customer, first_name, last_name, phone_number, id_account) FROM stdin;
1	Anna	Kowalska	600100200	1
5	Mikołaj	Leszczyk	999887753	7
\.


--
-- TOC entry 5067 (class 0 OID 16444)
-- Dependencies: 230
-- Data for Name: parking_location; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.parking_location (id_parking, name_parking, address_line, id_company) FROM stdin;
1	SmartPark Galeria	ul. Galeryjna 10, Kraków	1
2	SmartPark Biuro	ul. Biurowa 5, Kraków	1
\.


--
-- TOC entry 5071 (class 0 OID 16512)
-- Dependencies: 234
-- Data for Name: parking_session; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.parking_session (id_session, entry_time, exit_time, price_total_minor, id_parking, id_spot, id_vehicle, payment_status, id_account) FROM stdin;
1	2025-11-05 10:52:20.906413	2025-11-05 11:52:20.906413	30.00	1	1	1	Paid	1
11	2025-11-05 13:31:54.343955	2025-11-05 14:31:54.343955	10.00	1	1	1	Pending	1
12	2025-11-05 13:31:54.343955	2025-11-05 14:31:54.343955	20.00	1	2	2	Pending	2
14	2025-11-05 13:31:54.343955	2025-11-05 14:31:54.343955	10.00	2	1	1	Pending	1
2	2025-11-05 12:32:49.755569	2025-11-05 11:52:20.906413	50.00	1	2	1	Paid	1
13	2025-11-05 13:31:54.343955	2025-11-05 14:31:54.343955	10.00	1	3	2	Pending	2
15	2025-11-05 18:33:38.139472	\N	\N	2	\N	5	Pending	\N
16	2025-11-05 19:43:01.098355	\N	\N	2	4	6	Pending	\N
17	2025-11-07 12:23:25.789521	2025-11-07 12:23:31.750678	0.00	1	1	1	Pending	2
18	2025-11-07 12:24:56.514006	2025-11-07 14:25:27.949516	55.00	1	1	1	Pending	2
19	2025-11-07 12:37:39.000238	2025-11-07 15:37:56.847082	85.00	1	2	1	Pending	2
20	2025-11-07 12:57:15.739016	2025-11-07 15:57:19.612399	85.00	1	2	1	Pending	2
21	2025-11-07 13:00:28.421768	2025-11-07 16:00:28.421768	82.50	1	2	1	Pending	2
22	2025-11-07 13:01:16.489145	2025-11-07 16:01:16.489145	82.50	1	2	1	Pending	2
23	2025-11-07 13:02:04.782236	2025-11-07 16:02:04.782236	82.50	1	2	1	Pending	2
24	2025-11-07 13:03:02.850029	2025-11-07 15:03:02.850029	52.50	1	1	1	Pending	2
25	2025-11-07 13:05:10.762499	2025-11-07 15:05:19.751609	55.00	1	1	1	Pending	2
26	2025-11-07 13:07:31.495686	2025-11-07 15:07:39.778091	55.00	1	2	1	Pending	2
27	2025-11-07 13:08:54.888189	2025-11-07 15:09:02.100915	52.50	1	1	1	Pending	2
28	2025-11-07 13:10:34.170987	2025-11-07 15:10:41.093901	5.25	1	2	1	Pending	2
31	2025-11-11 14:16:00.781136	2025-11-11 14:17:02.931789	0.00	1	1	7	Pending	2
33	2025-11-11 14:17:31.341911	\N	\N	1	1	8	Session	\N
32	2025-11-11 14:17:11.054028	2025-11-11 14:17:49.20318	0.00	1	2	7	Pending	2
34	2025-11-11 14:17:54.667606	\N	\N	1	2	9	Session	\N
\.


--
-- TOC entry 5069 (class 0 OID 16490)
-- Dependencies: 232
-- Data for Name: parking_spot; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.parking_spot (id_spot, code, floor_lvl, to_reserved, type, id_parking) FROM stdin;
1	A1	0	f	Available	1
2	A2	0	f	Available	1
3	R1	0	t	Available	1
4	B1	-1	f	Available	2
5	B2	-1	t	Available	2
\.


--
-- TOC entry 5057 (class 0 OID 16388)
-- Dependencies: 220
-- Data for Name: plate_read; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.plate_read (id_read, id_parking, camera_id, licence_plate, event_time) FROM stdin;
1	1	101	KR1AK01	2025-11-05 10:51:12.714489
2	1	102	KR1AK01	2025-11-05 11:41:12.714489
3	2	103	KR08FGCT	2025-11-05 18:33:38.139472
4	2	104	KR08FG	2025-11-05 19:43:01.098355
5	1	101	KR1AK01	2025-11-07 12:23:25.789521
6	1	101	KR1AK01	2025-11-07 12:24:56.514006
7	1	101	KR1AK01	2025-11-07 12:37:39.000238
8	1	101	KR1AK01	2025-11-07 12:57:15.739016
9	1	101	KR1AK01	2025-11-07 13:00:28.421768
10	1	101	KR1AK01	2025-11-07 13:01:16.489145
11	1	101	KR1AK01	2025-11-07 13:02:04.782236
12	1	101	KR1AK01	2025-11-07 13:03:02.850029
13	1	101	KR1AK01	2025-11-07 13:05:10.762499
14	1	101	KR1AK01	2025-11-07 13:07:31.495686
15	1	101	KR1AK01	2025-11-07 13:08:54.888189
16	1	101	KR1AK01	2025-11-07 13:10:34.170987
17	1	101	KR08FGCT	2025-11-07 13:17:10.649009
18	1	101	GETF144	2025-11-11 13:38:02.340902
19	1	101	GETF144	2025-11-11 14:16:00.781136
20	1	101	GETF144	2025-11-11 14:17:11.054028
21	1	101	GETF14	2025-11-11 14:17:31.341911
22	1	101	GETF1644	2025-11-11 14:17:54.667606
\.


--
-- TOC entry 5075 (class 0 OID 16547)
-- Dependencies: 238
-- Data for Name: pricing; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.pricing (id_pricing, curency_code, rate_per_min, free_minutes, rounding_step_min, id_parking, reservation_fee_minor) FROM stdin;
1	PLN	5	15	5	1	300
2	PLN	3	10	10	2	360
\.


--
-- TOC entry 5079 (class 0 OID 16598)
-- Dependencies: 242
-- Data for Name: reservation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.reservation (id_reservation, start_time, end_time, type, id_spot, id_account, id_parking) FROM stdin;
1	2025-11-05 13:53:23.193405	2025-11-05 15:53:23.193405	Reserved	3	1	1
\.


--
-- TOC entry 5059 (class 0 OID 16401)
-- Dependencies: 222
-- Data for Name: vehicle; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.vehicle (id_vehicle, licence_plate, id_account) FROM stdin;
2	KR123AB	1
3	KR555BB	1
4	KR777CC	1
1	KR1AK01	2
5	KR08FGCT	\N
6	KR08FG	\N
7	GETF144	2
8	GETF14	\N
11	BBAAA	2
9	GETF1644	1
\.


--
-- TOC entry 5077 (class 0 OID 16579)
-- Dependencies: 240
-- Data for Name: virtual_payment; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.virtual_payment (id_payment, amount_minor, currency_code, type, date, id_account, id_session) FROM stdin;
2	30	PLN	Paid	2025-11-05 12:04:02.25775	1	1
1	50	PLN	Paid	2025-11-05 09:53:44.816925	1	2
\.


--
-- TOC entry 5073 (class 0 OID 16537)
-- Dependencies: 236
-- Data for Name: wallet; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wallet (id_wallet, balance_minor, currency_code, id_account) FROM stdin;
1	100.00	PLN	1
4	0.00	PLN	7
\.


--
-- TOC entry 5097 (class 0 OID 0)
-- Dependencies: 223
-- Name: account_id_account_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.account_id_account_seq', 7, true);


--
-- TOC entry 5098 (class 0 OID 0)
-- Dependencies: 227
-- Name: company_id_company_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.company_id_company_seq', 1, false);


--
-- TOC entry 5099 (class 0 OID 0)
-- Dependencies: 225
-- Name: customer_id_customer_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.customer_id_customer_seq', 5, true);


--
-- TOC entry 5100 (class 0 OID 0)
-- Dependencies: 229
-- Name: parking_location_id_parking_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.parking_location_id_parking_seq', 1, false);


--
-- TOC entry 5101 (class 0 OID 0)
-- Dependencies: 233
-- Name: parking_session_id_session_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.parking_session_id_session_seq', 34, true);


--
-- TOC entry 5102 (class 0 OID 0)
-- Dependencies: 231
-- Name: parking_spot_id_spot_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.parking_spot_id_spot_seq', 1, false);


--
-- TOC entry 5103 (class 0 OID 0)
-- Dependencies: 219
-- Name: plate_read_id_read_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.plate_read_id_read_seq', 22, true);


--
-- TOC entry 5104 (class 0 OID 0)
-- Dependencies: 237
-- Name: pricing_id_pricing_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.pricing_id_pricing_seq', 1, false);


--
-- TOC entry 5105 (class 0 OID 0)
-- Dependencies: 241
-- Name: reservation_id_reservation_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.reservation_id_reservation_seq', 1, false);


--
-- TOC entry 5106 (class 0 OID 0)
-- Dependencies: 221
-- Name: vehicle_id_vehicle_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.vehicle_id_vehicle_seq', 11, true);


--
-- TOC entry 5107 (class 0 OID 0)
-- Dependencies: 239
-- Name: virtual_parking_id_payment_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.virtual_parking_id_payment_seq', 1, false);


--
-- TOC entry 5108 (class 0 OID 0)
-- Dependencies: 235
-- Name: wallet_id_wallet_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.wallet_id_wallet_seq', 4, true);


--
-- TOC entry 4870 (class 2606 OID 16416)
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id_account);


--
-- TOC entry 4874 (class 2606 OID 16442)
-- Name: company company_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_pkey PRIMARY KEY (id_company);


--
-- TOC entry 4872 (class 2606 OID 16424)
-- Name: customer customer_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id_customer);


--
-- TOC entry 4876 (class 2606 OID 16450)
-- Name: parking_location parking_location_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_location
    ADD CONSTRAINT parking_location_pkey PRIMARY KEY (id_parking);


--
-- TOC entry 4880 (class 2606 OID 16520)
-- Name: parking_session parking_session_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session
    ADD CONSTRAINT parking_session_pkey PRIMARY KEY (id_session);


--
-- TOC entry 4878 (class 2606 OID 16500)
-- Name: parking_spot parking_spot_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_spot
    ADD CONSTRAINT parking_spot_pkey PRIMARY KEY (id_spot);


--
-- TOC entry 4864 (class 2606 OID 16399)
-- Name: plate_read plate_read_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plate_read
    ADD CONSTRAINT plate_read_pkey PRIMARY KEY (id_read);


--
-- TOC entry 4884 (class 2606 OID 16557)
-- Name: pricing pricing_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pricing
    ADD CONSTRAINT pricing_pkey PRIMARY KEY (id_pricing);


--
-- TOC entry 4888 (class 2606 OID 16606)
-- Name: reservation reservation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reservation
    ADD CONSTRAINT reservation_pkey PRIMARY KEY (id_reservation);


--
-- TOC entry 4866 (class 2606 OID 16623)
-- Name: vehicle vehicle_licence_plate_uk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vehicle
    ADD CONSTRAINT vehicle_licence_plate_uk UNIQUE (licence_plate);


--
-- TOC entry 4868 (class 2606 OID 16408)
-- Name: vehicle vehicle_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vehicle
    ADD CONSTRAINT vehicle_pkey PRIMARY KEY (id_vehicle);


--
-- TOC entry 4886 (class 2606 OID 16589)
-- Name: virtual_payment virtual_parking_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.virtual_payment
    ADD CONSTRAINT virtual_parking_pkey PRIMARY KEY (id_payment);


--
-- TOC entry 4882 (class 2606 OID 16545)
-- Name: wallet wallet_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallet
    ADD CONSTRAINT wallet_pkey PRIMARY KEY (id_wallet);


--
-- TOC entry 4891 (class 2606 OID 16425)
-- Name: customer customer_id_account_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_id_account_fkey FOREIGN KEY (id_account) REFERENCES public.account(id_account);


--
-- TOC entry 4889 (class 2606 OID 16466)
-- Name: plate_read fk_plate_from_locate_parking; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plate_read
    ADD CONSTRAINT fk_plate_from_locate_parking FOREIGN KEY (id_parking) REFERENCES public.parking_location(id_parking);


--
-- TOC entry 4890 (class 2606 OID 16690)
-- Name: vehicle fk_vehicle_account; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vehicle
    ADD CONSTRAINT fk_vehicle_account FOREIGN KEY (id_account) REFERENCES public.account(id_account);


--
-- TOC entry 4892 (class 2606 OID 16451)
-- Name: parking_location parking_location_id_company_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_location
    ADD CONSTRAINT parking_location_id_company_fkey FOREIGN KEY (id_company) REFERENCES public.company(id_company);


--
-- TOC entry 4894 (class 2606 OID 16640)
-- Name: parking_session parking_session_id_account_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session
    ADD CONSTRAINT parking_session_id_account_fkey FOREIGN KEY (id_account) REFERENCES public.account(id_account);


--
-- TOC entry 4895 (class 2606 OID 16521)
-- Name: parking_session parking_session_id_parking_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session
    ADD CONSTRAINT parking_session_id_parking_fkey FOREIGN KEY (id_parking) REFERENCES public.parking_location(id_parking);


--
-- TOC entry 4896 (class 2606 OID 16526)
-- Name: parking_session parking_session_id_spot_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session
    ADD CONSTRAINT parking_session_id_spot_fkey FOREIGN KEY (id_spot) REFERENCES public.parking_spot(id_spot);


--
-- TOC entry 4897 (class 2606 OID 16531)
-- Name: parking_session parking_session_id_vehicle_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_session
    ADD CONSTRAINT parking_session_id_vehicle_fkey FOREIGN KEY (id_vehicle) REFERENCES public.vehicle(id_vehicle);


--
-- TOC entry 4893 (class 2606 OID 16501)
-- Name: parking_spot parking_spot_id_parking_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parking_spot
    ADD CONSTRAINT parking_spot_id_parking_fkey FOREIGN KEY (id_parking) REFERENCES public.parking_location(id_parking);


--
-- TOC entry 4899 (class 2606 OID 16558)
-- Name: pricing pricing_id_parking_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pricing
    ADD CONSTRAINT pricing_id_parking_fkey FOREIGN KEY (id_parking) REFERENCES public.parking_location(id_parking);


--
-- TOC entry 4902 (class 2606 OID 16612)
-- Name: reservation reservation_id_account_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reservation
    ADD CONSTRAINT reservation_id_account_fkey FOREIGN KEY (id_account) REFERENCES public.account(id_account);


--
-- TOC entry 4903 (class 2606 OID 16617)
-- Name: reservation reservation_id_parking_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reservation
    ADD CONSTRAINT reservation_id_parking_fkey FOREIGN KEY (id_parking) REFERENCES public.parking_location(id_parking);


--
-- TOC entry 4904 (class 2606 OID 16607)
-- Name: reservation reservation_id_spot_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reservation
    ADD CONSTRAINT reservation_id_spot_fkey FOREIGN KEY (id_spot) REFERENCES public.parking_spot(id_spot);


--
-- TOC entry 4900 (class 2606 OID 16629)
-- Name: virtual_payment virtual_payment_id_account_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.virtual_payment
    ADD CONSTRAINT virtual_payment_id_account_fkey FOREIGN KEY (id_account) REFERENCES public.account(id_account);


--
-- TOC entry 4901 (class 2606 OID 16634)
-- Name: virtual_payment virtual_payment_id_session_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.virtual_payment
    ADD CONSTRAINT virtual_payment_id_session_fkey FOREIGN KEY (id_session) REFERENCES public.parking_session(id_session);


--
-- TOC entry 4898 (class 2606 OID 16624)
-- Name: wallet wallet_id_account_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallet
    ADD CONSTRAINT wallet_id_account_fkey FOREIGN KEY (id_account) REFERENCES public.account(id_account);


-- Completed on 2025-11-12 12:09:19

--
-- PostgreSQL database dump complete
--

\unrestrict OjSOeAvHdvaBDe78aT83JMTR1RsELgQGIC3VkYNpRlNp3Qsh2e1TAxJx7RU3b4n

