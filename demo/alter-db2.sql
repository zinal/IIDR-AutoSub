
ALTER TABLE cdcdemo.tab4 ADD COLUMN x INTEGER DEFAULT -1 NOT NULL;

UPDATE cdcdemo.tab4 SET x = MOD(a, 100);

ALTER TABLE cdcdemo.tab5 ADD COLUMN y VARCHAR(20) NULL;

UPDATE cdcdemo.tab5 SET y = SUBSTR(b,1,20);

ALTER TABLE cdcdemo.tab6 ADD COLUMN z DOUBLE DEFAULT -10.0 NOT NULL;

UPDATE cdcdemo.tab6 SET z = COS(a);
