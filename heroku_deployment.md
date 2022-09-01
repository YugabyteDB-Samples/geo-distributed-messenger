# Application Deployment in Heroku

Follow this instruction to deploy the core Messaging microservice in Heroku. 

If you wish to send the pictures through the Messenger then the Attachements microservice and Kong Gateway need to be deployed as well. This instructions doesn't explain how to do the latter.

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
    
    Check the [How To Connect a Heroku Java App to a Cloud-Native Database](https://dzone.com/articles/how-to-connect-a-heroku-app-to-a-yugabytedb-manage) article for alternate options.

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
