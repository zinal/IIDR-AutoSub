alter table cdcdemo.tab4 add column x double default -1;
update cdcdemo.tab4 set x = '4100 + cos(a)';
alter table cdcdemo.tab5 add column y double default -1;
update cdcdemo.tab5 set y = '5100 + cos(a)';
alter table cdcdemo.tab6 add column z double default -1;
update cdcdemo.tab6 set z = '6100 + cos(a)';

alter table cdcdemo.tab4 drop column x;
reorg table cdcdemo.tab4;
alter table cdcdemo.tab5 drop column y;
reorg table cdcdemo.tab5;
alter table cdcdemo.tab6 drop column z;
reorg table cdcdemo.tab6;
