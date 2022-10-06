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

This is an OPTIONAL step. Follow it only if you need to run the Attachments service on your local machine and wish to store pictures in Google Cloud Storage instead of Minio. Otherwise, skip this section!

1. Create the service account:
    ```shell
    gcloud iam service-accounts create google-storage-account

    gcloud projects add-iam-policy-binding geo-distributed-messenger \
        --member="serviceAccount:google-storage-account@geo-distributed-messenger.iam.gserviceaccount.com" \
        --role=roles/storage.admin
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
        --rules=tcp:80,tcp:443
    ```
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

## Create Instance Templates

Use the `gcloud/create_instance_template.sh` script to create instance templates for the US West, Central and East regions:
    ```shell
    ./create_instance_template.sh \
        -n {TEMPLATE_NAME} \
        -i {PROJECT_ID} \
        -r {CLOUD_REGION_NAME} \
        -s {NETWORK_SUBNET_NAME} \
        -a {APP_PORT_NUMBER} \
        -c "{DB_CONNECTION_ENDPOINT}" \
        -u {DB_USER} \
        -p {DB_PWD}
    ```

1. Create a template for the US West region:
    ```shell
    ./create_instance_template.sh \
        -n template-us-west \
        -i geo-distributed-messenger \
        -r us-west2 \
        -s us-west-subnet \
        -a 80 \
        -c "jdbc:postgresql://ADDRESS:5433/yugabyte?ssl=true&sslmode=require" \
        -u {DB_USER} \
        -p {DB_PWD}
    ```

2. Create another template for the US Central region:
    ```shell
    ./create_instance_template.sh \
        -n template-us-central \
        -i geo-distributed-messenger \
        -r us-central1 \
        -s us-central-subnet \
        -a 80 \
        -c "jdbc:postgresql://ADDRESS:5433/yugabyte?ssl=true&sslmode=require" \
        -u {DB_USER} \
        -p {DB_PWD}
    ```

3. And the last template for the US East region:
    ```shell
    ./create_instance_template.sh \
        -n template-us-east \
        -i geo-distributed-messenger \
        -r us-east4 \
        -s us-east-subnet \
        -a 80 \
        -c "jdbc:postgresql://ADDRESS:5433/yugabyte?ssl=true&sslmode=require" \
        -u {DB_USER} \
        -p {DB_PWD}
    ```

## Start Application Instances

1. Start an application instance in every region - US West, Central and East:
    ```shell
    gcloud compute instance-groups managed create ig-us-west \
        --template=template-us-west --size=1 --zone=us-west2-b

    gcloud compute instance-groups managed create ig-us-central \
        --template=template-us-central --size=1 --zone=us-central1-b

    gcloud compute instance-groups managed create ig-us-east \
        --template=template-us-east --size=1 --zone=us-east4-b
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
    gcloud compute instances list --project=geo-distributed-messenger
    ```

## Add Named Ports to Groups

For each instance group, define an HTTP service and map a port name to the relevant port. Once configured, the load balancing service forwards traffic to the named port:

```shell
gcloud compute instance-groups unmanaged set-named-ports ig-us-west \
    --project=geo-distributed-messenger \
    --named-ports http:80 \
    --zone us-west2-b

gcloud compute instance-groups unmanaged set-named-ports ig-us-central \
    --project=geo-distributed-messenger \
    --named-ports http:80 \
    --zone us-central1-b

gcloud compute instance-groups unmanaged set-named-ports ig-us-east \
    --project=geo-distributed-messenger \
    --named-ports http:80 \
    --zone us-east4-b
```

## Configure Global External Load Balancer

Now that the instances are up and running, set up a global load balancer that can direct traffic to those based
on the user location and other rules.

### Reserve external IP addresses

Reserve IP addresses that application users will use to reach the load balancer:
    ```shell
    gcloud compute addresses create load-balancer-ipv4-1 \
        --project=geo-distributed-messenger \
        --ip-version=IPV4 \
        --network-tier=PREMIUM \
        --global
    
    gcloud compute addresses create load-balancer-ipv6-1 \
        --project=geo-distributed-messenger \
        --ip-version=IPV6 \
        --network-tier=PREMIUM \
        --global
    ```

### Configure Health Check, Backend Service and URL Map

