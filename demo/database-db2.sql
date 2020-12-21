
CREATE TABLESPACE cdcdemo;

CREATE SCHEMA cdcdemo;

DROP TABLE cdcdemo.tab0;
DROP TABLE cdcdemo.tab1;
DROP TABLE cdcdemo.tab2;
DROP TABLE cdcdemo.tab3;
DROP TABLE cdcdemo.tab4;
DROP TABLE cdcdemo.tab5;
DROP TABLE cdcdemo.tab6;
DROP TABLE cdcdemo.tab7;
DROP TABLE cdcdemo.tab8;
DROP TABLE cdcdemo.tab9;

CREATE TABLE cdcdemo.tab0 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;
CREATE TABLE cdcdemo.tab1 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;
CREATE TABLE cdcdemo.tab2 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;
CREATE TABLE cdcdemo.tab3 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;
CREATE TABLE cdcdemo.tab4 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;
CREATE TABLE cdcdemo.tab5 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;
CREATE TABLE cdcdemo.tab6 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;
CREATE TABLE cdcdemo.tab7 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;
CREATE TABLE cdcdemo.tab8 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;
CREATE TABLE cdcdemo.tab9 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR(100) NOT NULL,
  C TIMESTAMP NOT NULL
) IN cdcdemo;

CREATE OR REPLACE PROCEDURE cdcdemo.datagen
LANGUAGE  SQL
BEGIN
  DECLARE v_i INTEGER DEFAULT 0;
  DECLARE v_sql VARCHAR(1000);
  exec_loop: LOOP
    SET v_sql = 'INSERT INTO cdcdemo.tab' || v_i;
    SET v_sql = v_sql || ' SELECT row_number() OVER ()';
    SET v_sql = v_sql || ' ,dbms_random.string(''x'', dbms_random.value(10, 50))';
    SET v_sql = v_sql || ' ,CURRENT TIMESTAMP';
    SET v_sql = v_sql || ' FROM (SELECT ''a'' AS x FROM syscat.columns FETCH FIRST 1000 ROWS ONLY) r1,';
    SET v_sql = v_sql || ' (SELECT ''a'' AS y FROM syscat.columns FETCH FIRST 1000 ROWS ONLY) r2';
    EXECUTE IMMEDIATE v_sql;
    COMMIT;
    SET v_i = v_i + 1;
    IF v_i > 9 THEN
        LEAVE exec_loop;
    END IF;
  END LOOP exec_loop;
END
@

SELECT COUNT(*) FROM cdcdemo.tab1;
