DROP SCHEMA IF EXISTS Messenger CASCADE;

CREATE SCHEMA Messenger;

SET search_path to Messenger;

CREATE SEQUENCE profile_id_seq CACHE 100;

CREATE TABLE Profile (
    id integer DEFAULT nextval('profile_id_seq'),
    full_name text NOT NULL,
    email text NOT NULL,
    phone text NOT NULL,
    country_code varchar(3),
    hashed_password text NOT NULL,
    user_picture_url text,
    PRIMARY KEY(id, country_code)
) PARTITION BY LIST (country_code);

CREATE TABLE Profile_USA
    PARTITION OF Profile
    FOR VALUES IN ('USA') TABLESPACE usa_tablespace;

CREATE TABLE Profile_EU
    PARTITION OF Profile
    FOR VALUES IN ('DEU') TABLESPACE eu_tablespace;

CREATE TABLE Profile_APAC
    PARTITION OF Profile
    FOR VALUES IN ('JPN') TABLESPACE apac_tablespace;



CREATE SEQUENCE workspace_id_seq CACHE 100;

CREATE TABLE Workspace(
    id integer DEFAULT nextval('workspace_id_seq'),
    name text NOT NULL,
    country_code varchar(3),
    PRIMARY KEY(id, country_code)
) PARTITION BY LIST (country_code);

CREATE TABLE Workspace_USA
    PARTITION OF Workspace
    FOR VALUES IN ('USA') TABLESPACE usa_tablespace;

CREATE TABLE Workspace_EU
    PARTITION OF Workspace
    FOR VALUES IN ('DEU') TABLESPACE eu_tablespace;

CREATE TABLE Workspace_APAC
    PARTITION OF Workspace
    FOR VALUES IN ('JPN') TABLESPACE apac_tablespace;



CREATE TABLE Workspace_Profile(
    workspace_id integer,
    profile_id integer,
    workspace_country varchar(3),
    profile_country varchar(3) NOT NULL,
    PRIMARY KEY(workspace_id, profile_id, workspace_country)
) PARTITION BY LIST (workspace_country);

CREATE TABLE Workspace_Profile_USA
    PARTITION OF Workspace_Profile
    FOR VALUES IN ('USA') TABLESPACE usa_tablespace;

CREATE TABLE Workspace_Profile_EU
    PARTITION OF Workspace_Profile
    FOR VALUES IN ('DEU') TABLESPACE eu_tablespace;

CREATE TABLE Workspace_Profile_APAC
    PARTITION OF Workspace_Profile
    FOR VALUES IN ('JPN') TABLESPACE apac_tablespace;



CREATE SEQUENCE channel_id_seq CACHE 100;

CREATE TABLE Channel(
    id integer DEFAULT nextval('channel_id_seq'),
    name text NOT NULL,
    workspace_id integer NOT NULL,
    country_code text NOT NULL,
    PRIMARY KEY(id, country_code)
) PARTITION BY LIST (country_code);

CREATE TABLE Channel_USA
    PARTITION OF Channel
    FOR VALUES IN ('USA') TABLESPACE usa_tablespace;

CREATE TABLE Channel_EU
    PARTITION OF Channel
    FOR VALUES IN ('DEU') TABLESPACE eu_tablespace;

CREATE TABLE Channel_APAC
    PARTITION OF Channel
    FOR VALUES IN ('JPN') TABLESPACE apac_tablespace;



CREATE SEQUENCE message_id_seq CACHE 100;

CREATE TABLE Message(
    id integer NOT NULL DEFAULT nextval('message_id_seq'),
    channel_id integer,
    sender_id integer NOT NULL,
    message text NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    country_code varchar(3) NOT NULL,
    sender_country_code varchar(3) NOT NULL,
    PRIMARY KEY(id, country_code)
) PARTITION BY LIST (country_code);

CREATE TABLE Message_USA
    PARTITION OF Message
    FOR VALUES IN ('USA') TABLESPACE usa_tablespace;

CREATE TABLE Message_EU
    PARTITION OF Message
    FOR VALUES IN ('DEU') TABLESPACE eu_tablespace;

CREATE TABLE Message_APAC
    PARTITION OF Message
    FOR VALUES IN ('JPN') TABLESPACE apac_tablespace;