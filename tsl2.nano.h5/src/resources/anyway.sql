-- SQL-92

CREATE TABLE Organisation (
                id INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                description VARCHAR(256),
                CONSTRAINT idx_organisation PRIMARY KEY (id)
);
-- COMMENT ON TABLE Organisation IS 'root definition - may be used for different clients';


CREATE UNIQUE INDEX Organisation_idx
 ON Organisation
 ( name );

CREATE TABLE Classification (
                id INTEGER NOT NULL,
                value INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                CONSTRAINT idx_classification PRIMARY KEY (id)
);
-- COMMENT ON TABLE Classification IS 'items level';


CREATE INDEX Classification_idx
 ON Classification
 ( name );

CREATE TABLE Category (
                id INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                CONSTRAINT idx_category PRIMARY KEY (id)
);
-- COMMENT ON TABLE Category IS 'combines a group of areas. may be useless on some use cases';


CREATE INDEX Category_idx
 ON Category
 ( name );

CREATE TABLE Area (
                id INTEGER NOT NULL,
                category INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                CONSTRAINT idx_area PRIMARY KEY (id)
);
-- COMMENT ON TABLE Area IS 'an area combines a group of item types';


CREATE INDEX Area_idx
 ON Area
 ( name );

CREATE TABLE Type (
                id INTEGER NOT NULL,
                area INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                CONSTRAINT idx_type PRIMARY KEY (id)
);
-- COMMENT ON TABLE Type IS 'item type';


CREATE INDEX Type_idx
 ON Type
 ( name );

CREATE TABLE Item (
                id INTEGER NOT NULL,
                orga INTEGER NOT NULL,
                class INTEGER NOT NULL,
                type INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                start DATE NOT NULL,
                end DATE,
                value NUMERIC NOT NULL,
                description VARCHAR(256),
                icon BLOB NOT NULL,
                CONSTRAINT idx_item PRIMARY KEY (id)
);
-- COMMENT ON TABLE Item IS 'shopping item, menu item in a restaurant, time-period in a timesheet, etc.';
-- COMMENT ON COLUMN Item.end IS 'availability';
-- COMMENT ON COLUMN Item.icon IS 'item image for better user experience';


CREATE INDEX Item_idx
 ON Item
 ( name );

CREATE TABLE ChargeItem (
                id INTEGER NOT NULL,
                charge INTEGER NOT NULL,
                item INTEGER NOT NULL,
                CONSTRAINT idx_chargeitem PRIMARY KEY (id)
);
-- COMMENT ON TABLE ChargeItem IS 'mapping between party and charge - resolving a many-to-many relation. a party will charge items.';


CREATE TABLE Coordinate (
                id INTEGER NOT NULL,
                x NUMERIC NOT NULL,
                y NUMERIC NOT NULL,
                z NUMERIC DEFAULT 0,
                CONSTRAINT idx_coordinate PRIMARY KEY (id)
);


CREATE TABLE Address (
                id INTEGER NOT NULL,
                street VARCHAR(64) NOT NULL,
                code VARCHAR(64) NOT NULL,
                city VARCHAR(64) NOT NULL,
                country VARCHAR(64) NOT NULL,
                CONSTRAINT idx_address PRIMARY KEY (id)
);


CREATE TABLE Party (
                id INTEGER NOT NULL,
                orga INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                description VARCHAR(256),
                CONSTRAINT idx_party PRIMARY KEY (id)
);
-- COMMENT ON TABLE Party IS 'may be a person, client, reservation item like a restaurant table etc.';


CREATE TABLE Property (
                id INTEGER NOT NULL,
                item INTEGER,
                party INTEGER,
                orga INTEGER,
                akey VARCHAR(64) NOT NULL,
                avalue VARCHAR(64) NOT NULL,
                CONSTRAINT idx_property PRIMARY KEY (id)
);
-- COMMENT ON TABLE Property IS 'extended optional properties for main entries like an organisation, party or item. 
-- on a party, it may be a bank connection or a party category.';


CREATE INDEX Property_idx
 ON Property
 ( akey );

CREATE TABLE Location (
                id INTEGER NOT NULL,
                address INTEGER,
                coordinate INTEGER,
                party INTEGER NOT NULL,
                name VARCHAR(64) NOT NULL,
                CONSTRAINT idx_location PRIMARY KEY (id)
);
-- COMMENT ON TABLE Location IS 'location of a party. can have an address or a coordinate';


CREATE INDEX Location_idx
 ON Location
 ( name );

CREATE TABLE Charge (
                id INTEGER NOT NULL,
                party INTEGER NOT NULL,
                chargeitem INTEGER NOT NULL,
                fromdate DATE NOT NULL,
                fromtime TIME NOT NULL,
                todate DATE NOT NULL,
                totime TIME NOT NULL,
                value DECIMAL NOT NULL,
                comment VARCHAR(512),
                CONSTRAINT idx_charge PRIMARY KEY (id)
);
-- COMMENT ON TABLE Charge IS 'agreement, booking or reservation';


CREATE INDEX Charge_idx
 ON Charge
 ( fromdate );

CREATE TABLE Discharge (
                id INTEGER NOT NULL,
                charge INTEGER NOT NULL,
                date TIMESTAMP NOT NULL,
                value DECIMAL NOT NULL,
                comment VARCHAR(512),
                CONSTRAINT idx_discharge PRIMARY KEY (id)
);


CREATE INDEX Discharge_idx
 ON Discharge
 ( date );

ALTER TABLE Party ADD CONSTRAINT Organisation_Party_fk
FOREIGN KEY (orga)
REFERENCES Organisation (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Property ADD CONSTRAINT Organisation_Property_fk
FOREIGN KEY (orga)
REFERENCES Organisation (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Item ADD CONSTRAINT Organisation_Item_fk
FOREIGN KEY (orga)
REFERENCES Organisation (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Item ADD CONSTRAINT Classification_Item_fk
FOREIGN KEY (class)
REFERENCES Classification (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Area ADD CONSTRAINT Category_Area_fk
FOREIGN KEY (category)
REFERENCES Category (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Type ADD CONSTRAINT Area_Type_fk
FOREIGN KEY (area)
REFERENCES Area (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Item ADD CONSTRAINT Type_Item_fk
FOREIGN KEY (type)
REFERENCES Type (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Property ADD CONSTRAINT Item_Property_fk
FOREIGN KEY (item)
REFERENCES Item (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE ChargeItem ADD CONSTRAINT Item_ChargeItem_fk
FOREIGN KEY (item)
REFERENCES Item (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Charge ADD CONSTRAINT ChargeItem_Charge_fk
FOREIGN KEY (chargeitem)
REFERENCES ChargeItem (id)
ON DELETE CASCADE
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Location ADD CONSTRAINT Coordinates_Location_fk
FOREIGN KEY (coordinate)
REFERENCES Coordinate (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Location ADD CONSTRAINT Address_Location_fk
FOREIGN KEY (address)
REFERENCES Address (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Charge ADD CONSTRAINT Party_Charge_fk
FOREIGN KEY (party)
REFERENCES Party (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Location ADD CONSTRAINT Party_Location_fk
FOREIGN KEY (party)
REFERENCES Party (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Property ADD CONSTRAINT Party_Property_fk
FOREIGN KEY (party)
REFERENCES Party (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;

ALTER TABLE Discharge ADD CONSTRAINT Charge_Discharge_fk
FOREIGN KEY (id)
REFERENCES Charge (id)
ON DELETE NO ACTION
ON UPDATE NO ACTION
-- NOT DEFERRABLE
;
