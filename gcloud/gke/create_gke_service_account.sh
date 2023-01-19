#! /bin/bash

set -e # stop executing the script if any command fails

while getopts n: flag
do
    case "${flag}" in
        n) sa_name=${OPTARG};;
    esac
done

project_id=$(gcloud config get-value project)

echo "Google Cloud project id: $project_id"

echo "Creating an IAM service account named $sa_name..."

gcloud iam service-accounts create $sa_name --project=$project_id

echo "Granting access to Google Cloud Storage..."

gcloud projects add-iam-policy-binding $project_id \
    --member "serviceAccount:${sa_name}@${project_id}.iam.gserviceaccount.com" \
    --role "roles/storage.admin"

echo "Account $sa_name has been created"

gcloud iam service-accounts list