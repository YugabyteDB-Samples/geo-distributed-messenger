#! /bin/bash

if [ ! -f "/etc/initialized_on_startup" ]; then
    echo "Launching the VM for the first time."

    sudo apt-get update
    sudo apt-get --yes --force-yes install zip unzip

    export SDKMAN_DIR="/usr/local/sdkman" && curl -s "https://get.sdkman.io" | bash
    source "/usr/local/sdkman/bin/sdkman-init.sh" 
    sdk install java 17.0.4-zulu
    sdk use java 17.0.4-zulu

    wget https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.zip
    sudo mkdir /etc/apache-maven
    sudo unzip apache-maven-3.8.6-bin.zip -d /etc/apache-maven
    export PATH=/etc/apache-maven/apache-maven-3.8.6/bin:$PATH
    rm apache-maven-3.8.6-bin.zip

    sudo mkdir /opt/messenger
    sudo chmod -R 777 /opt/messenger 
    git clone https://github.com/YugabyteDB-Samples/geo-distributed-messenger.git /opt/messenger

    #Create application executables
    cd /opt/messenger/messenger
    mvn clean package -Pprod
    cd /opt/messenger/attachments
    mvn clean package -Pprod
    
    #Install PostgreSQL (used by Kong Gateway)
    sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
    wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
    sudo apt-get update
    sudo apt-get --yes --force-yes install postgresql-13 postgresql-client-13

    sudo service postgresql start

    #Install Kong Gateway
    echo "deb [trusted=yes] https://download.konghq.com/gateway-3.x-ubuntu-$(lsb_release -sc)/ \
    default all" | sudo tee /etc/apt/sources.list.d/kong.list
    sudo apt-get update
    sudo apt install --yes --force-yes kong-enterprise-edition=3.0.0.0

    sudo -u postgres psql -c "CREATE USER kong WITH PASSWORD 'password'"
    sudo -u postgres psql -c "CREATE DATABASE kong OWNER kong"

    sudo touch /etc/kong/kong.conf
    echo 'pg_user = kong' | sudo tee --append /etc/kong/kong.conf
    echo 'pg_password = password' | sudo tee --append /etc/kong/kong.conf
    echo 'pg_database = kong' | sudo tee --append /etc/kong/kong.conf

    sudo kong migrations bootstrap -c /etc/kong/kong.conf
    sudo kong migrations up -c /etc/kong/kong.conf
    
    sudo kong start -c /etc/kong/kong.conf

    #Configure Kong Gateway services and routes
    curl -i -X POST \
        --url http://localhost:8001/services/ \
         --data 'name=attachments-service' \
        --data 'url=http://127.0.0.1:8081'

    curl -i -X POST http://localhost:8001/services/attachments-service/routes \
     -d "name=upload-route" \
     -d "paths[]=/upload" \
     -d "strip_path=false"

    curl -i -X POST http://localhost:8001/services/attachments-service/routes \
     -d "name=ping-route" \
     -d "paths[]=/ping" \
     -d "strip_path=false"

    sudo touch /etc/initialized_on_startup
else
# Executed on restarts
export PATH=/etc/apache-maven/apache-maven-3.8.6/bin:$PATH
export SDKMAN_DIR="/usr/local/sdkman"
source "/usr/local/sdkman/bin/sdkman-init.sh"

sudo service postgresql start
sudo kong start -c /etc/kong/kong.conf
fi

export PROJECT_ID=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/PROJECT_ID -H "Metadata-Flavor: Google")
export REGION=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/REGION -H "Metadata-Flavor: Google")

# Configuring env variable for the Messaging microservice
export PORT=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/PORT -H "Metadata-Flavor: Google")
export DB_URL=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_URL -H "Metadata-Flavor: Google")
export DB_USER=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_USER -H "Metadata-Flavor: Google")
export DB_PWD=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_PWD -H "Metadata-Flavor: Google")
export DB_MODE=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_MODE -H "Metadata-Flavor: Google")
export DB_SCHEMA_FILE=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_SCHEMA_FILE -H "Metadata-Flavor: Google")

# Configuring env variable for the Attachments microservice
export KONG_ATTACHMENTS_API_ROUTE=http://localhost:8000/upload
export ATTACHMENTS_SERVICE_PORT=8081
export ATTACHMENTS_SERVICE_STORAGE_IMPL=google-storage

# Runtime Configurator
export ENABLE_RUNTIME_CONFIGURATOR=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/ENABLE_RUNTIME_CONFIGURATOR -H "Metadata-Flavor: Google")

nohup java -jar /opt/messenger/messenger/target/geo-distributed-messenger-1.0-SNAPSHOT.jar &
nohup java -jar /opt/messenger/attachments/target/attachments-1.0.0-SNAPSHOT.jar &


