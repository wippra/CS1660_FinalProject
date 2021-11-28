# CS1660_FinalProject
University of Pittsburgh CS 1660 Cloud Computing Final Project involving Docker, Google Cloud Platform, and Hadoop

## Assumptions Made
While the intent was to generalize the application as much as possible, some simplifying assumptions were made:
- The jar files of each algorithm are already compiled and contained in a folder named `jars/` in the specified bucket, named `ii.jar`, `topn.jar`, and `search.jar`. The class names are "II", "TopNFromII", and "TermSearchFromII"
	- The steps for compilation are from [Hadoop's WordCount example](https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html#Usage), importantly with the commands `hadoop com.sun.tools.javac.Main WordCount.java` and `jar cf wc.jar WordCount*.class` with the classes and name of the output jar file replaced accordingly.
- The program expects billing to be enabled, and will fail if it isn't.
- The program expects the specified cluster to be started and running, and will fail if it isn't.

## Instructions for Running

The frontend GUI is a Python Flask App that should be accessible with a web browser. The code to run the app has been put into a docker container found on [Docker Hub](https://hub.docker.com/r/amw8711/final_project).

To download the frontend GUI application, run 

```
docker pull amw8711/final-project
```

Next, to run the application, run 

```
docker run -v <LOCAL_DIRECTORY_OF_CONFIG>:config --env GCP_CONFIG=/config/<NAME_OF_GCP_CONFIG_JSON> --env GOOGLE_APPLICATION_CREDENTIALS=/config/<NAME_OF_GCP_AUTH_JSON> -d -p 5000:5000 amw8711/final_project
```

### Explanation

- The `-v` command is used to make a local directory containing your GCP authentication and configuation details files accessible to the docker image in a `/config` directory. `<LOCAL_DIRECTORY_OF_CONFIG>` will be replaced with a local directory on your system. Example: `-v C:/Users/Adam/Documents/CloudComp/CS1660_FinalProject:/config`
- The `--env` commands each set necessary environment variables for the image to run. 
	- `GCP_CONFIG` is a json file with basic information about the bucket and cluster to be used. Example:
	```json
	{
		"project_id" : "cs-1660",
		"region": "us-east4",
		"cluster_name" : "cluster-160d",
		"bucket_name" : "dataproc-staging-us-east4-188727330464-75bstrkl"
	}
	```
	- `GOOGLE_APPLICATION_CREDENTIALS` is a json file [generated from a IAM Service account](https://cloud.google.com/docs/authentication/getting-started). The roles used for testing were `Dataproc Administrator` and `Storage Admin`. (*The specific permissions used were:*
		- `dataproc.clusters.create`
		- `dataproc.clusters.use`
		- `dataproc.jobs.create`
		- `dataproc.operations.get`
		- `storage.buckets.get`
		- `storage.buckets.list`
		- `storage.objects.create`
		- `storage.objects.delete`
		- `storage.objects.get`
		- `storage.objects.list`)
	- `<NAME_OF_GCP_CONFIG_JSON>` is replaced with the name of the json file of the basic GCP configuration. `<NAME_OF_GCP_AUTH_JSON>` is replaced with the name of the josn file of the GCP authentication. Example: `--env GCP_CONFIG=/config/gcp_config.json --env GOOGLE_APPLICATION_CREDENTIALS=/config/cs-1660-a09b8fdafade.json`
- The `-d` flag is optional and makes the app run in the background. Otherwise, the output is the container ID which is needed to stop the container. (Example:  `adecc9040f904510345750ffb1758a53ca4cf48c3936d0154fd020db62321fd1`)
- Complete Example of run command: `docker run -v C:/Users/Adam/Documents/CloudComp/CS1660_FinalProject:/config --env GCP_CONFIG=/config/gcp_config.json --env GOOGLE_APPLICATION_CREDENTIALS=/config/cs-1660-a09b8fdafade.json -d -p 5000:5000 amw8711/final_project`

The application should be running on localhost port 5000, and is accessible via web browser. (http://localhost:5000/)

To stop the application, run
```
docker stop <CONTAINER_ID> 
```

where `<CONTAINER_ID>` is replaced with the container ID produced when starting the application. (Example: `docker stop adecc9040f904510345750ffb1758a53ca4cf48c3936d0154fd020db62321fd1`)

## Code Walkthrough and Demo Video

The code walkthrough and demo are contained within the same video and is publically accessible [here](https://pitt-my.sharepoint.com/:v:/g/personal/amw290_pitt_edu/Ee3Sp9XkuONAtdpH4HIu6fYB_sp5CnmmrOvfBmXKorp3mQ). The backend files (`II.java`, `TermSearchFromII.java`, and `TopNFromII.java`) are found in the [backend](/backend) folder. The frontend file (`app.py`), is found in the [frontend](/frontend) folder.