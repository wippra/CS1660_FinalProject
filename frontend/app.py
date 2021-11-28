import time
import json
import os
from datetime import datetime

from google.cloud import dataproc_v1 as dataproc
from google.cloud import storage

from flask import Flask, request, session, redirect, url_for, render_template, flash

app = Flask(__name__)

# Load default config and override config from an environment variable
app.config.update(dict(SECRET_KEY='development key',))

# Get some of details of GCP from a file (specified with the GCP_CONFIG environment variable)
with open (os.getenv('GCP_CONFIG')) as config_file:
    config = json.load(config_file)

# Variables for GCP
bucket_name = config["bucket_name"]
bucket_path = r"gs://" + bucket_name
project_id = config["project_id"] 
region = config["region"]
cluster_name = config["cluster_name"]
ii_jars = [bucket_path + "/jars/ii.jar"]
topn_jars = [bucket_path + "/jars/topn.jar"]
search_jars = [bucket_path + "/jars/search.jar"]

# Attach the time to each directory to keep track of many directories, and to ensure each run has an empty output directory
base_dir_name = str(datetime.now()).replace(' ',"-").replace(":", "-").replace(".", "-")

# Setup the API clients for interacting with GCP buckets and creating Hadoop jobs
job_client = dataproc.JobControllerClient(client_options={"api_endpoint": "{}-dataproc.googleapis.com:443".format(region)})
storage_client = storage.Client()
bucket = storage_client.bucket(bucket_name)

def run_ii(input_dir):
    '''
    Submits the job for creating inverted indices for the given `input_dir`

    Returns the directory of the results
    '''
    output_dir = f"{bucket_path}/ii_results-{base_dir_name}"
    submit_job(project_id, region, cluster_name, "II", ii_jars, [input_dir, output_dir])
    return output_dir

def already_done(dir):
    '''
    Returns if a given directory exists in the program's GCP bucket
    '''
    return storage.Blob(bucket=bucket, name=dir).exists(storage_client)

def run_topn(n):
    '''
    Submits the job for finding the top n results from the inverted indices

    Returns the results of the top n as a string
    '''
    relative_output_dir = f"top{n}-{base_dir_name}"
    output_dir = f"{bucket_path}/{relative_output_dir}"
    input_dir = f"{session['ii_results_dir']}/"
    if not already_done(f"{relative_output_dir}/"):
        submit_job(project_id, region, cluster_name, "TopNFromII", topn_jars, [input_dir, output_dir, n])
    blob = bucket.blob(f"{relative_output_dir}/part-r-00000")
    return blob.download_as_bytes().decode("utf-8")

def run_search(term):
    '''
    Submits the job for finding the term search results from the inverted indices

    Returns the results of the term search as a string
    '''
    relative_output_dir = f"search-{term.lower()}-{base_dir_name}"
    output_dir = f"{bucket_path}/{relative_output_dir}"
    input_dir = f"{session['ii_results_dir']}/"
    if not already_done(f"{relative_output_dir}/"):
        submit_job(project_id, region, cluster_name, "TermSearchFromII", search_jars, [input_dir, output_dir, term])
    blob = bucket.blob(f"{relative_output_dir}/part-r-00000")
    return blob.download_as_bytes().decode("utf-8")

def upload_blob(source_file_name, destination_blob_name):
    '''
    Uploads a file to a GCP bucket

    Modified from gcloud documentation https://cloud.google.com/storage/docs/uploading-objects#storage-upload-object-python
    '''
    blob = bucket.blob(destination_blob_name)
    blob.upload_from_filename(source_file_name)
    return True

def submit_job(project_id, region, cluster_name, main_class, jars, args):
    '''
    Submits a Hadoop job to a GCP cluster

    Modified from gcloud documentation https://cloud.google.com/dataproc/docs/guides/submit-job#dataproc-submit-job-python
    '''
    job = {
        "placement": {"cluster_name": cluster_name},
        "hadoop_job": {
            "main_class": main_class,
            "jar_file_uris": jars,
            "args": args,
        },
    }
    operation = job_client.submit_job_as_operation(request={"project_id": project_id, "region": region, "job": job})
    response = operation.result()
    return response.done

