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
    PRIMARY KEY(id, country_code),
    UNIQUE(id)
);

CREATE SEQUENCE workspace_id_seq CACHE 100;

CREATE TABLE Workspace(
    id integer DEFAULT nextval('workspace_id_seq'),
    name text NOT NULL,
    country_code varchar(3),
    PRIMARY KEY(id, country_code)
);

CREATE TABLE Workspace_Profile(
    workspace_id integer,
    profile_id integer,
    workspace_country varchar(3),
    PRIMARY KEY(workspace_id, profile_id, workspace_country),
    FOREIGN KEY(workspace_id, workspace_country) REFERENCES Workspace(id, country_code),
    FOREIGN KEY(profile_id) REFERENCES Profile(id)
);

CREATE SEQUENCE channel_id_seq CACHE 100;

CREATE TABLE Channel(
    id integer DEFAULT nextval('channel_id_seq'),
    name text NOT NULL,
    workspace_id integer NOT NULL,
    country_code text NOT NULL,
    PRIMARY KEY(id, country_code),
    FOREIGN KEY(workspace_id, country_code) REFERENCES Workspace(id, country_code)
);

CREATE SEQUENCE message_id_seq CACHE 100;

CREATE TABLE Message(
    id integer NOT NULL DEFAULT nextval('message_id_seq'),
    channel_id integer,
    sender_id integer NOT NULL,
    message text NOT NULL,
    sent_at TIMESTAMP(0) DEFAULT NOW(),
    country_code text NOT NULL,
    PRIMARY KEY(id, country_code),
    FOREIGN KEY(channel_id, country_code) REFERENCES Channel(id, country_code),
    FOREIGN KEY(sender_id) REFERENCES Profile(id)
);
