#!/bin/sh
bq query --use_legacy_sql=False --dry_run=False 'select table, max(time) as latest_time, count(*) as numOf2, sum(ros) as row from (select * from trading.channel_minutely_rows limit 16) group by table order by table;'

