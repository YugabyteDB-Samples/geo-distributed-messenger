# Geo-Distributed Messenger With Vaadin and YugabyteDB

This project is a geo-distributed messenger that is inspired by Slack. The messenger is build on Vaadin, Spring Boot and YugabyteDB. PostgreSQL can be used as an alternate database for single-zone deployments. YugabyteDB is used as a distributed database that can span multiple zones and regions. 


This is an ongoing project, so, expect the source code and description to change significantly over the time. The development journey is documented in the following articles:
* [Geo What? A Quick Introduction to Geo-Distributed Apps](https://dzone.com/articles/geo-what-a-quick-introduction-to-geo-distributed-a)
* [What Makes the Architecture of Geo-Distributed Apps Different?](https://dzone.com/articles/what-makes-the-architecture-of-geo-distributed-app)
* [How to Build a Multi-Zone Java App in Days With Vaadin, YugabyteDB, and Heroku](https://dzone.com/articles/how-to-build-a-multi-zone-java-app-in-days-with-va)
* [How To Connect a Heroku Java App to a Cloud-Native Database](https://dzone.com/articles/how-to-connect-a-heroku-app-to-a-yugabytedb-manage)

Table of Contents:

<!-- vscode-markdown-toc -->

- [Geo-Distributed Messenger With Vaadin and YugabyteDB](#geo-distributed-messenger-with-vaadin-and-yugabytedb)
  - [Start a database](#start-a-database)
    - [Start PostgreSQL](#start-postgresql)
    - [Start YugabyteDB Locally](#start-yugabytedb-locally)
    - [Start YugabyteDB Managed](#start-yugabytedb-managed)
  - [Start Microservices](#start-microservices)
  - [Deploy on Bare Metal](#deploy-on-bare-metal)
  - [Deploy to Heroku](#deploy-to-heroku)
  - [Deploy Across Multiple Google Cloud Regions](#deploy-across-multiple-Google-cloud-regions)
  - [Project structure](#project-structure)

<!-- vscode-markdown-toc-config
    numbering=false
    autoSave=true
    /vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

## Create Custom Network

To allow containers easily discover and communicate to each other:

```shell
docker network create geo-messenger-net
```

## Start a database

First, you need to start a database. The app works with PostgreSQL and YugabyteDB which is a PostgreSQL-compliant database.

### Start PostgreSQL

This might be the easiest option to get started. Especially if you're planning to deploy the app within a single availability zone:

1. Start PostgreSQL in Docker:
    ```shell
    docker run --name postgresql --net geo-messenger-net \
        -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password \
        -p 5432:5432 \
        -v ~/postgresql_data/:/var/lib/postgresql/data -d postgres:13.8
    ```

2. Open a psql connection:
    ```shell
    psql -h 127.0.0.1 --username=postgres
    ```

3. Load the schema from `<project>/scripts/messenger_schema.sql` file

## Start YugabyteDB Locally

This is a more advanced option that allows you to experiment with a multi-node YugabyteDB instance on a personal laptop:

1. Start a three-node cluster in Docker:
    ```shell
    rm -r ~/yb_docker_data
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
2. Make sure all the nodes joined the cluster: http://127.0.0.1:7001

3. Open a psql connection:
    ```shell
    psql -h 127.0.0.1 -p 5433 yugabyte -U yugabyte -w
    ```

3. Load the schema from `<project>/scripts/messenger_schema.sql` file

### Start YugabyteDB Managed

YugabyteDB Managed is suggested for production deployments. Deploy a single-region or multi-region database instance and get the instance managed for you:

1. Follow the quick start guide: https://docs.yugabyte.com/preview/quick-start-yugabytedb-managed/

2. Use the [CloudShell](https://docs.yugabyte.com/preview/quick-start-yugabytedb-managed/#connect-to-your-cluster-using-cloud-shell) to load the schema from `<project>/scripts/messenger_schema.sql` file.

## Configure Kong Gateway

Prepare the Kong database - Postgres or YugabyteDB are used by Kong itself:

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

Refer to this installation guide for details: https://docs.konghq.com/gateway/latest/install-and-run/docker/

## Start Attachments Microservice

1. Navigate to the microservice directory:
    ```shell
    cd attachments 
    ```

2. Start the service:
    ```shell
    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE. Just run the `AttachmentsApplication.java` class.

The service will start listening on `http://localhost:8081/` for incoming requests.

## Create Kong Route for Attachments

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

    (has to be proxied to http://host.docker.internal:8081/ping)
    ```

## Start Messenging Microservice

1. Provide the database connectivity settings in the `application-dev.properties` file (PostgreSQL is used by default):

2. Start the app from command line:
    ```shell
    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE of choice. Just boot the `Application.java` file.

3. Open http://localhost:8080 in your browser (if it's not opened automatically)

4. Log in under a test user:
    ```shell
    username: test@gmail.com
    pwd: password
    ```

## Deploy on Bare Metal

1. Create a production build:
    ```shell
    mvn clean package -Pprod
    ```

2. Validate the build locally passing database-specific settings:
    ```shell
    mvn spring-boot:run -Dspring.profiles.active=prod -Dspring-boot.run.arguments="--PORT=<YOUR_SPRING_SERVER_PORT> --DB_URL=<YOUR_DB_URL> --DB_USER=<YOUR_DB_USER> --DB_PWD=<YOUR_DB_PWD>"
    ```

    Spring will use the `application-prod.properties` file for the production build filling in settings with values provided by you via the `spring-boot.run.arguments` argument.

3. Open http://localhost:{YOUR_SPRING_SERVER_PORT} in your browser (if it's not opened automatically)

4. Log in under a test user:
    ```shell
    username: test@gmail.com
    pwd: password
    ```

5. Take the JAR file and proceed deploying it in your production environment.

## Deploy to Heroku

(The instruction is provided for the Messaging microservice only. You need to deploy the Attachments service and Kong Gateway separately to enable the files uploading feature)

1. Create a production build:
    ```shell
    mvn clean package -Pprod
    ```

2. Log in to your Heroku account:
    ```shell
    heroku login
    ```

3. Install the Heroku Java Plugin:
    ```shell
    heroku plugins:install java
    ```

4. Create a new application in Heroku:
    ```shell
    heroku create geo-distributed-messenger
    ```

5. Provide app and DB-specific configuration settings to Heroku:
    ```shell
    heroku config:set PORT=<YOUR_SPRING_SERVER_PORT> -a geo-distributed-messenger
    heroku config:set DB_URL="<YOUR_DB_URL>" -a geo-distributed-messenger
    heroku config:set DB_USER=<YOUR_DB_USER> -a geo-distributed-messenger
    heroku config:set DB_PWD=<YOUR_DB_PWD> -a geo-distributed-messenger
    ```

    Note, the `DB_URL` should be in the following format
    ```shell
    jdbc:postgresql://us-east1.9b01e695-51d1-4666-adae-a1e7e13ccfb9.gcp.ybdb.io:5433/yugabyte?ssl=true&sslmode=require
    ```

6. (Optional) If you use YugabyteDB Managed then you need to whitelist your Heroku app on the database end:
    * Install the [Fixie Socks Add-On](https://elements.heroku.com/addons/fixie-socks):
        ```shell
        heroku addons:create fixie-socks:handlebar -a geo-distributed-messenger
        ```

        Replace `handlebar` with your fixie socks package.
        
    * Find your static IP addresses on the Fixie Socks Dashboard:
        ```shell
        heroku addons:open fixie-socks
        ```
    * Add the IPs to YugabyteDB Managed [IP Allow list](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-secure-clusters/add-connections/).

    * Request the app to route all TCP/IP requests through the proxy:
        ```shell
        heroku config:set USE_FIXIE_SOCKS=true -a geo-distributed-messenger
        ```

5. Deploy the production build to Heroku:
    ```shell
    heroku deploy:jar target/geo-distributed-messenger-1.0-SNAPSHOT.jar -a geo-distributed-messenger
    ```

6. Check the applicatin logs to confirm the flight is normal:
    ```shell
    heroku logs --tail -a geo-distributed-messenger
    ```
7. Open the app:
    ```shell
    heroku open -a geo-distributed-messenger
    ```

9. Log in under a test user:
    ```shell
    username: test@gmail.com
    pwd: password
    ```

Use the `heroku restart -a geo-distributed-messenger` command if you need to restart the app for any reason.

## Deploy Across Multiple Google Cloud Regions

Follow [these instructions](gcloud/gcloud_deployment.md) to deploy multiple application instances across several cloud regions.

## Project structure

- `MainLayout.java` in `src/main/java` contains the navigation setup (i.e., the
  side/top bar and the main menu). This setup uses
  [App Layout](https://vaadin.com/components/vaadin-app-layout).
- `views` package in `src/main/java` contains the server-side Java views of your application.
- `views` folder in `frontend/` contains the client-side JavaScript views of your application.
- `themes` folder in `frontend/` contains the custom CSS styles.


## Deploying using Docker

To build the Dockerized version of the project, run

```
docker build . -t geo-distributed-messenger:latest
```

Once the Docker image is correctly built, you can test it locally using

```
docker run -p 8080:8080 geo-distributed-messenger:latest
```


## Deploying using Kubernetes

We assume here that you have the Kubernetes cluster from Docker Desktop running (can be enabled in the settings).

First build the Docker image for your application. You then need to make the Docker image available to you cluster. With Docker Desktop Kubernetes, this happens automatically. With Minikube, you can run `eval $(minikube docker-env)` and then build the image to make it available. For other clusters, you need to publish to a Docker repository or check the documentation for the cluster.

The included `kubernetes.yaml` sets up a deployment with 2 pods (server instances) and a load balancer service. You can deploy the application on a Kubernetes cluster using

```
kubectl apply -f kubernetes.yaml
```

If everything works, you can access your application by opening http://localhost:8000/.
If you have something else running on port 8000, you need to change the load balancer port in `kubernetes.yaml`.

Tip: If you want to understand which pod your requests go to, you can add the value of `VaadinServletRequest.getCurrent().getLocalAddr()` somewhere in your UI.
