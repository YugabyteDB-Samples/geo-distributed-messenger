# Local Application Deployment

Follow this instruction if you wish to run the entire application with all the components on your local laptop or on-premise environment.

<!-- vscode-markdown-toc -->

- [Local Application Deployment](#local-application-deployment)
  - [Prerequisite](#prerequisite)
  - [Architecture](#architecture)
  - [Create Custom Network](#create-custom-network)
  - [Start Database](#start-database)
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

* Java 17+
* Maven 3.8.4+
* Docker 20.10.12+

## Architecture

![architecture_local_deployment](https://user-images.githubusercontent.com/1537233/197897660-cc063e29-7f6e-4da2-8754-97548c879cc3.png)

The application logic is shared between two microservices.

The main Messaging microservice implements basic messaging capabilities letting exchange messages and content across messenger's channels. The microservices stores application data (workspaces, users, channels, messages, etc.) in YugabyteDB database. Plus, Kong uses YugabyteDB as a store for its metadata and routes configs.

The second Attachments microservice is responsible for storing using pictures (attachements) in an object storage. MinIO is used as that storage for the local deployment.

The Messaging microservice communicates to the Attachments one via the Kong Gateway. If the user wants to share a picture, the Messaging service triggers a special API endpoint on the Kong end and that endpoint routes the request to the Attachments instance.

## Create Custom Network

YugabyteDB, Kong Gateway and Minio will be running in Docker containers. 

Create a custom network for them:
```shell
docker network create geo-messenger-net
```

## Start Database

You have two choices here. You can use YugabyteDB or PostgreSQL. The application works with both databases with no code changes. The following guide uses YugabyteDB for both the application and Kong-specific data:

Start a multi-node YugabyteDB cluster.

1. Start the cluster:
    ```shell
    mkdir $HOME/yb_docker_data

    docker run -d --name yugabytedb_node1 --net geo-messenger-net \
      -p 15433:15433 -p 7001:7000 -p 9001:9000 -p 5433:5433 \
      -v $HOME/yb_docker_data/node1:/home/yugabyte/yb_data --restart unless-stopped \
      yugabytedb/yugabyte:latest \
      bin/yugabyted start --base_dir=/home/yugabyte/yb_data --daemon=false
      
    docker run -d --name yugabytedb_node2 --net geo-messenger-net \
      -p 15434:15433 -p 7002:7000 -p 9002:9000 -p 5434:5433 \
      -v $HOME/yb_docker_data/node2:/home/yugabyte/yb_data --restart unless-stopped \
      yugabytedb/yugabyte:latest \
      bin/yugabyted start --join=yugabytedb_node1 --base_dir=/home/yugabyte/yb_data --daemon=false
          
    docker run -d --name yugabytedb_node3 --net geo-messenger-net \
      -p 15435:15433 -p 7003:7000 -p 9003:9000 -p 5435:5433 \
      -v $HOME/yb_docker_data/node3:/home/yugabyte/yb_data --restart unless-stopped \
      yugabytedb/yugabyte:latest \
      bin/yugabyted start --join=yugabytedb_node1 --base_dir=/home/yugabyte/yb_data --daemon=false
    ```
2. Confirm the cluster is ready: http://127.0.0.1:15433

## Start Minio

[Minio](https://min.io) is used in local deployments as an object store for pictures that are loaded through the Attachments service. 

1. Start the Minio service in:
    ```shell
    mkdir -p $HOME/minio/data

    docker run -d \
    --net geo-messenger-net \
    -p 9100:9000 \
    -p 9101:9001 \
    --name minio1 \
    -v $HOME/minio/data:/data \
    -e "MINIO_ROOT_USER=minio_user" \
    -e "MINIO_ROOT_PASSWORD=password" \
    quay.io/minio/minio:latest server /data --console-address ":9001"
    ```

2. Open the Minio console and log in using the `minio_user` as the user and `password` as the password:
    http://127.0.0.1:9101

## Configure Kong Gateway

Kong Gateway is used between application microservices for communication purposes. If you'd like to learn more about Kong Gateway deployment in Docker then check this page: https://docs.konghq.com/gateway/latest/install-and-run/docker/

1. Connect to YugabyteDB and create the `kong` database:
    ```shell
    psql -h 127.0.0.1 -p 5433 -U yugabyte

    create database kong;

    \q
    ```

2. Set up the Kong database by applying migrations:
    ```shell
    docker run --rm --net geo-messenger-net \
    -e "KONG_DATABASE=postgres" \
    -e "KONG_PG_HOST=yugabytedb_node1" \
    -e "KONG_PG_PORT=5433" \
    -e "KONG_PG_USER=yugabyte" \
    -e "KONG_PG_PASSWORD=yugabyte" \
    kong:latest kong migrations bootstrap
    ```

It can take up to 5 minutes to complete the bootstrapping process. The container might not display any logs, until the process is finished. Once the boostrapping is completed, you'll see the following log messages:

```shell
....

migrating response-ratelimiting on database 'kong'...
response-ratelimiting migrated up to: 000_base_response_rate_limiting (executed)
migrating session on database 'kong'...
session migrated up to: 000_base_session (executed)
session migrated up to: 001_add_ttl_index (executed)
session migrated up to: 002_320_to_330 (executed)
58 migrations processed
58 executed
Database is up-to-date
``` 


Next, start a Kong Gateway container connecting it to YugabyteDB:
  ```shell
  docker run -d --name kong-gateway \
  --net geo-messenger-net \
  -e "KONG_DATABASE=postgres" \
  -e "KONG_PG_HOST=yugabytedb_node1" \
  -e "KONG_PG_PORT=5433" \
  -e "KONG_PG_USER=yugabyte" \
  -e "KONG_PG_PASSWORD=yugabyte" \
  -e "KONG_PROXY_ACCESS_LOG=/dev/stdout" \
  -e "KONG_ADMIN_ACCESS_LOG=/dev/stdout" \
  -e "KONG_PROXY_ERROR_LOG=/dev/stderr" \
  -e "KONG_ADMIN_ERROR_LOG=/dev/stderr" \
  -e "KONG_ADMIN_LISTEN=0.0.0.0:8001, 0.0.0.0:8444 ssl" \
  -p 8000:8000 \
  -p 8443:8443 \
  -p 127.0.0.1:8001:8001 \
  -p 127.0.0.1:8444:8444 \
  kong:latest
  ```

Make sure the container is up and running:
```shell
curl -i -X GET --url http://localhost:8001/services
```

Finally, open YugabyteDB UI and select "kong" from the "Databases" section:
http://127.0.0.1:15433

You'll see a picture similar to the one below displaying tables and indexes used by Kong internally:
![kong-ui](https://github.com/YugabyteDB-Samples/geo-distributed-messenger/assets/1537233/00108b76-66a9-4457-9442-f7ecc71acb3c)


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

Enjoy and have fun! 

Next, try out the [cloud-native geo-distributed deployment option](gcloud_deployment.md) of the messenger that spans accross countries and continents.

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
