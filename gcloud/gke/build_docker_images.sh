#! /bin/bash

# The script builds Docker images and deployed to a selected cloud region: https://cloud.google.com/build

set -e # stop executing the script if any command fails

while getopts r: flag
do
    case "${flag}" in
        r) region=${OPTARG};;
    esac
done

echo "Building an Attachments image in $region..."

cd ../../attachments

gcloud builds submit --config cloudbuild.yaml \
    --substitutions _REGION=$region
    
echo "Building a Messenger image in $region..."

cd ../messenger

gcloud builds submit --config cloudbuild.yaml \
    --substitutions _REGION=$region