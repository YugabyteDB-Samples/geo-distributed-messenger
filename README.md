# Geo-Distributed Messenger With Vaadin, Kong and YugabyteDB

This project is a geo-distributed messenger that is inspired by Slack. The messenger is build on Vaadin, Spring Boot and YugabyteDB. PostgreSQL can be used as an alternate database for single-zone deployments. YugabyteDB is used as a distributed database that can span multiple zones and regions. 

## Dev Journal

This is an ongoing project, so, expect the source code and description to change significantly over the time. The development journey is documented in the following articles:
* [Geo What? A Quick Introduction to Geo-Distributed Apps](https://dzone.com/articles/geo-what-a-quick-introduction-to-geo-distributed-a)
* [What Makes the Architecture of Geo-Distributed Apps Different?](https://dzone.com/articles/what-makes-the-architecture-of-geo-distributed-app)
* [How to Build a Multi-Zone Java App in Days With Vaadin, YugabyteDB, and Heroku](https://dzone.com/articles/how-to-build-a-multi-zone-java-app-in-days-with-va)
* [How To Connect a Heroku Java App to a Cloud-Native Database](https://dzone.com/articles/how-to-connect-a-heroku-app-to-a-yugabytedb-manage)
* [Automating Java Application Deployment Across Multiple Cloud Regions](https://dzone.com/articles/automating-java-application-deployment-across-mult)


## Deployment Options

The application can be started in several environments.

| Deployment Type    | Description   |         
| ------------------ |:--------------|
| [Your Laptop](local_deployment.md)        | Deploy the entire app with all the components (Kong, YugabyteDB, Minio) on your local machine.|
| [Heroku](heroku_deployment.md)             | Deploy the application in Heroku.     |
| [Google Cloud](gcloud_deployment.md)       | Deploy the application in the cloud native way across multiple geographic location using Google Cloud infrastructure and resources.     |