@app.before_first_request
def before_first_request():
    session.clear()

@app.route('/', methods=['GET', 'POST'])
@app.route('/home', methods=['GET', 'POST'])
@app.route('/homepage', methods=['GET', 'POST'])
def home():
    if request.method == 'GET':
        return render_template('homepage.html')
    elif engine_loaded():
        flash("Reload the engine by restarting the app.")
        return render_template('homepage.html')
    elif request.files:
        files_list = request.files.getlist("files")
        if len(files_list) == 1 and files_list[0].filename == '':
            # Load default files (from Hugo.tar.gz, shakespeare.tar.gz, Tolstoy.tar.gz)
            session['input_dir'] = "/default_input"

            # Create the Inverted Index from the default directory
            session['ii_results_dir'] = run_ii(f"{bucket_path}/default_input")
            session['engine_loaded'] = True
            return redirect(url_for('engine'))
        else:
            # Allow the user to put in their own files by creating a bucket directory for them
            input_dir = f"/input-{base_dir_name}"
            session['input_dir'] = input_dir

            # Upload each user file to a new input directory
            for file in files_list:
                file.save(file.filename)
                upload_blob(file.filename, f"input-{base_dir_name}/{file.filename}")

            # Create the Inverted Index from the custom directory
            session['ii_results_dir'] = run_ii(f"{bucket_path}{input_dir}")
            session['engine_loaded'] = True
            return redirect(url_for('engine'))
    else:
        flash("Something went wrong, please try again.")
        return render_template('homepage.html')

@app.route('/engine')
def engine():
    if not engine_loaded():
        flash("Please initialize the engine")
        return redirect(url_for('home'))
    return render_template('engine.html')

@app.route('/search', methods=['GET', 'POST'])
def search():
    if not engine_loaded():
        flash("Please initialize the engine")
        return redirect(url_for('home'))
    elif request.method == 'GET':
        return render_template('search.html')
    elif request.form['term']:
        term = request.form['term']

        # Time and run the term search job and get the output
        start_time = time.time()
        raw_data = run_search(term)

        # Special case when no results are found
        if(len(raw_data) == 0):
            flash("No results found.")
            return render_template('search.html')

        # Format the key-value results of the term search job
        data = []
        for index, item in enumerate(raw_data.strip().split('\n')):
            # Each output entry is (full_path, frequency)
            entry = item.split('\t')

            # Get the relative path and file name each without the bucket info from the full path
            relative_dir = entry[0].replace(f"{bucket_path}{session['input_dir']}/", '')
            dir_list = relative_dir.split('/')
            doc_folder = '/'.join(dir_list[:-1])
            doc_name = dir_list[-1]

            # Add each table row (doc_id, doc_folder, doc_name, frequency) to data
            row = []
            row.append(index+1)
            row.append(doc_folder)
            row.append(doc_name)
            row.append(entry[1])

            data.append(row)
        
        return render_template('search-term.html', data=data, term=term, time=(time.time() - start_time))
    else:
        flash("Something went wrong, please try again.")
        return render_template('search.html')

@app.route('/top-n', methods=['GET', 'POST'])
def top_n():
    if not engine_loaded():
        flash("Please initialize the engine")
        return redirect(url_for('home'))
    elif request.method == 'GET':
        return render_template('top-n.html')
    elif request.form['top-n']:
        # Verify input
        n = request.form['top-n']
        if not n.isnumeric() or int(n) <= 0:
            flash("Please enter a valid number.")
            return render_template('top-n.html')

        # Time and run the top n job and get the output
        start_time = time.time()
        raw_data = run_topn(n)

        # Format the key-value results of the top n job
        data = [i.split('\t') for i in raw_data.strip().split('\n')]
        return render_template('top-n-results.html', data=data, time=(time.time() - start_time))
    else:
        flash("Something went wrong, please try again.")
        return render_template('top-n.html')

def engine_loaded():
    if 'engine_loaded' not in session or not session['engine_loaded']:
        return False
    return True