1. Create a [health check](https://cloud.google.com/load-balancing/docs/health-checks) for application instances:
    ```shell
    gcloud compute health-checks create http http-basic-check \
        --project=geo-distributed-messenger \
        --check-interval=20s --timeout=5s \
        --healthy-threshold=2 --unhealthy-threshold=2 \
        --request-path=/login \
        --port 80
    ```

2. Create a [backend service](https://cloud.google.com/compute/docs/reference/latest/backendServices) for the geo-messenger instances. The [service is reponsible](https://cloud.google.com/load-balancing/docs/https/setting-up-https#gcloud-and-using-curl) for selecting an application instance (backend) for serving a particular user request:
    ```shell
    gcloud compute backend-services create geo-messenger-backend-service \
        --project=geo-distributed-messenger \
        --load-balancing-scheme=EXTERNAL \
        --protocol=HTTP \
        --port-name=http \
        --health-checks=http-basic-check \
        --global
    ```
3. Add your instance groups as backends to the backend services:
    ```shell
    gcloud compute backend-services add-backend geo-messenger-backend-service \
        --project=geo-distributed-messenger \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-us-central \
        --instance-group-zone=us-central1-b \
        --global
    
    gcloud compute backend-services add-backend geo-messenger-backend-service \
        --project=geo-distributed-messenger \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-us-east \
        --instance-group-zone=us-east4-b \
        --global
    
    gcloud compute backend-services add-backend geo-messenger-backend-service \
        --project=geo-distributed-messenger \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-us-west \
        --instance-group-zone=us-west2-b \
        --global
    ```
4. Create a default URL map to route all the incoming requests to the geo messenger backend service (in practice, you can define backend services and URL maps for different microservices):
    ```shell
    gcloud compute url-maps create web-map-http \
        --project=geo-distributed-messenger \
        --default-service geo-messenger-backend-service
    ```
### Configure HTTP Proxy

<!-- External traffic goes to the load balancer (and the to the backend services and backends) through the HTTPS proxy.

1. Create a self-signed private key and certificate (for testing only):
    ```shell
    openssl genrsa -out https_proxy_private_key.pem 2048

    openssl req -new -key https_proxy_private_key.pem \
        -out https_proxy_csr.pem \
        -config cert_config    
    
    openssl x509 -req \
        -signkey https_proxy_private_key.pem \
        -in https_proxy_csr.pem \
        -out https_proxy_cert.pem \
        -extfile cert_config \
        -extensions extension_requirements \
        -days 365
    ```

2. Share the certificate with Google Compute Engine:
    ```shell
    gcloud compute ssl-certificates create geo-messenger-ssl-cert \
        --project=geo-distributed-messenger \
        --certificate https_proxy_cert.pem \
        --private-key https_proxy_private_key.pem
    ``` -->

<!-- 3. Create a target HTTPS proxy to route requests to the URL map:
    ```shell
    gcloud compute target-https-proxies create https-load-balancer-proxy \
        --project=geo-distributed-messenger \
        --url-map web-map --ssl-certificates geo-messenger-ssl-cert
    ``` -->

1. Create a target HTTP proxy to route user requests to the URL map:
    ```shell
    gcloud compute target-http-proxies create http-load-balancer-proxy \
        --project=geo-distributed-messenger \
        --url-map web-map-http \
        --global
    ```
2. Create two global forwarding rules to route incoming requests to the proxy, one for each of the IP addresses you created:
    ```shell
    gcloud compute forwarding-rules create http-content-rule \
        --project=geo-distributed-messenger \
        --load-balancing-scheme=EXTERNAL \
        --network-tier=PREMIUM \
        --address=load-balancer-ipv4-1  \
        --global \
        --target-http-proxy=http-load-balancer-proxy \
        --ports=80
    
    gcloud compute forwarding-rules create http-content-ipv6-rule \
        --project=geo-distributed-messenger \
        --load-balancing-scheme=EXTERNAL_MANAGED \
        --network-tier=PREMIUM \
        --address=load-balancer-ipv6-1  \
        --global \
        --target-http-proxy=http-load-balancer-proxy \
        --ports=80
    ```

After creating the global forwarding rule, it can take several minutes for your configuration to propagate worldwide.

## Test Load Balancer

1. Record the IP addresses of the load balancer:
    ```shell
    gcloud compute addresses describe load-balancer-ipv4-1 \
        --project=geo-distributed-messenger \
        --format="get(address)" \
        --global
    
    gcloud compute addresses describe load-balancer-ipv6-1 \
        --project=geo-distributed-messenger \
        --format="get(address)" \
        --global
    ```

2. Send a request through the load balancer:
    ```shell
    curl -k http://34.160.49.56
    ```

## Clear Resources

