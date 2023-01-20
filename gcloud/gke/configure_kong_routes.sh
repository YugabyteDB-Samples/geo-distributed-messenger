#! /bin/bash

set -e # stop executing the script if any command fails

while getopts n:s: flag
do
    case "${flag}" in
        n) cluster_name=${OPTARG};;
        s) kong_proxy_ip=${OPTARG};;
    esac
done

echo "Configuring Kong Gateway services and routes..."

curl -i -X POST \
    --url http://${kong_proxy_ip}:8001/config \
        -F config=@kong_routes.yaml

echo "Configured the routes"

curl -i -X GET --url http://${kong_proxy_ip}:8001/services/attachments-service/routes