FROM maven:3.8.3-openjdk-17

ARG build_type=dev

RUN mkdir -p /opt/messenger/messenger
WORKDIR /opt/messenger/messenger

COPY pom.xml mvnw ./
COPY .mvn ./.mvn
COPY src ./src

COPY frontend/themes ./frontend/themes
COPY frontend/index.html ./frontend
COPY package.json tsconfig.json types.d.ts webpack.config.js ./

RUN mvn clean package -P$build_type -DskipTests

CMD ["java", "-jar", "/opt/messenger/messenger/target/geo-distributed-messenger-1.0-SNAPSHOT.jar"]