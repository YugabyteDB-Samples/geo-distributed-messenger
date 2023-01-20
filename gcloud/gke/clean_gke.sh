#! /bin/bash

while getopts n:r: flag
do
    case "${flag}" in
        n) cluster_name=${OPTARG};;
        r) region=${OPTARG};;
    esac
done

kubectl config use-context $cluster_name

echo "Cleaning GKE resources..."

kubectl delete services attachments-service --namespace geo-messenger
kubectl delete services messenger-service --namespace geo-messenger

kubectl delete deployments attachments-gke --namespace geo-messenger
kubectl delete deployments messenger-gke --namespace geo-messenger

kubectl delete -f kong_gateway.yaml
kubectl delete namespace kong

gcloud container fleet memberships unregister $cluster_name --gke-cluster $region/$cluster_name
gcloud container clusters delete $cluster_name --region $region
kubectl config delete-context $cluster_name

gcloud container fleet ingress disable