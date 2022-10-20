# Application Deployment in Google Cloud

You can deploy multiple application instances across several geographies in Google Cloud with the `gcloud` tool. 
Follow this guide to create a custom project, provision infrastructure and start an app on one or multiple VMs.

Refer to the following Google Cloud doc for more details:
https://cloud.google.com/load-balancing/docs/https/setting-up-https

## Create Project

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

4. Set this new project as default:
    ```shell
    gcloud config set project geo-distributed-messenger
    ```

5. Open Google Console and enable a billing account for the project: `https://console.cloud.google.com`

## Create Service Account 

This is an OPTIONAL step. Follow the steps below only if you need to run the Attachments service on your local machine and wish to store pictures in Google Cloud Storage. Otherwise, skip this section!

1. Create the service account:
    ```shell
    gcloud iam service-accounts create google-storage-account

    gcloud projects add-iam-policy-binding geo-distributed-messenger \
        --member="serviceAccount:google-storage-account@geo-distributed-messenger.iam.gserviceaccount.com" \
        --role=roles/storage.admin

    gcloud projects add-iam-policy-binding geo-distributed-messenger \
        --member="serviceAccount:google-storage-account@geo-distributed-messenger.iam.gserviceaccount.com" \
        --role=roles/viewer
    ```
2. Generate the key:
    ```shell
    cd {project_dir}/glcoud

    gcloud iam service-accounts keys create google-storage-account-key.json \
        --iam-account=google-storage-account@geo-distributed-messenger.iam.gserviceaccount.com
    ```
3. Add a special environment variable. The attachments service will use it while working with the Cloud Storage SDK:
    ```shell
    echo 'export GOOGLE_APPLICATION_CREDENTIALS={absolute_path_to_the_key}/google-storage-account-key.json' >> ~/.bashrc 

    echo 'export GOOGLE_APPLICATION_CREDENTIALS={absolute_path_to_the_key}/google-storage-account-key.json' >> ~/.zshrc
    ```

## Create Custom Network

1. Create the custom VPC network:
    ```shell
    gcloud compute networks create geo-messenger-network \
        --subnet-mode=custom
    ```

2. Create subnets in 3 regions of the USA:
    ```shell
    gcloud compute networks subnets create us-central-subnet \
        --network=geo-messenger-network \
        --range=10.1.10.0/24 \
        --region=us-central1
    
    gcloud compute networks subnets create us-west-subnet \
        --network=geo-messenger-network \
        --range=10.1.11.0/24 \
        --region=us-west2

    gcloud compute networks subnets create us-east-subnet \
        --network=geo-messenger-network \
        --range=10.1.12.0/24 \
        --region=us-east4
    ```
3. Create subnets in Asia and Europe:
    ```shell
    gcloud compute networks subnets create europe-west-subnet \
        --network=geo-messenger-network \
        --range=10.2.10.0/24 \
        --region=europe-west3

    gcloud compute networks subnets create asia-east-subnet \
        --network=geo-messenger-network \
        --range=10.3.10.0/24 \
        --region=asia-east1
    ```

3. Create a firewall rule to allow SSH connectivity to VMs within the VPC:
    ```shell
    gcloud compute firewall-rules create allow-ssh \
        --network=geo-messenger-network \
        --action=allow \
        --direction=INGRESS \
        --rules=tcp:22 \
        --target-tags=allow-ssh
    ```
    Note, allow to turn on the `compute.googleapis.com` persmission if requested.

4. Create the healthcheck rule to allow the global load balancer and Google Cloud health checks to communicate with backend instances on port `80` and `443`:
    ```shell
    gcloud compute firewall-rules create allow-health-check-and-proxy \
        --network=geo-messenger-network \
        --action=allow \
        --direction=ingress \
        --target-tags=allow-health-check \
        --source-ranges=130.211.0.0/22,35.191.0.0/16 \
        --rules=tcp:80
    ```
5. (Optional) for dev and testing purpose only, add IPs of your personal laptop and other machines that need to communicate to the backend on port `80` (note, you need to replace `0.0.0.0/0` with your IP):
    ```shell
    gcloud compute firewall-rules create allow-http-my-machines \
        --network=geo-messenger-network \
        --action=allow \
        --direction=ingress \
        --target-tags=allow-http-my-machines \
        --source-ranges=0.0.0.0/0 \
        --rules=tcp:80
    ```

## Create Runtime Configurator

This step is optional if you don't plan to change database connectivity settings in runtime. By default, the database settings are provided in the `application.properties` file along with other properties. The Runtime Configurator is useful when you need an instance of Messaging microservice to connect to a specific database deployment or node from its region.

### Create Config

