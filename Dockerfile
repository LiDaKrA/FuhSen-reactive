# Dockerfile for Fuhsen-reactive
# Pull base image
FROM ubuntu:15.04
MAINTAINER Luigi Selmi <luigiselmi@gmail.com>

# Install Java 8.
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y  software-properties-common && \
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update && \
    echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y oracle-java8-installer && \
    apt-get clean

# Define JAVA_HOME environment variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

WORKDIR /home/lidakra

# Install Fuhsen release in /home/lidakra/
RUN wget https://github.com/LiDaKrA/FuhSen-reactive/releases/download/v1.0.4/fuhsen-1.0-SNAPSHOT.tgz && \
    tar xvf fuhsen-1.0-SNAPSHOT.tgz

# Run Fuhsen 
CMD ["./fuhsen-1.0-SNAPSHOT/bin/fuhsen"]
