.. _google-page:

************
Google Cloud
************

Nextflow provides out of the box support for the `Google Cloud Platform <https://cloud.google.com/>`_
enabling the seamless deployment and execution of Nextflow pipelines over Google cloud services.

The execution can be done either deploying a Nextflow managed cluster using `Google Compute Engine <https://cloud.google.com/compute/>`_
instances or via the `Genomics Pipelines <https://cloud.google.com/genomics/>`_ managed service.

.. warning:: This is an experimental feature and it may change in a future release. It requires Nextflow
  version `19.1.0` or later.


Requirements
============

To allow the deployment in the Google Cloud you need to configure the security credentials using
a *Security account key* JSON file.

Nextflow looks for this file using the ``GOOGLE_APPLICATION_CREDENTIALS`` variable that
has to be defined in the launching environment.

If you don't have it, download the credentials file from the Google Cloud Console following this produce:

* Open the `Google Cloud Console <https://console.cloud.google.com>`_
* Go to APIs & Services → Credentials
* Click on the *Create credentials* (blue) drop-down and choose *Service account key*, in the following page
* Select an existing *Service account* or create a new one if needed
* Select JSON as *Key type*
* Click the *Create* button and download the JSON file giving a name of your choice e.g. ``creds.json``.

Finally define the following variable replacing the path in the example with the one of your
credentials file just downloaded::

    export GOOGLE_APPLICATION_CREDENTIALS=/path/your/file/creds.json


Compute Engine
==============

When using this feature Nextflow allows you set up an ephemeral computing cluster in in the Google Cloud platform
and use it to deploy your pipeline execution.

Configuration
-------------

Cloud configuration attributes are provided in the ``nextflow.config`` file as shown in the example below::

    cloud {
        driver = 'google'
        imageId = 'rare-lattice-222412/global/images/seqvm-1'
        instanceType = 'n1-highcpu-8'
    }

    google {
        project = 'your-project-id'
        zone = 'us-central1-f'
    }

The above attributes define the image ID and instance type to be used along with the project and zone to be used.
Replace these values with the ones of your choice.

.. tip:: Make sure to specify the ``google`` as cloud driver value as shown in the above example.

Nextflow requires a Linux image that provides support for `Cloud-init <http://cloudinit.readthedocs.io/>`_
bootstrapping mechanism and includes the Java runtime (version 8 or later) and the Docker engine (version 1.11 or later).

Refer to the Google cloud documentation to learn `how to create a custom image <https://cloud.google.com/compute/docs/images/create-delete-deprecate-private-images>`_.

User & SSH key management
-------------------------

By default Nextflow creates in each GCE instance a user with the same name as the one in your local computer and install
the SSH public key available at the path ``$HOME/.ssh/id_rsa.pub``.

A different user/key can be specified as shown below::

    cloud {
        driver = 'google'
        userName = 'the-user-name'
        keyFile = '/path/to/ssh/key.pub'
    }

Storage
-------

Both input data and the pipeline work directory must be located in one or more `Google Storage <https://cloud.google.com/storage/>`_ buckets.
Make sure your security credentials allows you to access those buckets.

Cluster deployment
------------------

Once you have defined the configuration settings in the ``nextflow.config`` file you can create the cloud cluster by
using the following command::

    nextflow cloud create my-cluster -c <num-of-nodes>

The string ``my-cluster`` identifies the cluster instance. Replace it with a name of your choice.

Finally replace ``<num-of-nodes>`` with the actual number of instances that will make-up the cluster. One node is
created as master, the remaining as workers. If the option ``-c`` is omitted only the **master** node is created.

.. warning:: You will be charged accordingly the type and the number of instances chosen.

The console will display the configuration that you have defined and ask you to confirm creation of the cluster with the
configuration displayed. It will take some time for the cluster to deploy. You should be able to track the status of the
deployed in the administration console for the Google Cloud Platform under VM instances in the Compute Engine section.


Pipeline execution
------------------

Once the master node is available, Nextflow will display the SSH command to connect to the master node. Use
that command to connect to the cluster.

.. note:: On MacOS, use the following command to avoid being asked for a pass-phrase even
  you haven't defined one::

    ssh-add -K [private key file]

The suggested approach is to run your pipeline downloading it from a public repository such as GitHub and to pack the
binaries dependencies in a Docker container as described in the :ref:`Pipeline sharing <sharing-page>` section.

.. warning:: Before run any Nextflow command, make sure the file ``READY`` has been create in the home directory.
  If you can't find it, it means that the initialisation process is still on-going. Wait a few seconds until it completes.

Then, you can run Nextflow as usual. For example::

    nextflow run rnaseq-nf -profile gcp -w gs://my-bucket/work


.. tip:: Make sure to use Google Storage bucket as Nextflow work directory and as location for pipeline input data.

Cluster shutdown
----------------

When completed shutdown the cluster instances by using the following command::

    nextflow cloud shutdown my-cluster

Preemptible instances 
---------------------

An optional parameter allows you to set the instance to be preemptible. Both master and worker instances can be set to
be preemptible. The following example shows a cluster configuration with a preemptible setting::

    cloud {
        imageId = 'rare-lattice-222412/global/images/seqvm-1'
        instanceType = 'n1-highcpu-8'
        preemptible = true
    }

