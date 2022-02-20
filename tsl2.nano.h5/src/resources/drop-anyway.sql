
-- drop all anyway tables

-- first: drop constraints
ALTER TABLE Organisation DROP CONSTRAINT Organisation_Party_fk;
ALTER TABLE Organisation DROP CONSTRAINT Organisation_Property_fk;
ALTER TABLE Organisation DROP CONSTRAINT Organisation_Item_fk;
ALTER TABLE Classification DROP CONSTRAINT Classification_Item_fk;
ALTER TABLE Category DROP CONSTRAINT Category_Area_fk;
ALTER TABLE Area DROP CONSTRAINT Area_Type_fk;
ALTER TABLE Type DROP CONSTRAINT Type_Item_fk;
ALTER TABLE Item DROP CONSTRAINT Item_Property_fk;
ALTER TABLE Item DROP CONSTRAINT Item_ChargeItem_fk;
ALTER TABLE ChargeItem DROP CONSTRAINT ChargeItem_Charge_fk;
ALTER TABLE Coordinates DROP CONSTRAINT Coordinates_Location_fk;
ALTER TABLE Address DROP CONSTRAINT Address_Location_fk;
ALTER TABLE Party DROP CONSTRAINT Party_Charge_fk;
ALTER TABLE Party DROP CONSTRAINT Party_Location_fk;
ALTER TABLE Party DROP CONSTRAINT Party_Property_fk;
ALTER TABLE Party DROP CONSTRAINT Party_Mission_fk
ALTER TABLE Charge DROP CONSTRAINT Charge_Discharge_fk;
ALTER TABLE Account DROP CONSTRAINT Account_Discharge_fk;
ALTER TABLE Digital DROP CONSTRAINT Digital_Location_fk;
-- some database providers don't support the ansi-sql DROP CONSTRAINT 
commit;

DROP TABLE Organisation;
DROP TABLE Classification;
DROP TABLE Category;
DROP TABLE Area;
DROP TABLE Type;
DROP TABLE Mission;
DROP TABLE Item;
DROP TABLE ChargeItem;
DROP TABLE Coordinate;
DROP TABLE Address;
DROP TABLE Party;
DROP TABLE Property;
DROP TABLE Location;
DROP TABLE Charge;
DROP TABLE Discharge;
DROP TABLE Account;
DROP TABLE Digital;