1. Enable [Runtime Configurator APIs](https://cloud.google.com/deployment-manager/runtime-configurator)

2. Create a `RuntimeConfig` for the Messaging microservice:
    ```shell
    gcloud beta runtime-config configs create messaging-microservice-settings
    ```

### Specify Database Configuration Settings

An instance of the Messaging microservice subscribes for updates on the following configuration variables:
    ```shell
    {REGION}/spring.datasource.url
    {REGION}/spring.datasource.username
    {REGION}/spring.datasource.password
    {REGION}/yugabytedb.connection.type
    ```
    where:
    * `{REGION}` is the region the VM was started in. You provide the region name via the `-r` parameter of the `./create_instance_template.sh` script.
    * `yugabytedb.connection.type` - can be set to `standard`, `replica` or `geo`. Refer to the section below for details.

Once an instance of the microservice is started, you can use the [Runtime Configurator APIs](https://cloud.google.com/deployment-manager/runtime-configurator/set-and-get-variables) to set and update those variable.

As an example, this is how to update the database connectivity settings for all the VMs started in the `us-west2` region:
    ```shell
    gcloud beta runtime-config configs variables set us-west2/spring.datasource.username \
      {NEW_DATABASE_USERNAME} --config-name messaging-microservice-settings --is-text
    gcloud beta runtime-config configs variables set us-west2/spring.datasource.password \
      {NEW_DATABASE_PASSWORD} --config-name messaging-microservice-settings --is-text
    gcloud beta runtime-config configs variables set us-west2/yugabytedb.connection.type standard \
     --config-name messaging-microservice-settings --is-text

    gcloud beta runtime-config configs variables set us-west2/spring.datasource.url \
      {NEW_DATABASE_URL} --config-name messaging-microservice-settings --is-text
    ```
    Note, the `spring.datasource.url` parameter MUST be updated the last because the application logic watches for its changes.

## Create Instance Templates

Use the `gcloud/create_instance_template.sh` script to create instance templates for the US West, Central and East regions:
    ```shell
    ./create_instance_template.sh \
        -n {INSTANCE_TEMPLATE_NAME} \
        -i {PROJECT_ID} \
        -r {CLOUD_REGION_NAME} \
        -s {NETWORK_SUBNET_NAME} \
        -d {ENABLE_DYNAMIC_RUNTIME_CONFIGURATOR}
        -a {APP_PORT_NUMBER} \
        -c "{DB_CONNECTION_ENDPOINT}" \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m {DB_MODE}
    ```
    where `DB_MODE` can be set to one of these values:
    * 'standard' - the data source is connected to a standard/regular node. 
    * 'replica' - the connection goes via a replica node.
    * 'geo' - the data source is connected to a geo-partitioned cluster.


1. Create a template for the US West, Central and East regions:
    ```shell
    ./create_instance_template.sh \
        -n template-us-west \
        -i geo-distributed-messenger \
        -r us-west2 \
        -s us-west-subnet \
        -d true \
        -a 80 \
        -c "jdbc:postgresql://ADDRESS:5433/yugabyte?ssl=true&sslmode=require" \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard

    ./create_instance_template.sh \
        -n template-us-central \
        -i geo-distributed-messenger \
        -r us-central1 \
        -s us-central-subnet \
        -d true \
        -a 80 \
        -c "jdbc:postgresql://ADDRESS:5433/yugabyte?ssl=true&sslmode=require" \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard

    ./create_instance_template.sh \
        -n template-us-east \
        -i geo-distributed-messenger \
        -r us-east4 \
        -s us-east-subnet \
        -d true \
        -a 80 \
        -c "jdbc:postgresql://ADDRESS:5433/yugabyte?ssl=true&sslmode=require" \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard
    ```
2. Create a template for Europe:
    ```shell
    ./create_instance_template.sh \
        -n template-europe-west \
        -i geo-distributed-messenger \
        -r europe-west3 \
        -s europe-west-subnet \
        -d true \
        -a 80 \
        -c "jdbc:postgresql://ADDRESS:5433/yugabyte?ssl=true&sslmode=require" \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard
    ```  
3. Create a template for Asia:
    ```shell
    ./create_instance_template.sh \
        -n template-asia-east \
        -i geo-distributed-messenger \
        -r asia-east1 \
        -s asia-east-subnet \
        -d true \
        -a 80 \
        -c "jdbc:postgresql://ADDRESS:5433/yugabyte?ssl=true&sslmode=require" \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard
    ```  
## Start Application Instances

1. Start an application instance in every region:
    ```shell
    gcloud compute instance-groups managed create ig-us-west \
        --template=template-us-west --size=1 --zone=us-west2-b

    gcloud compute instance-groups managed create ig-us-central \
        --template=template-us-central --size=1 --zone=us-central1-b

    gcloud compute instance-groups managed create ig-us-east \
        --template=template-us-east --size=1 --zone=us-east4-b

    gcloud compute instance-groups managed create ig-europe-west \
        --template=template-europe-west --size=1 --zone=europe-west3-b

    gcloud compute instance-groups managed create ig-asia-east \
        --template=template-asia-east --size=1 --zone=asia-east1-b
    ```

2. (YugabyteDB Managed specific) Add VMs external IP to the [IP Allow list](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-secure-clusters/add-connections/#assign-an-ip-allow-list-to-a-cluster).

3. Open Google Cloud Logging and wait while the VM finishes executing the `startup_script.sh` that sets up the environment and start an application instance. It can take between 5-10 minutes.

    Alternatively, check the status from the terminal:
    ```shell
    #find an instance name
    gcloud compute instances list --project=geo-distributed-messenger

    #connect to the instance
    gcloud compute ssh {INSTANCE_NAME} --project=geo-distributed-messenger

    sudo journalctl -u google-startup-scripts.service -f
    ```

4. Open the app by connecting to `http://{INSTANCE_EXTERNAL_IP}`. Use `test@gmail.com` and `password` as testing credentials.
    Note, you can find the external address by running this command:
    ```shell
    gcloud compute instances list
    ```

## Configure Global External Load Balancer

Now that the instances are up and running, configure a global load balancer that will forward user requests to the nearest instance.

### Add Named Ports to Instance Groups

Set named ports for every instance group letting the load balancers know that the instances are capable of processing the HTTP requests on port `80`:

```shell
gcloud compute instance-groups unmanaged set-named-ports ig-us-west \
    --named-ports http:80 \
    --zone us-west2-b

gcloud compute instance-groups unmanaged set-named-ports ig-us-central \
    --named-ports http:80 \
    --zone us-central1-b

gcloud compute instance-groups unmanaged set-named-ports ig-us-east \
    --named-ports http:80 \
    --zone us-east4-b
```

### Reserve external IP addresses

Reserve IP addresses that application users will use to reach the load balancer:
    ```shell
    gcloud compute addresses create load-balancer-public-ip \
        --ip-version=IPV4 \
        --network-tier=PREMIUM \
        --global
    ```

### Configure Backend Service

1. Create a [health check](https://cloud.google.com/load-balancing/docs/health-checks) for application instances:
    ```shell
    gcloud compute health-checks create http load-balancer-http-basic-check \
        --check-interval=20s --timeout=5s \
        --healthy-threshold=2 --unhealthy-threshold=2 \
        --request-path=/login \
        --port 80
    ```

2. Create a [backend service](https://cloud.google.com/compute/docs/reference/latest/backendServices) that selects a VM instance for serving a particular user request:
    ```shell
    gcloud compute backend-services create load-balancer-backend-service \
        --load-balancing-scheme=EXTERNAL_MANAGED \
        --protocol=HTTP \
        --port-name=http \
        --health-checks=load-balancer-http-basic-check \
        --global
    ```
3. Add your instance groups as backends to the backend services:
    ```shell
    gcloud compute backend-services add-backend load-balancer-backend-service \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-us-central \
        --instance-group-zone=us-central1-b \
        --global
    
    gcloud compute backend-services add-backend load-balancer-backend-service \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-us-east \
        --instance-group-zone=us-east4-b \
        --global
    
    gcloud compute backend-services add-backend load-balancer-backend-service \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-us-west \
        --instance-group-zone=us-west2-b \
        --global
    ```
4. Create a default URL map to route all the incoming requests to the created backend service (in practice, you can define backend services and URL maps for different microservices):
    ```shell
    gcloud compute url-maps create load-balancer-url-map --default-service load-balancer-backend-service
    ```

### Configure Frontend

Create a user-facing frontend (aka. HTTP(s) proxy) that receives requests and forwards them to the backend service:

1. Create a target HTTP proxy to route user requests to the backend's URL map:
    ```shell
    gcloud compute target-http-proxies create load-balancer-http-frontend \
        --url-map load-balancer-url-map \
        --global
    ```
2. Create a global forwarding rule to route incoming requests to the proxy:
    ```shell
    gcloud compute forwarding-rules create load-balancer-http-frontend-forwarding-rule \
        --load-balancing-scheme=EXTERNAL_MANAGED \
        --network-tier=PREMIUM \
        --address=load-balancer-public-ip  \
        --global \
        --target-http-proxy=load-balancer-http-frontend \
        --ports=80
    ```

After creating the global forwarding rule, it can take several minutes for your configuration to propagate worldwide.

## Test Load Balancer

1. Find the public IP addresses of the load balancer:
    ```shell
    gcloud compute addresses describe load-balancer-public-ip \
        --format="get(address)" \
        --global
    ```

2. Send a request through the load balancer:
    ```shell
    curl -v http://{LOAD_BALANCER_PUBLIC_IP}
    ```

    Note, it can take several minutes before the load balancer's settings get propogated globally. Until this happens, the `curl` command might hit different HTTP errors.

## Clear Resources

