## Local Geo-Partitioned Cluster

### Start the Cluster

0. (Optional) Clean docker resources:
    ```shell
    docker container stop $(docker ps -aq)
    docker container rm -v $(docker ps -aq)
    docker network rm yugabytedb_network
    rm -r ~/yb_docker_data
    ```

1. Start the cluster:
    ```shell
    mkdir ~/yb_docker_data

    docker network create yugabytedb_network

    docker run -d --name yugabytedb_node_usa --net yugabytedb_network -p 7001:7000 -p 9000:9000 -p 5433:5433 \
    -v ~/yb_docker_data/node_usa:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:latest bin/yugabyted start --listen=yugabytedb_node_usa \
    --base_dir=/home/yugabyte/yb_data --daemon=false \
    --master_flags="placement_zone=A,placement_region=USA,placement_cloud=CLOUD" \
    --tserver_flags="placement_zone=A,placement_region=USA,placement_cloud=CLOUD"
    
    docker run -d --name yugabytedb_node_eu --net yugabytedb_network \
    -v ~/yb_docker_data/node_eu:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:latest bin/yugabyted start --join=yugabytedb_node_usa --listen=yugabytedb_node_eu \
    --base_dir=/home/yugabyte/yb_data --daemon=false \
    --master_flags="placement_zone=A,placement_region=EU,placement_cloud=CLOUD" \
    --tserver_flags="placement_zone=A,placement_region=EU,placement_cloud=CLOUD"
        
    docker run -d --name yugabytedb_node_apac --net yugabytedb_network \
    -v ~/yb_docker_data/node_apac:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:latest bin/yugabyted start --join=yugabytedb_node_usa --listen=yugabytedb_node_apac \
    --base_dir=/home/yugabyte/yb_data --daemon=false \
    --master_flags="placement_zone=A,placement_region=APAC,placement_cloud=CLOUD" \
    --tserver_flags="placement_zone=A,placement_region=APAC,placement_cloud=CLOUD"
    ```

2. Modify nodes placement:
    ```shell
    docker exec -i yugabytedb_node_usa \
    yb-admin -master_addresses yugabytedb_node_usa:7100,yugabytedb_node_eu:7100,yugabytedb_node_apac:7100 \
    modify_placement_info CLOUD.USA.A,CLOUD.EU.A,CLOUD.APAC.A 3
    ```

3. Open a psql session:
    ```shell
    psql -h 127.0.0.1 -p 5433 yugabyte -U yugabyte -w
    ```

4. Retreive a list of YugabyteDB servers with their placement info:
    ```sql
    select * from yb_servers();
    ```

### Create Tablespaces and Schema

1. Create tablespaces from the `messenger_local_tablespaces.sql` file.

2. Create the geo-partitioned schema from the `messenger_schema_partitioned.sql` file.

3. Allow updates across multiple geographies:
    ```shell
    SET force_global_transaction = TRUE;
    ```
4. Load data by starting the app.

### Explore the geo-partitioned cluster

* Show that Profiles are stored in partitions of their residency:
    ```sql
    SELECT tableoid::regclass,full_name,email,country_code from Profile;
    ```

* Confirm YugabyteDB queries local data only if requested:
    ```sql
    EXPLAIN ANALYZE SELECT * FROM Workspace WHERE country_code = 'USA';
    ```

* Confirm the above is true for JOINs and other complex queries:

    Find a Channel from Europe:
    ```sql
    SELECT * FROM Channel WHERE country_code = 'DEU' LIMIT 1;;
    ```

    Check how many messages are in the channel:
    ```sql
    SELECT count(*) 
    FROM Channel as c JOIN Message as m ON c.id = m.channel_id
    WHERE c.id=12; 
    ```

    Compare the following three execution plans:
    ```sql
    EXPLAIN ANALYZE SELECT c.name, message, sender_id, c.country_code, m.country_code, sender_country_code 
    FROM Channel as c JOIN Message as m ON c.id = m.channel_id
    WHERE c.id=12; 
    ```

    ```sql
    EXPLAIN ANALYZE SELECT c.name, message, sender_id, c.country_code, m.country_code, sender_country_code 
    FROM Channel as c JOIN Message as m ON c.id = m.channel_id
    WHERE c.country_code = 'DEU' AND c.id=12; 
    ```

    ```sql
    EXPLAIN ANALYZE SELECT c.name, message, sender_id, c.country_code, m.country_code, sender_country_code 
    FROM Channel as c JOIN Message as m ON c.id = m.channel_id
    WHERE c.country_code = 'DEU' AND m.country_code = 'DEU' AND c.id=12; 
    ```

    See what partitions selected Channels and Messages belong to:
    ```sql
    SELECT c.tableoid::regclass, m.tableoid::regclass, c.name, message, sender_id, c.country_code, m.country_code, sender_country_code 
    FROM Channel as c JOIN Message as m ON c.id = m.channel_id
    WHERE c.country_code = 'DEU' AND m.country_code = 'DEU' AND c.id=12;
    ```