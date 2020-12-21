
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
