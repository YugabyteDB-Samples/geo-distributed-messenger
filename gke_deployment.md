# Geo-Distributed Deployment in Google Kubernetes Engine

The instruction explains how to deploy the applicaion in Google Kubernetes Engine.

<!-- vscode-markdown-toc -->

- [Geo-Distributed Deployment in Google Kubernetes Engine](#geo-distributed-deployment-in-google-kubernetes-engine)
  - [Prerequisite](#prerequisite)
  - [Architecture](#architecture)
  - [Create Google Cloud Project](#create-google-cloud-project)
  - [Enable Google Cloud Storage](#enable-google-cloud-storage)
  - [Create Artifact Registry Repository](#create-artifact-registry-repository)
  - [Prepare App Images](#prepare-app-images)
  - [Enable Anthos Pricing](#enable-anthos-pricing)
  - [Create Service Account](#create-service-account)
  - [Start GKE Cluster](#start-gke-clusters)
  - [Start Application](#start-application)
  - [Deploy Multi Cluster Ingress](#deploy-multi-cluster-ingress)
  - [Playing with the App](#playing-with-the-app)
  - [Testing Fault Tolerance](#testing-fault-tolerance)
  - [Clean Project](#clean-project)
  
<!-- vscode-markdown-toc-config
    numbering=false
    autoSave=true
    /vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

## Prerequisite 

* [Google Cloud Platform](http://console.cloud.google.com/) account
* [YugabyteDB Managed](http://cloud.yugabyte.com) cluster in Google Cloud Platform

## Architecture 

The application is designed to function across multiple cloud regions. Check the [following article](https://dzone.com/articles/geo-what-a-quick-introduction-to-geo-distributed-a) for details on why this matters.

As the diagram shows, you can deploy multiple instances of the Messaging, Attachments, Kong Gateway services in several Google Kubernetes Engine (GKE) clusters. The clusters can be placed in different distant regions - as `Region A` and `Region B`.

TBD: New Diagram with Kong

![gke_deployment_architecture](https://user-images.githubusercontent.com/1537233/211410498-cf9b5560-7280-4ddd-bee2-0facba81d583.png)

YugabyteDB is deployed in a multi-region mode in the regions of choice. Google Cloud Storage runs across multiple locations as well and used to store pictures that the users share via the messenger.

The users connects to the app using the IP address of the Multi Cluster Ingress. The Ingress relies on the Global Cloud Load Balancer that forwards the user requets to a GKE cluster that is closest to the user.

## Create Google Cloud Project

1. Log in under your account:
    ```shell
    gcloud auth login
    ```

2. Create a new project for the app (use any other project name if `geo-distributed-messenger` is not available):
    ```shell
    gcloud projects create geo-distributed-messenger --name="Geo-Distributed Messenger"
    ```

3. Set this new project as default:
    ```shell
    gcloud config set project geo-distributed-messenger
    ```

4. Open Google Console and enable a billing account for the project: `https://console.cloud.google.com`

5. [Enable](https://console.cloud.google.com/flows/enableapi?apiid=artifactregistry.googleapis.com,cloudbuild.googleapis.com,container.googleapis.com&redirect=https://console.cloud.google.com&_ga=2.220829720.1831599196.1672860095-1629291620.1658249275&_gac=1.192717528.1671329959.CjwKCAiA7vWcBhBUEiwAXieItpcBgXS6j-SP2knNZYtSNXNn5f47EGszdv3UbRLZfbWH8alv4pQ9cxoCSG0QAvD_BwE) Artifact Registry, Cloud Build and Kubernetes Engine APIs.

## Enable Google Cloud Storage

The Attachments microservice uploads pictures to the [Google Cloud Storage](https://cloud.google.com/storage). Enable the service for this Google project.

## Create Artifact Registry Repository

First, you need to create a Docker container for each application microservice and load the container
to [Artifact Registry](https://cloud.google.com/artifact-registry).

1. Create a repository for Docker images in the `us-east4` region:
```shell
gcloud artifacts repositories create geo-distributed-messenger-repo \
    --repository-format=docker \
    --location=us-east4 \
    --description="Docker repository for geo-distributed messenger containers"
```

2. Also, create and store the images in the `europe-west1` region:
    ```shell
    gcloud artifacts repositories create geo-distributed-messenger-repo \
        --repository-format=docker \
        --location=europe-west1 \
        --description="Docker repository for geo-distributed messenger containers"
    ```

## Prepare App Images

Build a Docker image for each application microservice and store the images in selected cloud regions.

1. Navigate to the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Build and submit images to the `us-east4` region:
    ```shell
    ./build_docker_images.sh \
        -r us-east4
    ```

3. Repeate the build process for the `europe-west1` region:
    ```shell
    ./build_docker_images.sh \
        -r europe-west1
    ```

## Enable Anthos Pricing

In the guide below, you'll deploy multiple kubernetes clusters that can be accessed via the Multi Region Ingress. 
The ingress will be managed as part of the [Anthos platform](https://cloud.google.com/anthos).

Enable the Anthos pricing for you project:
```shell
gcloud services enable \
    anthos.googleapis.com \
    multiclusteringress.googleapis.com \
    gkehub.googleapis.com \
    container.googleapis.com \
    multiclusterservicediscovery.googleapis.com
```

## Create Service Account

Create a service account that will be used by Kubernetes workloads.

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Create the service account named `geo-messenger-sa`:
    ```shell
    ./create_gke_service_account.sh -n geo-messenger-sa
    ```

## Start GKE Clusters

Start two GKE clusters in distant cluster locations and register them with the fleet:

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Start the first cluster in the `us-east4` region:
    ```shell
    ./start_gke_cluster.sh \
        -r us-east4 \
        -n gke-us-east4 \
        -s geo-messenger-sa \
        -a geo-messenger-k8-sa
    ```

    the arguments are:
    * `-r` - the name of a cloud region
    * `-n` - the name of the GKE cluster that will be created by the script
    * `-s` - the name of the IAM service account (created earlier)
    * `-a` - the name of the Kubernetes service account (created by the script) 

3. Start the second cluster in the `europe-west1` region:
    ```shell
    ./start_gke_cluster.sh \
        -r europe-west1 \
        -n gke-europe-west1 \
        -s geo-messenger-sa \
        -a geo-messenger-k8-sa
    ```

4. Verify that both clusters are registered with an Anthos fleet:
    ```shell
    gcloud container fleet memberships list
    ```

5. Enable [Multi Cluster Ingress](https://cloud.google.com/kubernetes-engine/docs/concepts/multi-cluster-ingress) and select `gke-us-east4` as the config cluster:
    ```shell
    gcloud container fleet ingress enable \
        --config-membership=gke-us-east4
    ```

Now, you can open the [Anthos](https://cloud.google.com/anthos) dashboard to observe the clusters and Ingress.

## Start Application

Start an instance of Spring Cloud Config Server, Attachments and Messenger in every GKE cluster.

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Create a copy of the `PROJECT_ROO_DIR/messenger/secret-gke-template.yaml` file named `PROJECT_ROO_DIR/messenger/secret-gke.yaml`

3. Open the `PROJECT_ROO_DIR/messenger/secret-gke.yaml` and provide connectivity settings of your YugabyteDB Managed cluster. Don't forget to update the [IP Allow List](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-secure-clusters/add-connections/) on the YugabyteDB Managed side. For development and learning, you can use the range `0.0.0.0/0` to allow connections from any GKE pod.

4. Start the application in the `gke-us-east4` cluster:
    ```shell
    ./start_gke_app.sh \
        -r us-east4 \
        -n gke-us-east4 \
        -a geo-messenger-k8-sa
    ```

    the arguments are:
    * `-r` - the name of the cluster's cloud region
    * `-n` - the cluster name
    * `-a` - the name of the Kubernetes service account

5. Start the app in the `gke-europe-west1` cluster:
    ```shell
    ./start_gke_app.sh \
        -r europe-west1 \
        -n gke-europe-west1 \
        -a geo-messenger-k8-sa
    ```

It will take several minutes to deploy the application. You can monitor the deployment status using the following commands or [GKE Dashboard](https://cloud.google.com/kubernetes-engine).

1. First, select one of the clusters:
    ```shell
    kubectl config use-context gke-us-east4
    # or
    kubectl config use-context gke-europe-west1
    ```

2. Get the deployment status:
    ```shell
    kubectl get deployments

    # Or, view logs of a particular microservice:
    kubectl logs -f deployment/config-server-gke
    kubectl logs -f deployment/attachments-gke
    kubectl logs -f deployment/messenger-gke
    ```

3. Once the deployments are ready, check the pods and services status: 
    ```shell
    kubectl get pods
    kubectl get services
    ```

Lastly, you can connect a Messenger instance directly from any cloud region.

1. First, select one of the clusters:
    ```shell
    kubectl config use-context gke-us-east4
    # or
    kubectl config use-context gke-europe-west1
    ```

2. Find the EXTERNAL_IP of the respective Kubernetes service:
    ```shell
    kubectl get service messenger-service
    ```

3. Open the address in the browser and send a few messeges and pictures:
    ```shell
    http://EXTERNAL_IP/
    ```

    use the `test@gmail.com\password` credentials to log in.

![messenger_view](https://user-images.githubusercontent.com/1537233/211407944-c50ae7af-20d4-4f90-9753-d2379f9290df.png)


## Deploy Multi Cluster Ingress

With the application running across two distant GKE clusters, you can proceed with the [configuration of the Multi Cluster Ingress](https://cloud.google.com/kubernetes-engine/docs/how-to/multi-cluster-ingress) and Service. The Ingress needs to be configured via the config cluster - the `gke-us-east4` one.

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```
2. Deploy multi cluster service and ingress:
    ```shell
    ./deploy_multi_cluster_ingress.sh -n gke-us-east4
    ```

    where `-n` is the name of the config cluster.

3. The multi cluster service creates a derived headless Service (might take several minutes) in every cluster that matches pods with `app: messenger`:
    ```shell
    kubectl get service

    # the name of the service should look as follows
    mci-geo-messenger-mcs-svc-d3tnpay37ltoop2o
    ```

4. Verify the deployment has succeeded:
    ```shell
    kubectl describe mci geo-messenger-ingress | grep VIP
    ```

5. Keep executing the previous command until you see the `VIP:` parameter set to a static IP address like this one below:
    ```shell
    VIP:        34.110.218.170
    ```

6. It can take 10+ minutes for the IP address to be ready for usage. You can see various errors while the IP is being configured. Keep checking the IP readiness using this call:
    ```shell
    curl http://VIP/login

    # once the IP is ready, you'll get an HTML page that starts with:
    <!doctype html><html lang="en"><head><script initial="">window.Vaadin = window.Vaadin || {};window.Vaadin.TypeScript= {};
    ```

Finally, open the VIP address in the browser!
http://VIP/


## Playing With the App

1. Open the app in the browser using the VIP address of the Multi Cluster Ingress.

2. Log in using `test@gmail.com\password` account

3. Send a few messages and pictures in any channel.

4. Go to the GCP "Load Balancing" page and select the load balancer which name starts with `mci-`

5. Open the "Monitoring" tab and confirm the load balancer forwarded app requests to a GKE cluster that is closest to your physical location (in my case, that's `gke-us-east4`).

![us-only-traffic](https://user-images.githubusercontent.com/1537233/211407459-83372922-3cd1-41a2-95ac-819f6c823989.png)

## Testing Fault Tolerance

Next, emulate an outage in the region where the load balancer forwards your requests to. You can do that by stopping the Messenger pod from the region. That pod receives the traffic from the Multi Cluster Ingress the first, thus, if the load balancer can't connect to the pod then it will start routing requests to another cloud region.

1. Select the cluster where your traffic gets redirected by the Multi Cluster Ingress:
    ```shell
    kubectl config use-context gke-us-east4 
    # or
    kubectl config use-context gke-europe-west1
    ```
2. Delete the Messenger's microservice deployment and respective pod:
    ```shell
    kubectl delete deployments messenger-gke -n geo-messenger
    ```

3. Refresh the messenger's tab in the browser (the one that uses the VIP address)

4. You should be asked to aunthenticate again (`test@gmail.com\password` account) because the load balancer started forwarding your requests to another GKE cluster.

5. Send a few messages and pictures in any channel.

6. Go back the GCP "Load Balancing" page and select the load balancer which name starts with `mci-`

7. Open the "Monitoring" tab and confirm the load balancer now forwards your to another cluster region (in my case, that's `gke-europe-west1`).

![europe-traffic](https://user-images.githubusercontent.com/1537233/211407495-9e94bfed-7a33-486d-9d6d-d63049344d3d.png)


Bring back the just deleted K8 deployment of the Messenger service to your original region. The load balancer will start serving your requests back there:

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Start the application in the `gke-us-east4` cluster:
    ```shell
    ./start_gke_app.sh \
        -r us-east4 \
        -n gke-us-east4 \
        -a geo-messenger-k8-sa


    # If originally your traffic was forwarded to the `gke-europe-west1` region, then use this command instead:
    ./start_gke_app.sh \
        -r europe-west1 \
        -n gke-europe-west1 \
        -a geo-messenger-k8-sa
    ```

3. Refresh the browser tab with the messenger app and at some point you'll be redirected to the authentication screen. That means that the Multi Cluster Ingress started forwarding your requests back to the GKE cluster closest to your physical location.

## Clean Project

1. Go to the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Run the script that removes GKE clusters for each used cloud region:
    ```shell
    ./clean_gke.sh \
        -n gke-us-east4 \
        -r us-east4
    
    ./clean_gke.sh \
        -n gke-europe-west1 \
        -r europe-west1
    ```

    the arguments are:
    * `-r` - the name of the cluster's cloud region
    * `-n` - the cluster name
    
3. Use Google Console (or respective gcloud commands) to stop the MCI load balancer (on the "Load Balancing" page), service account and project.

