SELECT * FROM dba_data_files;

CREATE TABLESPACE cdcdemo DATAFILE '/datum/oradata/cont/cdcdemo01.dbf' SIZE 1G AUTOEXTEND ON NEXT 64M MAXSIZE 16G;

CREATE USER cdcdemo IDENTIFIED BY "passw0rd" DEFAULT TABLESPACE cdcdemo QUOTA UNLIMITED ON cdcdemo;

/*
DROP TABLE cdcdemo.tab0 PURGE;
DROP TABLE cdcdemo.tab1 PURGE;
DROP TABLE cdcdemo.tab2 PURGE;
DROP TABLE cdcdemo.tab3 PURGE;
DROP TABLE cdcdemo.tab4 PURGE;
DROP TABLE cdcdemo.tab5 PURGE;
DROP TABLE cdcdemo.tab6 PURGE;
DROP TABLE cdcdemo.tab7 PURGE;
DROP TABLE cdcdemo.tab8 PURGE;
DROP TABLE cdcdemo.tab9 PURGE;
*/

CREATE TABLE cdcdemo.tab0 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;
CREATE TABLE cdcdemo.tab1 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;
CREATE TABLE cdcdemo.tab2 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;
CREATE TABLE cdcdemo.tab3 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;
CREATE TABLE cdcdemo.tab4 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;
CREATE TABLE cdcdemo.tab5 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;
CREATE TABLE cdcdemo.tab6 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;
CREATE TABLE cdcdemo.tab7 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;
CREATE TABLE cdcdemo.tab8 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;
CREATE TABLE cdcdemo.tab9 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  C DATE NOT NULL
) TABLESPACE cdcdemo;

DECLARE
  v_sql VARCHAR2(1000);
BEGIN
  FOR v_i IN 0..9 LOOP
    v_sql := 'INSERT /*+ APPEND*/ INTO cdcdemo.tab' || v_i;
    v_sql := v_sql || ' SELECT row_number() OVER (ORDER BY x)';
    v_sql := v_sql || ' ,dbms_random.string(''x'', dbms_random.value(10, 50))';
    v_sql := v_sql || ' ,SYSDATE';
    v_sql := v_sql || ' FROM (SELECT ''a'' AS x FROM all_tab_columns WHERE ROWNUM<=1000) r1,';
    v_sql := v_sql || ' (SELECT ''a'' AS y FROM all_tab_columns WHERE ROWNUM<=1000) r2';
    EXECUTE IMMEDIATE v_sql;
    COMMIT;
  END LOOP;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action1 AS
  v_b VARCHAR2(100);
  v_id PLS_INTEGER;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT b INTO v_b FROM cdcdemo.tab0 WHERE a=v_id;
  UPDATE cdcdemo.tab2 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab4 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab6 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab8 SET b=v_b, c=SYSDATE WHERE a=v_id;
  COMMIT;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action2 AS
  v_b VARCHAR2(100);
  v_id PLS_INTEGER;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT b INTO v_b FROM cdcdemo.tab1 WHERE a=v_id;
  UPDATE cdcdemo.tab3 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab5 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab7 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab9 SET b=v_b, c=SYSDATE WHERE a=v_id;
  COMMIT;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action3 AS
  v_b VARCHAR2(100);
  v_id PLS_INTEGER;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT b INTO v_b FROM cdcdemo.tab8 WHERE a=v_id;
  UPDATE cdcdemo.tab0 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab2 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab4 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab6 SET b=v_b, c=SYSDATE WHERE a=v_id;
  COMMIT;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action4 AS
  v_b VARCHAR2(100);
  v_id PLS_INTEGER;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT b INTO v_b FROM cdcdemo.tab9 WHERE a=v_id;
  UPDATE cdcdemo.tab1 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab3 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab5 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab7 SET b=v_b, c=SYSDATE WHERE a=v_id;
  COMMIT;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action5 AS
  v_b VARCHAR2(100);
  v_id PLS_INTEGER;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT b INTO v_b FROM cdcdemo.tab6 WHERE a=v_id;
  UPDATE cdcdemo.tab0 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab1 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab2 SET b=v_b, c=SYSDATE WHERE a=v_id;
  UPDATE cdcdemo.tab3 SET b=v_b, c=SYSDATE WHERE a=v_id;
  COMMIT;
END;
/
