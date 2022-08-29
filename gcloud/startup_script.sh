#! /bin/bash

if [ ! -f "/etc/initialized_on_startup" ]; then
    echo "Launching the VM for the first time."

    sudo apt-get update
    sudo apt-get install zip unzip

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
    cd /opt/messenger

    #Option #1: copy the released JAR to the target folder (it will be started from there)
    mkdir target
    cp release/geo-distributed-messenger-1.0-SNAPSHOT.jar target
    #Option #2: comment two lines above and build the app from sources
    #mvn clean package -Pprod
    
    sudo touch /etc/initialized_on_startup
else
# Executed on restarts
export PATH=/etc/apache-maven/apache-maven-3.8.6/bin:$PATH
export SDKMAN_DIR="/usr/local/sdkman"
source "/usr/local/sdkman/bin/sdkman-init.sh"
fi

# Executed during the first VM start as well as on restarts
export PORT=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/PORT -H "Metadata-Flavor: Google")
export DB_URL=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_URL -H "Metadata-Flavor: Google")
export DB_USER=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_USER -H "Metadata-Flavor: Google")
export DB_PWD=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_PWD -H "Metadata-Flavor: Google")

java -jar /opt/messenger/target/geo-distributed-messenger-1.0-SNAPSHOT.jar