# Local Application Deployment

Follow this instruction if you wish to run the entire application with all the components on your local machine. 
The instruction is prepared for Unix-based systems. Feel free to submit a pull-request suggesting Windows-specific instructions.

<!-- vscode-markdown-toc -->

- [Local Application Deployment](#local-application-deployment)
  - [Prerequisite](#prerequisite)
  - [Architecture](#architecture)
  - [Create Custom Network](#create-custom-network)
  - [Start Database](#start-database)
    - [YugabyteDB](#yugabyteDB)
    - [PostgreSQL](#postgresql)
  - [Start Minio](#start-minio)
  - [Configure Kong Gateway](#configure-kong-gateway)
  - [Start Attachments Microservice](#start-attachments-microservice)
  - [Create Kong Routes](#create-kong-routes)
  - [Start Messenging Microservice](#start-messenging-microservice)
  - [Clean Resources](#clean-resources)

<!-- vscode-markdown-toc-config
    numbering=false
    autoSave=true
    /vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

## Prerequisite

TBD

## Architecture

TBD

* Database - YugabyteDB or PostgreSQL
* Object Storage - Minio
* Microservices Gateway - Kong Gateway
* Microservices - Messaging and Attachements.

## Create Custom Network

YugabyteDB/PostgreSQL, Kong Gateway and Minio will be running in Docker containers. Create a custom network for them:

```shell
docker network create geo-messenger-net
```

## Start Database

You have two choices here. You can use YugabyteDB or PostgreSQL. The application works with both databases with no code changes. If you select YugabyteDB, then you still need to deploy PostgreSQL that is used by Kong Gateway.

### YugabyteDB

Start a multi-node YugabyteDB cluster.

1. Start the cluster:
    ```shell
    mkdir ~/yb_docker_data

    docker run -d --name yugabytedb_node1 --net geo-messenger-net \
    -p 7001:7000 -p 9000:9000 -p 5433:5433 \
    -v ~/yb_docker_data/node1:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:latest \
    bin/yugabyted start --listen=yugabytedb_node1 \
    --base_dir=/home/yugabyte/yb_data --daemon=false
    
    docker run -d --name yugabytedb_node2 --net geo-messenger-net \
    -v ~/yb_docker_data/node2:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:latest \
    bin/yugabyted start --join=yugabytedb_node1 --listen=yugabytedb_node2 \
    --base_dir=/home/yugabyte/yb_data --daemon=false
        
    docker run -d --name yugabytedb_node3 --net geo-messenger-net \
    -v ~/yb_docker_data/node3:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:latest \
    bin/yugabyted start --join=yugabytedb_node1 --listen=yugabytedb_node3 \
    --base_dir=/home/yugabyte/yb_data --daemon=false
    ```
2. Confirm the cluster is ready: http://127.0.0.1:7001

### PostgreSQL

You need to deploy PostgreSQL for Kong Gateway. The application can use any of the databases.

Start PostgreSQL in Docker:
    ```shell
    mkdir ~/postgresql_data/

    docker run --name postgresql --net geo-messenger-net \
        -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password \
        -p 5432:5432 \
        -v ~/postgresql_data/:/var/lib/postgresql/data -d postgres:13.8
    ```

## Start Minio

[Minio](https://min.io) is used in local deployments as an object store for pictures that are loaded through the Attachments service. 

1. Start the Minio service in:
    ```shell
    mkdir -p ~/minio/data

    docker run -d \
    --net geo-messenger-net \
    -p 9100:9000 \
    -p 9101:9001 \
    --name minio1 \
    -v ~/minio/data:/data \
    -e "MINIO_ROOT_USER=minio_user" \
    -e "MINIO_ROOT_PASSWORD=password" \
    quay.io/minio/minio:RELEASE.2022-08-26T19-53-15Z server /data --console-address ":9001"
    ```

2. Open the Minio console and log in using the `minio_user` as the user and `password` as the password:
    http://127.0.0.1:9101

## Configure Kong Gateway

Kong Gateway is used between application microservices for communication purposes. If you'd like to learn more about Kong Gateway deployment in Docker then check this page: https://docs.konghq.com/gateway/latest/install-and-run/docker/

1. Connect to Postgres and create the `kong` database:
    ```shell
    psql -h 127.0.0.1 --username=postgres

    create database kong;

    \q
    ```

2. Set up the Kong database by applying migrations:
    ```shell
    docker run --rm --net geo-messenger-net \
    -e "KONG_DATABASE=postgres" \
    -e "KONG_PG_HOST=postgresql" \
    -e "KONG_PG_USER=postgres" \
    -e "KONG_PG_PASSWORD=password" \
    kong:2.8.1-alpine kong migrations bootstrap
    ```
3. Start a container with Kong Gateway:
    ```shell
    docker run -d --name kong-gateway \
    --net geo-messenger-net \
    -e "KONG_DATABASE=postgres" \
    -e "KONG_PG_HOST=postgresql" \
    -e "KONG_PG_USER=postgres" \
    -e "KONG_PG_PASSWORD=password" \
    -e "KONG_PROXY_ACCESS_LOG=/dev/stdout" \
    -e "KONG_ADMIN_ACCESS_LOG=/dev/stdout" \
    -e "KONG_PROXY_ERROR_LOG=/dev/stderr" \
    -e "KONG_ADMIN_ERROR_LOG=/dev/stderr" \
    -e "KONG_ADMIN_LISTEN=0.0.0.0:8001, 0.0.0.0:8444 ssl" \
    -p 8000:8000 \
    -p 8443:8443 \
    -p 127.0.0.1:8001:8001 \
    -p 127.0.0.1:8444:8444 \
    kong:2.8.1-alpine
    ```

4. Verify the installation:
    ```shell
    curl -i -X GET --url http://localhost:8001/services
    ```

## Start Attachments Microservice

1. Navigate to the microservice directory:
    ```shell
    cd {project-root-dir}/attachments 
    ```

2. Start the service:
    ```shell
    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE. Just run the `AttachmentsApplication.java` class.

The service will start listening on `http://localhost:8081/` for incoming requests.

## Create Kong Routes

Use Kong Admin API to configure the Kong Service and Route that will lead to the Attachements microservice.

1. Create a Kong Service:
    ```shell
    curl -i -X POST \
        --url http://localhost:8001/services/ \
         --data 'name=attachments-service' \
        --data 'url=http://host.docker.internal:8081'
    ```


    where:
    * `name` is the name of the Service
    * `url` is the address of the Attachment application
    * `host.docker.internal` - an internal Docker DNS name used by containers to connect to the host. Works for Mac OS and Windows. [Extra step needs to be done for Linux](https://stackoverflow.com/questions/24319662/from-inside-of-a-docker-container-how-do-i-connect-to-the-localhost-of-the-mach)

2. Issue the following `POST` requests to add two Routes for the `attachments-service`:
    ```shell
    curl -i -X POST http://localhost:8001/services/attachments-service/routes \
     -d "name=upload-route" \
     -d "paths[]=/upload" \
     -d "strip_path=false"

    curl -i -X POST http://localhost:8001/services/attachments-service/routes \
     -d "name=ping-route" \
     -d "paths[]=/ping" \
     -d "strip_path=false"
    ```
3. Confirm Kong can reach out to the Attachments service:
    ```shell
    curl -i -X GET http://localhost:8000/ping

    (will be proxied to http://host.docker.internal:8081/ping)
    ```

## Start Messenging Microservice

1. Review and update the database connectivity settings in the `{project-root-dir}/messenger/src/main/resources/application-dev.properties` file (YugabyteDB is used by default).

2. Navigate to the microservice directory:
    ```shell
    cd {project-root-dir}/messenger
    ```

3. Start the app from command line:
    ```shell
    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE of choice. Just boot the `Application.java` file.

4. Open http://localhost:8080 in your browser (if it's not opened automatically)

5. Log in under a test user:
    ```shell
    username: test@gmail.com
    pwd: password
    ```
Enjoy!

## Clean Resources

If you're done working with the app, then use these command to remove Docker containers and other resources associated with them:

```shell
docker kill kong-gateway
docker container rm kong-gateway

docker kill minio1
docker container rm minio1

docker kill postgresql
docker container rm postgresql

docker kill yugabytedb_node1
docker container rm yugabytedb_node1

docker kill yugabytedb_node2
docker container rm yugabytedb_node1

docker kill yugabytedb_node2
docker container rm yugabytedb_node1

docker network rm geo-messenger-net

rm -R ~/postgresql_data/
rm -R ~/yb_docker_data
rm -R ~/minio/data

#remove all unused volumes
docker volume prune 
```
