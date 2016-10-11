# Dockerfile for Fuhsen-reactive
# 1) Build an image using this docker file. Run the following docker command
#
#    $ docker build -t lidakra/fuhsen:latest .
#
# 2) Test Fuhsen in a container. Run the following docker command for testing
#
#    $ docker run --rm -it -p 9000:9000 --name fuhsen lidakra/fuhsen /bin/bash
#
# 3) Fuhsen needs API keys to access social networs. The keys are stored in conf/application.conf
# For security reason the application.conf file on Github does not contain the keys. The config file
# must be provided in a Docker data volume loaded with the config file. As an example copy the config file in 
# a folder in the server host (e.g. /home/lidakra/application.conf) then create or run a container using an image
# already available or a small one like alpine (a small Linux version) mapping the config file in the host to the 
# config file in the container.
# 
#    $ docker run -d -v /home/lidakra/application.conf:/home/lidakra/fuhsen-1.1.0/conf/application.conf:ro \
#                                         --name fuhsen-conf alpine echo "Fuhsen Config File"
#
# 4) Start a container with Fuhsen using the config file in the data volume
#
#    $ docker run -d -p 9000:9000 --volumes-from fuhsen-conf --name fuhsen lidakra/fuhsen 
#

# Pull base image
#FROM ubuntu:15.04
FROM ubuntu
MAINTAINER Luigi Selmi <luigiselmi@gmail.com>

# Install Java 8.
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y  software-properties-common && \
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update && \
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y oracle-java8-installer && \
    apt-get clean

# Define JAVA_HOME environment variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# Copy OCCRP SSL Certificate
COPY  certs/data.occrp.org.cer $JAVA_HOME/jre/lib/security/

# Install the OCCRP SSL certificate
WORKDIR $JAVA_HOME/jre/lib/security/
RUN keytool -importcert -alias occrp -keystore cacerts -storepass changeit -file data.occrp.org.cer -noprompt

#Install Fuhsen package from the project folder (create a package using "sbt universal:package-zip-tarball" command)
COPY target/universal/fuhsen-1.1.0.tgz /home/lidakra/
WORKDIR /home/lidakra/
RUN tar xvf fuhsen-1.1.0.tgz  

# Copy the schema folder (as sbt universal package does not include it by default)
COPY schema/ /home/lidakra/fuhsen-1.1.0/schema/
 
# Start Fuhsen
WORKDIR /home/lidakra/fuhsen-1.1.0
CMD ./bin/fuhsen
