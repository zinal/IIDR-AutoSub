
CREATE SCHEMA cdcbak;

CREATE TABLE cdcbak.tab0 AS (SELECT * FROM cdcdemo.tab0) WITH DATA IN cdcdemo;
CREATE TABLE cdcbak.tab1 AS (SELECT * FROM cdcdemo.tab1) WITH DATA IN cdcdemo;
CREATE TABLE cdcbak.tab2 AS (SELECT * FROM cdcdemo.tab2) WITH DATA IN cdcdemo;
CREATE TABLE cdcbak.tab3 AS (SELECT * FROM cdcdemo.tab3) WITH DATA IN cdcdemo;
CREATE TABLE cdcbak.tab4 AS (SELECT * FROM cdcdemo.tab4) WITH DATA IN cdcdemo;
CREATE TABLE cdcbak.tab5 AS (SELECT * FROM cdcdemo.tab5) WITH DATA IN cdcdemo;
CREATE TABLE cdcbak.tab6 AS (SELECT * FROM cdcdemo.tab6) WITH DATA IN cdcdemo;
CREATE TABLE cdcbak.tab7 AS (SELECT * FROM cdcdemo.tab7) WITH DATA IN cdcdemo;
CREATE TABLE cdcbak.tab8 AS (SELECT * FROM cdcdemo.tab8) WITH DATA IN cdcdemo;
CREATE TABLE cdcbak.tab9 AS (SELECT * FROM cdcdemo.tab9) WITH DATA IN cdcdemo;


DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab0);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab0;

DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab1);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab1;

DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab2);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab2;

DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab3);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab3;

DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab4);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab4;

DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab5);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab5;

DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab6);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab6;

DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab7);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab7;

DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab8);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab8;

DECLARE c1 CURSOR FOR (SELECT a,b,'*' AS l, c FROM cdcbak.tab9);
LOAD FROM c1 OF CURSOR REPLACE INTO cdcdemo.tab9;

(SELECT 0 AS num, COUNT(*) AS cnt FROM cdcdemo.tab0)
UNION ALL
(SELECT 1 AS num, COUNT(*) AS cnt FROM cdcdemo.tab1)
UNION ALL
(SELECT 2 AS num, COUNT(*) AS cnt FROM cdcdemo.tab2)
UNION ALL
(SELECT 3 AS num, COUNT(*) AS cnt FROM cdcdemo.tab3)
UNION ALL
(SELECT 4 AS num, COUNT(*) AS cnt FROM cdcdemo.tab4)
UNION ALL
(SELECT 5 AS num, COUNT(*) AS cnt FROM cdcdemo.tab5)
UNION ALL
(SELECT 6 AS num, COUNT(*) AS cnt FROM cdcdemo.tab6)
UNION ALL
(SELECT 7 AS num, COUNT(*) AS cnt FROM cdcdemo.tab7)
UNION ALL
(SELECT 8 AS num, COUNT(*) AS cnt FROM cdcdemo.tab8)
UNION ALL
(SELECT 9 AS num, COUNT(*) AS cnt FROM cdcdemo.tab9);
