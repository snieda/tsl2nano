-- SQL-92

create schema ADMIN

CREATE TABLE ADMIN.Grouprole (
                id INTEGER NOT NULL,
                CONSTRAINT id PRIMARY KEY (id)
);


CREATE TABLE ADMIN.Role (
                id INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                CONSTRAINT id PRIMARY KEY (id)
);


CREATE TABLE ADMIN.History (
                id INTEGER NOT NULL,
                table VARCHAR(64) NOT NULL,
                column VARCHAR(64) NOT NULL,
                action VARCHAR(64) NOT NULL,
                date TIMESTAMP NOT NULL,
                CONSTRAINT id PRIMARY KEY (id)
);


CREATE TABLE ADMIN.User (
                id INTEGER NOT NULL,
                short VARCHAR(8) NOT NULL,
                name VARCHAR(64) NOT NULL,
                name2 VARCHAR(64) NOT NULL,
                passwd VARCHAR(64) NOT NULL,
                CONSTRAINT id PRIMARY KEY (id)
);
-- COMMENT ON TABLE ADMIN.User IS 'application user';


CREATE TABLE ADMIN.Usergroup (
                id INTEGER NOT NULL,
                CONSTRAINT id PRIMARY KEY (id)
);


CREATE TABLE ADMIN.Group (
                id INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                CONSTRAINT id PRIMARY KEY (id)
);


CREATE TABLE ADMIN.Version (
                id INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                date TIMESTAMP NOT NULL,
                CONSTRAINT id PRIMARY KEY (id)
);
-- COMMENT ON TABLE ADMIN.Version IS 'Version der Datenbank-Instanz';


ALTER TABLE ADMIN.Group ADD CONSTRAINT ADMIN_Grouprole_ADMIN_Group_fk
FOREIGN KEY (id)
REFERENCES ADMIN.Grouprole (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
NOT DEFERRABLE;

ALTER TABLE ADMIN.Role ADD CONSTRAINT ADMIN_Grouprole_ADMIN_Role_fk
FOREIGN KEY (id)
REFERENCES ADMIN.Grouprole (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
NOT DEFERRABLE;

ALTER TABLE ADMIN.Usergroup ADD CONSTRAINT ADMIN_User_ADMIN_Usergroup_fk
FOREIGN KEY (id)
REFERENCES ADMIN.User (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
NOT DEFERRABLE;

ALTER TABLE ADMIN.Group ADD CONSTRAINT ADMIN_Usergroup_ADMIN_Group_fk
FOREIGN KEY (id)
REFERENCES ADMIN.Usergroup (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
NOT DEFERRABLE;
