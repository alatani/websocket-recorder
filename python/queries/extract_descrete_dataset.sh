#!/bin/sh
BUCKET_SIZE_SECS=5
SELECT_SINCE="2018-06-01 00:00:00"
SELECT_UNTIL="2018-07-01 00:00:00"
#cat test_query.sql | \
cat extract_descrete_dataset.sql | \
bq query \
  --use_legacy_sql=False \
  --dry_run=False \
  --parameter=bucket_size_sec:INT64:$BUCKET_SIZE_SECS \
  --parameter="select_since:TIMESTAMP:$SELECT_SINCE" \
  --parameter="select_until:TIMESTAMP:$SELECT_UNTIL" \
  --allow_large_results \
  --destination_table=analysis.bucket_features_5secs_180601_180630

