# CS1660_FinalProject
University of Pittsburgh CS 1660 Cloud Computing Final Project involving Docker, Google Cloud Platform, and Hadoop

## Instructions for Running

The frontend GUI is a Python Flask App that should be accessible with a web browser. The code to run the app has been put into a docker container found on [Docker Hub](https://hub.docker.com/r/amw8711/final-project-gui).

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

I plan to connect this app to the backend and GCP in a similar way to how I did it in the [mini project](https://github.com/wippra/CS1660_MiniProject).