Setting an instance to preemptible allows the administrator to kill the VM at will and may affect the pricing of the
instance.

Cluster auto-scaling
--------------------

Nextflow integration for Google Cloud Engine provides a native support auto-scaling that allows the computing cluster
to scale out or scale down i.e., add or remove computing nodes dynamically at runtime.

This is a critical feature, especially for pipelines crunching non-homogeneous datasets, because it allows the cluster
to adapt dynamically to the actual workload computing resources need as they change over the time.

Cluster auto-scaling is enabled by adding the autoscale option group in the configuration file as shown below::

    cloud {
        imageId = 'rare-lattice-222412/global/images/seqvm-1'
        autoscale {
            enabled = true
            maxInstances = 10
        }
    }


The above example enables automatic cluster scale-out i.e. new instances are automatically launched and added to the
cluster when tasks remain too long in wait status because there aren't enough computing resources available. The
``maxInstances`` attribute defines the upper limit to which the cluster can grow.

By default unused instances are not removed when they are not utilised. If you want to enable automatic cluster scale-down
specify the ``terminateWhenIdle`` attribute in the ``autoscale`` configuration group.

It is also possible to define a different AMI image ID, type and spot price for instances launched by the Nextflow autoscaler.
For example::

    cloud {
        imageId = 'your-project/global/images/xxx'
        instanceType = 'n1-highcpu-8'
        preemptible = false
        autoscale {
            enable = true
            preemptible = true
            minInstances = 5
            maxInstances = 10
            imageId = 'your-project/global/images/yyy'
            instanceType = 'n1-highcpu-8'
            terminateWhenIdle = true
        }
    }

By doing that it's is possible to create a cluster with a single node i.e. the master node. Then the autoscaler will
automatically add the missing instances, up to the number defined by the ``minInstances`` attributes. 


Advanced configuration
----------------------

Read :ref:`Cloud configuration<config-cloud>` section to learn more about advanced cloud configuration options.


.. _google-pipelines:

Genomics Pipelines
==================

`Genomics Pipelines <https://cloud.google.com/genomics/>`_ is a managed computing service that allows the execution of
containerized workloads in the Google Cloud Platform infrastructure.

Nextflow provides built-in support for Genomics Pipelines API which allows the seamless deployment of a Nextflow pipeline
in the cloud, offloading the process executions as pipelines.

.. warning:: This API works well for coarse-grained workloads i.e. long running jobs. It's not suggested the use
  this feature for pipelines spawning many short lived tasks.

.. _google-pipelines-config:

Configuration
-------------

Make sure to have defined in your environment the ``GOOGLE_APPLICATION_CREDENTIALS`` variable.
See the section `Requirements`_ for details.

.. tip:: Make sure to have enabled Genomics API to use this feature. To learn how to enable it
  follow `this link <https://cloud.google.com/genomics/docs/quickstart>`_.

Create a ``nextflow.config`` file in the project root directory. The config must specify the following parameters:

* Google Pipelines as Nextflow executor i.e. ``process.executor = 'google-pipelines'``.
* The Docker container images to be used to run pipeline tasks e.g. ``process.container = 'biocontainers/salmon:0.8.2--1'``.
* The Google Compute Engine machine instance type i.e. ``cloud.instanceType = 'n1-standard-1'``.
  See the list of available machine types at the `this link <https://cloud.google.com/compute/docs/machine-types>`_.
* The Google Cloud `project` to run in e.g. ``google.project = 'rare-lattice-222412'``.
* The Google Cloud `region` or `zone`. You need to specify either one, **not** both. Multiple regions or zones can be
  specified by separating them with a comma e.g. ``google.zone = 'us-central1-f,us-central-1-b'``.

Example::

    process {
        executor = 'google-pipelines'
        container = 'your/container:latest'
    }
    
    cloud {
        instanceType = 'n1-standard-1'
    }
     
    google {
        project = 'your-project-id'
        zone = 'europe-west1-b'
    }


.. tip:: You can use a different Docker image for each process using one or more :ref:`config-process-selectors`.
  The use of multiple instance types is not yet supported.


Pipeline execution
------------------

The pipeline can be launched either in a local computer or a cloud instance.

Pipeline input data can be stored to be stored either locally or in a Google Storage bucket. The latter is preferred
to avoid unnecessary data transfer.

The pipeline execution must specifies a S3 bucket where jobs intermediate results are stored with the ``-bucket-dir``
command line options. For example::

    nextflow run <script or project name> -bucket-dir gs://my-bucket/some/path


Hybrid execution
----------------

Nextflow allows the use of multiple executors in the same workflow application. This feature enables the deployment
of hybrid workloads in which some jobs are execute in the local computer or local computing cluster and
some other jobs are offloaded to Google Pipelines service.

To enable this feature use one or more :ref:`config-process-selectors` in your Nextflow configuration file to apply
the Google Pipelines *executor* only to a subset of processes in your workflow.
For example::


    process {
        withLabel: bigTask {
            executor = 'google-pipelines'
            container = 'my/image:tag'
        }
    }

    cloud {
        instanceType = 'n1-ultramem-160'
    }

    google {
        project = 'your-project-id'
        zone = 'europe-west1-b'
    }


Then run your application as before specifying the Google Storage bucket
for the remote execution as a command line option::

    nextflow run <script or project name> -bucket-dir gs://my-bucket/some/path

