CREATE TABLESPACE usa_tablespace WITH (
  replica_placement='{"num_replicas": 1, "placement_blocks":
  [{"cloud":"CLOUD","region":"USA","zone":"A","min_num_replicas":1}]}'
);

CREATE TABLESPACE eu_tablespace WITH (
  replica_placement='{"num_replicas": 1, "placement_blocks":
  [{"cloud":"CLOUD","region":"EU","zone":"A","min_num_replicas":1}]}'
);

CREATE TABLESPACE apac_tablespace WITH (
  replica_placement='{"num_replicas": 1, "placement_blocks":
  [{"cloud":"CLOUD","region":"APAC","zone":"A","min_num_replicas":1}]}'
);