#! /bin/bash

while getopts n:r:s:a:c:u:p: flag
do
    case "${flag}" in
        n) name=${OPTARG};;
        r) region=${OPTARG};;
        s) subnet=${OPTARG};;
        a) port=${OPTARG};;
        c) url=${OPTARG};;
        u) user=${OPTARG};;
        p) pwd=${OPTARG};;
    esac
done

echo "Creating instance template $name for zone $zone and subnet $subnet..."

gcloud compute instance-templates create $name \
   --region=$region \
   --network=geo-messenger-network \
   --project=geo-distributed-messenger \
   --subnet=$subnet \
   --machine-type=e2-small \
   --boot-disk-type=pd-balanced \
   --boot-disk-size=10GB \
   --image-family=ubuntu-1804-lts \
   --image-project=ubuntu-os-cloud \
   --tags=allow-health-check,allow-ssh,allow-http-my-machines \
   --metadata-from-file=startup-script=startup_script.sh, \
   --metadata=PORT=$port,DB_URL=$url,DB_USER=$user,DB_PWD=$pwd

if [ $? -eq 0 ]; then
    echo "Instance template $name has been created!"
else
    echo FAIL
fi