SELECT * FROM dba_data_files;

CREATE TABLESPACE cdcdemo DATAFILE '/datum/oradata/cont/cdcdemo01.dbf' SIZE 1G AUTOEXTEND ON NEXT 64M MAXSIZE 16G;

CREATE USER cdcdemo IDENTIFIED BY "passw0rd" DEFAULT TABLESPACE cdcdemo QUOTA UNLIMITED ON cdcdemo;

/* Cleanup tables:
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
DROP TABLE cdcdemo.oplog PURGE;
*/

CREATE TABLE cdcdemo.tab0 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.tab1 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.tab2 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.tab3 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.tab4 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.tab5 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.tab6 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.tab7 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.tab8 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.tab9 (
  A INTEGER NOT NULL PRIMARY KEY,
  B VARCHAR2(100) NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;

CREATE TABLE cdcdemo.oplog (
  X INTEGER NOT NULL,
  A INTEGER NOT NULL,
  L VARCHAR2(10) NOT NULL,
  C TIMESTAMP NOT NULL
) TABLESPACE cdcdemo;


CREATE OR REPLACE PROCEDURE cdcdemo.datagen AS
  v_sql VARCHAR2(1000);
BEGIN
  FOR v_i IN 0..9 LOOP
    v_sql := 'INSERT /*+ APPEND*/ INTO cdcdemo.tab' || v_i;
    v_sql := v_sql || ' SELECT row_number() OVER (ORDER BY x)';
    v_sql := v_sql || ' ,dbms_random.string(''x'', dbms_random.value(10, 50))';
    v_sql := v_sql || ' ,''*'' ,SYSTIMESTAMP';
    v_sql := v_sql || ' FROM (SELECT ''a'' AS x FROM all_tab_columns WHERE ROWNUM<=1000) r1,';
    v_sql := v_sql || ' (SELECT ''a'' AS y FROM all_tab_columns WHERE ROWNUM<=1000) r2';
    EXECUTE IMMEDIATE v_sql;
    COMMIT;
  END LOOP;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action1 (
  p_label VARCHAR2,
  p_id INTEGER
) AS
  v_b VARCHAR2(100);
  v_t TIMESTAMP;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT 'A1+' || dbms_random.string('x', dbms_random.value(10, 30))
        , SYSTIMESTAMP
    INTO v_b, v_t FROM DUAL;
  UPDATE cdcdemo.tab0 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  UPDATE cdcdemo.tab5 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(0, p_id, p_label, v_t);
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(5, p_id, p_label, v_t);
  COMMIT;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action2 (
  p_label VARCHAR2,
  p_id INTEGER
) AS
  v_b VARCHAR2(100);
  v_t TIMESTAMP;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT 'A1+' || dbms_random.string('x', dbms_random.value(10, 30))
        , SYSTIMESTAMP
    INTO v_b, v_t FROM DUAL;
  UPDATE cdcdemo.tab1 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  UPDATE cdcdemo.tab6 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(1, p_id, p_label, v_t);
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(6, p_id, p_label, v_t);
  COMMIT;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action3 (
  p_label VARCHAR2,
  p_id INTEGER
) AS
  v_b VARCHAR2(100);
  v_t TIMESTAMP;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT 'A1+' || dbms_random.string('x', dbms_random.value(10, 30))
        , SYSTIMESTAMP
    INTO v_b, v_t FROM DUAL;
  UPDATE cdcdemo.tab2 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  UPDATE cdcdemo.tab7 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(2, p_id, p_label, v_t);
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(7, p_id, p_label, v_t);
  COMMIT;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action4 (
  p_label VARCHAR2,
  p_id INTEGER
) AS
  v_b VARCHAR2(100);
  v_t TIMESTAMP;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT 'A1+' || dbms_random.string('x', dbms_random.value(10, 30))
        , SYSTIMESTAMP
    INTO v_b, v_t FROM DUAL;
  UPDATE cdcdemo.tab3 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  UPDATE cdcdemo.tab8 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(3, p_id, p_label, v_t);
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(8, p_id, p_label, v_t);
  COMMIT;
END;
/

CREATE OR REPLACE PROCEDURE cdcdemo.action5 (
  p_label VARCHAR2,
  p_id INTEGER
) AS
  v_b VARCHAR2(100);
  v_t TIMESTAMP;
BEGIN
  v_id := DBMS_RANDOM.VALUE(1, 1000000);
  SELECT 'A1+' || dbms_random.string('x', dbms_random.value(10, 30))
        , SYSTIMESTAMP
    INTO v_b, v_t FROM DUAL;
  UPDATE cdcdemo.tab4 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  UPDATE cdcdemo.tab9 SET b=v_b, l=p_label, c=v_t WHERE a=p_id;
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(4, p_id, p_label, v_t);
  INSERT INTO cdcdemo.oplog(x, a, l, c) VALUES(9, p_id, p_label, v_t);
  COMMIT;
END;
/

CALL cdcdemo.datagen;

ALTER TABLE cdcdemo.tab0 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE cdcdemo.tab1 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE cdcdemo.tab2 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE cdcdemo.tab3 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE cdcdemo.tab4 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE cdcdemo.tab5 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE cdcdemo.tab6 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE cdcdemo.tab7 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE cdcdemo.tab8 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE cdcdemo.tab9 ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;

-- End Of File
