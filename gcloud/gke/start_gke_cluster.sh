#! /bin/bash

# The script starts a GKE cluster in a specified cluster region and registers the cluster with the fleet
# Refer to the following guide for details: https://cloud.google.com/kubernetes-engine/docs/how-to/multi-cluster-ingress-setup

set -e # stop executing the script if any command fails

while getopts r:n:s:a: flag
do
    case "${flag}" in
        r) region=${OPTARG};;
        n) cluster_name=${OPTARG};;
        s) iam_service_account=${OPTARG};;
        a) k8_service_account=${OPTARG};;
    esac
done

project_id=$(gcloud config get-value project)
k8_namespace="geo-messenger"

echo "Google Cloud project id: $project_id"

# Starting a GKE cluster

echo "Starting a GKE cluster in $region named $cluster_name..."

gcloud container clusters create $cluster_name \
    --region=$region \
    --enable-ip-alias \
    --workload-pool=$project_id.svc.id.goog \
    --max-nodes=1 \
    --release-channel=stable \
    --project=$project_id
    
echo "Cluster $cluster_name has been created in $region"
gcloud container clusters list

echo "Available cluster nodes: "
kubectl get nodes

# Configure the cluster credentials

echo "Configuring cluster credentials and permissions..."

# Details on how to configure the workload identity: https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity

gcloud container clusters get-credentials $cluster_name \
    --region=$region \
    --project=$project_id

kubectl config rename-context gke_${project_id}_${region}_${cluster_name} $cluster_name
kubectl config set-context $cluster_name --namespace=$k8_namespace
kubectl config use-context $cluster_name

kubectl apply -f namespace.yaml

kubectl create serviceaccount $k8_service_account --namespace $k8_namespace

gcloud iam service-accounts add-iam-policy-binding ${iam_service_account}@${project_id}.iam.gserviceaccount.com \
    --role roles/iam.workloadIdentityUser \
    --member "serviceAccount:${project_id}.svc.id.goog[$k8_namespace/$k8_service_account]"

kubectl annotate serviceaccount $k8_service_account \
    --namespace $k8_namespace \
    iam.gke.io/gcp-service-account=${iam_service_account}@${project_id}.iam.gserviceaccount.com

echo "Finished configuring cluster credentials"

echo "Registering the cluster to the fleet..."

gcloud container fleet memberships register $cluster_name \
    --gke-cluster $region/$cluster_name \
    --enable-workload-identity \
    --project=$project_id

echo "The cluster $cluster_name has been registered with the fleet"

gcloud container fleet memberships list --project=$project_id