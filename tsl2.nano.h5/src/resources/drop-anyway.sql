
-- drop all anyway tables

-- first: drop constraints
DROP CONSTRAINT Organisation_Party_fk;
DROP CONSTRAINT Organisation_Property_fk;
DROP CONSTRAINT Organisation_Item_fk;
DROP CONSTRAINT Classification_Item_fk;
DROP CONSTRAINT Category_Area_fk;
DROP CONSTRAINT Area_Type_fk;
DROP CONSTRAINT Type_Item_fk;
DROP CONSTRAINT Item_Property_fk;
DROP CONSTRAINT Item_ChargeItem_fk;
DROP CONSTRAINT ChargeItem_Charge_fk;
DROP CONSTRAINT Coordinates_Location_fk;
DROP CONSTRAINT Address_Location_fk;
DROP CONSTRAINT Party_Charge_fk;
DROP CONSTRAINT Party_Location_fk;
DROP CONSTRAINT Party_Property_fk;
DROP CONSTRAINT Charge_Discharge_fk;
DROP CONSTRAINT Account_Discharge_fk;
DROP CONSTRAINT Digital_Location_fk;
-- some database providers don't support the ansi-sql DROP CONSTRAINT 
commit;

DROP TABLE Organisation;
DROP TABLE Classification;
DROP TABLE Category;
DROP TABLE Area;
DROP TABLE Type;
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
