# FuhSen [![Build Status](https://travis-ci.org/LiDaKrA/FuhSen-reactive.svg?branch=master)](https://travis-ci.org/LiDaKrA/FuhSen-reactive)

![Logo](https://cloud.githubusercontent.com/assets/4923203/15185984/39e36d62-1769-11e6-993f-cbe815ded833.png)


### Description
Reactive version of the Federated RDF-Based Hybrid Search Engine - **FuhSen**.

### Documentation
The FuhSen wiki contains video tutorials, class, and interaction diagrams to facilitate the understanding of the FunSen architecture  and to facilitate the extension and resuse of FuhSen.
https://github.com/LiDaKrA/FuhSen-reactive/wiki
### Dependencies
FuhSen reactive project depends on the following software

* JDK 1.8
* Play Web Framework 2.4.6 "Damiya" and Activator 1.3.7

Download Play: https://www.playframework.com/download

Installation steps: https://www.playframework.com/documentation/2.4.x/Installing

Fuhsen depends on the Silk Workbench to transform the data collected from the data sources into RDF.
An instance of the workbench must be available with the configuration files containing the transformation rules.
The configuration files for the RDF transformation and all the resources needed to set up an instance of the Silk Workbench are 
provided in the project [Data Integration Workspace](https://github.com/LiDaKrA/data-integration-workspace).
Fuhsen collects data from social networks and other data sources. Some of these require a key to use their API that is 
stored in conf/application.conf. The key must be provided before starting Fuhsen. 

#### IDE support 
The quick and easy way to start compiling, running and coding FuhSen is to use "activator ui".
However, you can also set up your favorits Java IDE (Eclipse or IntellJ Idea). https://www.playframework.com/documentation/2.4.x/IDE

### Install and Build
Fuhsen can be installed from the source code on Github or from the Docker image in the [Lidakra repository](https://hub.docker.com/r/lidakra/)

### Install and build from the source code  
To obtain the latest version of the project please clone the github repository

    $ git clone https://github.com/LiDaKrA/FuhSen-reactive.git

The build system for Fuhsen is Sbt. The project can be compiled and run using Sbt or the Typesafe Activator. In order to compile the project with sbt from the project root folder run the command

    $ sbt compile

The project can be packaged in a tar file in /target/universal/ with the command

    $ sbt package universal:packageZipTarball 


Before making a build, update the version of the project in the following files:
.travis.yml, build.sbt, Dockerfile, start_fuhsen.sh

### Install from the Docker image
A Docker image containing Fuhsen can be built from the Docker file or pulled from the Lidakra Repository on Docker Hub.
Once the image has been downloaded or created the configuration file in conf/application.conf must be changed in order to provide
the keys for the data sources used by Fuhsen and also to update the url of the Silk Workbench.
The config file must be provided in a Docker data volume loaded with the config file. As an example copy the config file in 
a folder in the server host (e.g. /home/lidakra/application.conf) then create or run a container using an image
already available or a small one like alpine (a small Linux version) mapping the config file in the host with the keys to the config file in the container

    $ docker run -d --net none -v /home/lidakra/application.conf:/home/lidakra/fuhsen-1.0.4.4/conf/application.conf:ro \
                                         --name fuhsen-conf alpine echo "Fuhsen Config File"

Start a container with Fuhsen using the config file in the data volume

    $ docker run -d -p 9000:9000 --volumes-from fuhsen-conf --name fuhsen lidakra/fuhsen

### Run
Fuhsen can be started using Sbt or the Typesafe activator.

#### Run with Sbt
From the project root folder run the command

    $ sbt start

The Fuhsen server will listen on port 9000.

#### Run with Typesafe Activator 
From the project root folder execute the command "activator ui". The application is going to be compiled and launched 
at the following address: http://localhost:9000. Once the UI is launched in the browser go to the Run tab and select "Run app".


#### Example Usage

#### License

* Copyright (C) 2015-2016 EIS Uni-Bonn
* Licensed under the Apache License


