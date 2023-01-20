#! /bin/bash

set -e # stop executing the script if any command fails

while getopts n: flag
do
    case "${flag}" in
        n) cluster_name=${OPTARG};;
    esac
done

# Starting Kong Gateway in the DB-less mode

echo "Starting Kong Gateway in the $cluster_name cluster"

kubectl config use-context $cluster_name

set +e # continue executing the script if the namespace already exists
kubectl create namespace kong
set -e # stop executing the script if any command fails

# the original config is here - https://bit.ly/kong-ingress-dbless
kubectl apply -f kong_gateway.yaml

echo "Deployed Kong Gateway"

kubectl get pods -n kong
kubectl get services -n kong