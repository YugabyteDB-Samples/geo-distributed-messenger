DROP SCHEMA IF EXISTS Messenger CASCADE;

CREATE SCHEMA Messenger;

SET search_path to Messenger;

CREATE SEQUENCE profile_id_seq CACHE 100;

CREATE TABLE Profile (
    id integer NOT NULL DEFAULT nextval('profile_id_seq'),
    full_name text NOT NULL,
    email text NOT NULL,
    phone text NOT NULL,
    country_code text NOT NULL,
    hashed_password text NOT NULL,
    user_picture_url text,
    PRIMARY KEY(id, country_code)
);