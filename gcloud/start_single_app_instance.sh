#! /bin/bash

while getopts n:i:z:a:c:u:p: flag
do
    case "${flag}" in
        n) name=${OPTARG};;
        i) project_id=${OPTARG};;
        z) zone=${OPTARG};;
        a) port=${OPTARG};;
        c) url=${OPTARG};;
        u) user=${OPTARG};;
        p) pwd=${OPTARG};;
    esac
done

echo "Starting instance $name in zone $zone..."

gcloud compute instances create $name \
        --project=$project_id \
        --service-account=google-storage-account@geo-distributed-messenger.iam.gserviceaccount.com \
        --machine-type=e2-highcpu-4 \
        --boot-disk-type=pd-balanced --boot-disk-size=10GB \
        --network=default --zone=$zone \
        --image-family=ubuntu-1804-lts --image-project=ubuntu-os-cloud \
        --tags=geo-messenger-instance, \
        --metadata-from-file=startup-script=startup_script.sh, \
        --metadata=PORT=$port,DB_URL=$url,DB_USER=$user,DB_PWD=$pwd,GOOGLE_STORAGE_PROJECT_ID=$project_id

if [ $? -eq 0 ]; then
    echo "Instance $name has been created!"
    echo "The will be started on port $port and connect to the database $url"
    echo "Use command below to check the progress: "
    echo "  "
    echo "  gcloud compute ssh $name --project=geo-distributed-messenger"
    echo "  sudo journalctl -u google-startup-scripts.service -f"
else
    echo FAIL
fi