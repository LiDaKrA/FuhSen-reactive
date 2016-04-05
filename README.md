# FuhSen-reactive [![Build Status](https://travis-ci.org/LiDaKrA/FuhSen-reactive.svg?branch=master)](https://travis-ci.org/LiDaKrA/FuhSen-reactive)
================

* Copyright (C) 2015-2016 EIS Uni-Bonn
* Licensed under the Apache License

Reactive version of the Federated RDF-Based Hybrid Search Engine.

-----

## Getting started

###Software requirements
FuhSen reactive project depends on the following software

* JDK 1.8
* Play Web Framework 2.4.6 "Damiya" and Activator 1.3.7

Download Play: https://www.playframework.com/download

Installation steps: https://www.playframework.com/documentation/2.4.x/Installing

### IDE support 
The quick and easy way to start compiling, running and coding FuhSen is to use "activator ui".
However, you can also set up your favorits Java IDE (Eclipse or IntellJ Idea). https://www.playframework.com/documentation/2.4.x/IDE

### Download 
To obtain the latest version of the project please clone the github repository

	git clone https://github.com/LiDaKrA/FuhSen-reactive.git

### Build
Before making a build update the version of the project in the following files:
.travis.yml, build.sbt, Docker, start_fuhsen.sh

### Run 
In order to run the app navigate until project folder, then execute command "activator ui", once the UI is launched go to Run tab and select "Run app".
The application is going to be compiled and launched in the following address: http://localhost:9000
