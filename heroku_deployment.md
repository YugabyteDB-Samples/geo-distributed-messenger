# Application Deployment in Heroku

This instruction shows how to deploy the main Messaging microservice in Heroku and get that service connected with a YugabyteDB Managed cluster.

Note, if you'd like to deploy the complete solution with the Attachments microservice, Kong Gateway, MinIO then follow the [local deployment instruction](local_deployment.md).

<!-- vscode-markdown-toc -->

- [Application Deployment in Heroku](#local-application-deployment)
  - [Prerequisite](#prerequisite)
  - [Architecture](#architecture)
  - [Prepare Heroku Environment](#create-custom-network)

<!-- vscode-markdown-toc-config
    numbering=false
    autoSave=true
    /vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

## Prerequisite

* [Heroku](https://www.heroku.com) account
* [YugabyteDB Managed](http://cloud.yugabyte.com) instance
* Java 17+
* Maven 3.8.4+

## Architecture

The main Messaging microservice implements basic messaging capabilities letting exchange messages and content across messenger's channels. The microservice is deployed in Heroku and stores application data (workspaces, users, channels, messages, etc.) in a YugabyteDB Managed instance.

## Prepare Heroku Environment

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

5. Provide application and database-specific configuration settings to Heroku:
    ```shell
    heroku config:set PORT=<YOUR_SPRING_SERVER_PORT> -a geo-distributed-messenger
    heroku config:set DB_URL="<YOUR_DB_URL>" -a geo-distributed-messenger
    heroku config:set DB_USER=<YOUR_DB_USER> -a geo-distributed-messenger
    heroku config:set DB_PWD=<YOUR_DB_PWD> -a geo-distributed-messenger
    heroku config:set DB_SCHEMA_FILE="classpath:messenger_schema.sql" -a geo-distributed-messenger
    heroku config:set DB_MODE="standard" -a geo-distributed-messenger
    ```

    Note, the `DB_URL` should be in the following format
    ```shell
    jdbc:postgresql://us-east1.9b01e695-51d1-4666-adae-a1e7e13ccfb9.gcp.ybdb.io:5433/yugabyte?ssl=true&sslmode=require
    ```

## Whitelist Heroku IPs

YugabyteDB Managed needs to whitelist your Heroku app on the database end:
1. Install the [Fixie Socks Add-On](https://elements.heroku.com/addons/fixie-socks):
    ```shell
    heroku addons:create fixie-socks:handlebar -a geo-distributed-messenger
    ```

    Replace `handlebar` with your fixie socks package.
    
2. Find your static IP addresses on the Fixie Socks Dashboard:
    ```shell
    heroku addons:open fixie-socks
    ```
3. Add the IPs to YugabyteDB Managed [IP Allow list](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-secure-clusters/add-connections/).

4. Request the app to route all TCP/IP requests through the proxy:
    ```shell
    heroku config:set USE_FIXIE_SOCKS=true -a geo-distributed-messenger
    ```

Check the [How To Connect a Heroku Java App to a Cloud-Native Database](https://dzone.com/articles/how-to-connect-a-heroku-app-to-a-yugabytedb-manage) article for alternate options.

## Deploy Messenger to Heroku

1. Deploy the production build to Heroku:
    ```shell
    heroku deploy:jar target/geo-distributed-messenger-1.0-SNAPSHOT.jar -a geo-distributed-messenger
    ```

2. Check the applicatin logs to confirm the flight is normal:
    ```shell
    heroku logs --tail -a geo-distributed-messenger
    ```

3. Open the app:
    ```shell
    heroku open -a geo-distributed-messenger
    ```

4. Log in under a test user:
    ```shell
    username: test@gmail.com
    pwd: password
    ```

Enjoy and have fun! 

Next, try out the [cloud-native geo-distributed deployment option](gcloud_deployment.md) of the messenger that spans accross countries and continents.

