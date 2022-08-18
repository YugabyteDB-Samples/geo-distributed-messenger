# Deploying App Instances With GCloud

You can deploy multiple application instances across several geographies in Google Cloud with the `gcloud` tool. 
Follow this guide to create a custom project, provision infrastructure and start an app on one or multiple VMs.

## Prepare JAR executable

1. Build the package:
    ```shell
    mvn clean package -Pprod
    ```
2. Upload to the repo:
    ```shell
    mv 
    ```

## Create Project and Network

1. Navigate to the `gcloud` directory within the project structure:
    ```shell
    cd gcloud
    ```

2. Log in under your account:
    ```shell
    gcloud auth login
    ```

3. Create a new project for the app (use any other project name if `geo-distributed-messenger` is not available):
    ```shell
    gcloud projects create geo-distributed-messenger --name="Geo-Distributed Messenger"
    ```

4. Open Google Console and enable a billing account for the project: `https://console.cloud.google.com`

5. Create a firewall rule to allow ingress traffic for HTTP, HTTP(S) and SSH:
    ```shell
    gcloud compute --project=geo-distributed-messenger firewall-rules create geo-messenger-allowed-traffic \
        --direction=INGRESS --priority=1000 --network=default \
        --action=ALLOW --rules=tcp:22,tcp:80,tcp:443 \
        --source-ranges=0.0.0.0/0 --target-tags=geo-messenger-instance
    ```
    Note, allow to enable the `compute.googleapis.com` if asked by the preeceding command.

## Create First VM

1. Create an instance and run the app in a target cloud region:
    ```shell
    ./start_single_app_instance.sh \
        -n {INSTANCE_NAME} \
        -z {CLOUD_ZONE_NAME} \
        -a {APP_PORT_NUMBER} \
        -c "{DB_CONNECTION_ENDPOINT}" \
        -u {DB_USER} \
        -p {DB_PWD}
    ```

    For instance, the command below starts the app in the US West region:
    ```shell
    ./start_single_app_instance.sh \
        -n messenger-us-west-instance \
        -z us-west2-a \
        -a 80 \
        -c "jdbc:postgresql://us-east1.9b01e695-51d1-4666-adae-a1e7e13ccfb9.gcp.ybdb.io:5433/yugabyte?ssl=true&sslmode=require" \
        -u admin \
        -p super-complex-password
    ```

2. (YugabyteDB Managed specific) Add VMs external IP to the [IP Allow list](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-secure-clusters/add-connections/#assign-an-ip-allow-list-to-a-cluster).

3. Open Google Cloud Logging and wait while the VM finishes executing the `startup_script.sh` that sets up the environment and start an application instance. It can take between 5-10 minutes.

or log in to the machine and check the status this way:
```shell
gcloud compute ssh messenger-us-west-instance --project=geo-distributed-messenger

sudo journalctl -u google-startup-scripts.service -f
```

## Deploy More Instances

TBD - first, released the current version by preloading a JAR to GitHub (under the releases?). It takes too much time to build the file from sources.