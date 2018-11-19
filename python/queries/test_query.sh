#!/bin/sh
BUCKET_SIZE_SECS=10
SELECT_SINCE="2018-06-24 00:00:00"
SELECT_UNTIL="2018-06-26 00:00:00"
cat test_query.sql | \
bq query \
  --use_legacy_sql=False \
  --dry_run=False \
  --parameter=bucket_size_sec:INT64:$BUCKET_SIZE_SECS \
  --parameter="select_since:TIMESTAMP:$SELECT_SINCE" \
  --parameter="select_until:TIMESTAMP:$SELECT_UNTIL" \
  #--format=json \
  --max_rows=100000
  #--allow_large_results \
  #--destination_table=analysis.receive_delays

