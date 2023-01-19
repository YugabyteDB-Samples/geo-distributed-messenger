#! /bin/bash

set -e # stop executing the script if any command fails

while getopts n: flag
do
    case "${flag}" in
        n) cluster_name=${OPTARG};;
    esac
done

echo "Deploying Multi Cluster Service via the $cluster_name config cluster..."

kubectl config use-context $cluster_name

kubectl apply -f multi-cluster-ingress-healthcheck.yaml

kubectl apply -f multi-cluster-service.yaml

echo "The Multi Cluster Service is started"

kubectl get mcs --namespace geo-messenger

echo "Starting Multi Cluster Ingress..."

kubectl apply -f multi-cluster-ingress.yaml

echo "The Ingress is deployed"

kubectl describe mci geo-messenger-ingress --namespace geo-messenger
