# Geo-Distributed Messenger

This project is a geo-distributed messenger that is inspired by Slack. The messenger is build on Vaadin, Spring Boot and YugabyteDB. PostgreSQL can be used as an alternate database for single-zone deployments. YugabyteDB is used as a distributed database that can span multiple zones and regions. 

This is an ongoing project, so, expect the source code and description to change significantly over the time. The first version of the app will operate within a single cloud region while the next versions will function across continents.

## Start a database

First, you need to start a database. The app works with PostgreSQL and YugabyteDB which is a PostgreSQL-compliant database.

### Start PostgreSQL

This might be the easiest option to get started. Especially if you're planning to deploy the app within a single availability zone:

1. Start PostgreSQL in Docker:
    ```shell
    docker run --name postgresql -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password \
    -p 5432:5432 -v ~/postgresql_data/:/var/lib/postgresql/data -d postgres
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

    docker network create yugabytedb_network

    docker run -d --name yugabytedb_node1 --net yugabytedb_network \
    -p 7001:7000 -p 9000:9000 -p 5433:5433 \
    -v ~/yb_docker_data/node1:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:latest \
    bin/yugabyted start --listen=yugabytedb_node1 \
    --base_dir=/home/yugabyte/yb_data --daemon=false
    
    docker run -d --name yugabytedb_node2 --net yugabytedb_network \
    -v ~/yb_docker_data/node2:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:latest \
    bin/yugabyted start --join=yugabytedb_node1 --listen=yugabytedb_node2 \
    --base_dir=/home/yugabyte/yb_data --daemon=false
        
    docker run -d --name yugabytedb_node3 --net yugabytedb_network \
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

## Run Application

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

## Deploy to Production

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

6. (Optional) If you use YugabyteDB Managed then you need to whitelist your Heroku app on the database end:
    * Install the [Proximo add-on](https://elements.heroku.com/addons/proximo):
        ```shell
        heroku addons:create proximo:development -a geo-distributed-messenger
        ```
    * Find your static IP address (see [details here](https://devcenter.heroku.com/articles/proximo#determining-your-static-ip-address-1)):
        ```shell
        heroku config -a geo-distributed-messenger | grep PROXIMO_URL
        ```
    * Add the IPs to YugabyteDB Managed [IP Allow list](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-secure-clusters/add-connections/).

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