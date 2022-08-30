CREATE TABLESPACE us_east1_ts WITH (
  replica_placement='{"num_replicas": 1, "placement_blocks":
  [{"cloud":"CLOUD","region":"USA","zone":"A","min_num_replicas":1}]}'
);

CREATE TABLESPACE europe_west3_t WITH (
  replica_placement='{"num_replicas": 1, "placement_blocks":
  [{"cloud":"CLOUD","region":"EU","zone":"A","min_num_replicas":1}]}'
);

CREATE TABLESPACE asia_east1_ts WITH (
  replica_placement='{"num_replicas": 1, "placement_blocks":
  [{"cloud":"CLOUD","region":"APAC","zone":"A","min_num_replicas":1}]}'
);