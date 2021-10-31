# CS1660_FinalProject
University of Pittsburgh CS 1660 Cloud Computing Final Project involving Docker, Google Cloud Platform, and Hadoop

## Instructions for Running

The frontend GUI is a Python Flask App that should be accessible with a web browser. Without Docker, it can be run with the code on the repository by installing the requirements, setting FLASK_APP=app.py and running `flask run`. The code to run the app has been put into a docker container found on [Docker Hub](https://hub.docker.com/r/amw8711/final-project-gui).

To download the frontend GUI application, run 

```
docker pull amw8711/final-project-gui
```

Next, to run the application, run 

```
docker run -d -p 5000:5000 amw8711/final-project-gui
```

The `-d` flag is optional and makes the app run in the background. Otherwise, the output is the container ID which is needed to stop the container. (Example:  `adecc9040f904510345750ffb1758a53ca4cf48c3936d0154fd020db62321fd1`)

The application should be running on localhost port 5000, and is accessible via web browser.

To stop the application, run
```
docker stop <CONTAINER_ID> 
```

where `<CONTAINER_ID>` is replaced with the container ID produced when starting the application. (Example: `docker stop adecc9040f904510345750ffb1758a53ca4cf48c3936d0154fd020db62321fd1`)

## Plans for Deployment

I plan to connect this app to the backend and GCP in a similar way to how I did it in the [mini project](https://github.com/wippra/CS1660_MiniProject). Both the frontend and the backend will be accessible through an external IP address (made available through a delpoyment on GCP). The frontend will send requests to the backend, including the files and what operations to perform. The backend will accepts these requests and perform necessary actions (such as taking the uploaded files and putting them on HDFS). The backend will send its results back to the frontend, so that it can format them and display them to the user.

To connect to GCP, to start I will:

- Add the docker image to Google's Container Registry
  - Using the Cloud Shell on https://console.cloud.google.com/ , pull the Docker image to the VM (`docker pull amw8711/final-project-gui`)
  - Using the Cloud Shell, tag the image for the Container Registry (`docker tag amw8711/final-project-gui us.gcr.io/cs-1660/final-project-gui`)
  - Using the Cloud Shell, push the tagged image to the Container Registry (`docker push us.gcr.io/cs-1660/final-project-gui`)
- Create a cluster for the frontend. Done through the GCP website: Left Navigation Menu > Kubernetes Engine > Clusters. Create and configure a GKE standard cluster.
- Deploy the image to the cluster. Done through the GCP website: Clusters > Deploy. Edit the container with an Existing container image with the one pushed to the registry.
- Expose the image to make it externally available for the backend and to the public. Done through the GCP website: Kubernetes Engine > Workloads. Click on the deployment's name. Under Actions > Expose to create a Load Balancer or other service. This provides the external IP needed to access the application.

Further steps will be needed to make the exposed app work with the backend.
