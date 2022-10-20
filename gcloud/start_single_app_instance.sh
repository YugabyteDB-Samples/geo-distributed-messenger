#! /bin/bash

while getopts n:i:r:s:d:a:c:u:p:m: flag
do
    case "${flag}" in
        n) name=${OPTARG};;
        i) project_id=${OPTARG};;
        r) region=${OPTARG};;
        s) subnet=${OPTARG};;
        d) dynamic_configurator=${OPTARG};;
        a) port=${OPTARG};;
        c) url=${OPTARG};;
        u) user=${OPTARG};;
        p) pwd=${OPTARG};;
        m) mode=${OPTARG};;
    esac
done

echo "Starting instance $name in zone $zone..."

gcloud compute instances create $name \
        --project=$project_id \
        --scopes=default,storage-full \
        --machine-type=e2-highcpu-4 \
        --boot-disk-type=pd-balanced --boot-disk-size=10GB \
        --network=default --zone=$zone \
        --image-family=ubuntu-1804-lts --image-project=ubuntu-os-cloud \
        --tags=geo-messenger-instance, \
        --metadata-from-file=startup-script=startup_script.sh, \
        --metadata=PORT=$port,DB_URL=$url,DB_USER=$user,DB_PWD=$pwd,DB_MODE=$mode,REGION=$region,PROJECT_ID=$project_id,ENABLE_RUNTIME_CONFIGURATOR=$dynamic_configurator

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