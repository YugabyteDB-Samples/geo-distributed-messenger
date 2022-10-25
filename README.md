# Geo-Distributed Messenger With YugabyteDB

This a sample geo-distributed messenger that runs on YugabyteDB database, Kong Gateway, Spring Boot, Vaadin, MinIO and several cloud services.   

![image9](https://user-images.githubusercontent.com/1537233/197895210-5052d681-cd8e-45b2-a621-429b05bce682.png)

The messenger can be deployed as a single instance on your local laptop or function across the world in the public Google Cloud infrastructure.

Use this project as a blueprint for designing and building geo-distributed apps with YugabyteDB from scratch.

## Development Journal

The project's development journey was thoroughly documented on DZone so that the reader can understand design choices and tradeoffs. Follow the journey from start to finish by surfing through the following articles:
* [Geo What? A Quick Introduction to Geo-Distributed Apps](https://dzone.com/articles/geo-what-a-quick-introduction-to-geo-distributed-a)
* [What Makes the Architecture of Geo-Distributed Apps Different?](https://dzone.com/articles/what-makes-the-architecture-of-geo-distributed-app)
* [How to Build a Multi-Zone Java App in Days With Vaadin, YugabyteDB, and Heroku](https://dzone.com/articles/how-to-build-a-multi-zone-java-app-in-days-with-va)
* [How To Connect a Heroku Java App to a Cloud-Native Database](https://dzone.com/articles/how-to-connect-a-heroku-app-to-a-yugabytedb-manage)
* [Automating Java Application Deployment Across Multiple Cloud Regions](https://dzone.com/articles/automating-java-application-deployment-across-mult)
* [Geo-distributed API Layer With Kong Gateway](https://dzone.com/articles/geo-distributed-api-layer-with-kong-gateway)
* [Using Global Cloud Load Balancer to Route User Requests to App Instances](https://dzone.com/articles/using-global-cloud-load-balancer-to-route-user-req)

And feel free to reach out to the [main developer and author of the project](https://twitter.com/denismagda) for feedback or questions.

## Deployment Options

The application can be started in several environments.

| Deployment Type    | Description   |         
| ------------------ |:--------------|
| [Your Laptop](local_deployment.md)        | Deploy the entire app with all the components (Kong, YugabyteDB, Minio) on your local machine.|
| [Heroku](heroku_deployment.md)             | Deploy the application in Heroku.     |
| [Google Cloud](gcloud_deployment.md)       | Deploy the application in the cloud native way across multiple geographic location using Google Cloud infrastructure and resources.     |
