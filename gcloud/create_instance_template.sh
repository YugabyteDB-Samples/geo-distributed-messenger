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

echo "Creating instance template $name for zone $zone and subnet $subnet..."

gcloud compute instance-templates create $name \
   --region=$region \
   --network=geo-messenger-network \
   --project=$project_id \
   --subnet=$subnet \
   --scopes=default,storage-full \
   --machine-type=e2-highcpu-4 \
   --boot-disk-type=pd-balanced \
   --boot-disk-size=10GB \
   --image-family=ubuntu-1804-lts \
   --image-project=ubuntu-os-cloud \
   --tags=allow-health-check,allow-ssh,allow-http-my-machines \
   --metadata-from-file=startup-script=startup_script.sh, \
   --metadata=PORT=$port,DB_URL=$url,DB_USER=$user,DB_PWD=$pwd,DB_MODE=$mode,REGION=$region,PROJECT_ID=$project_id,ENABLE_RUNTIME_CONFIGURATOR=$dynamic_configurator

if [ $? -eq 0 ]; then
    echo "Instance template $name has been created!"
else
    echo FAIL
fi