# Dockerfile for Fuhsen-reactive
# 1) Build an image using this docker file. Run the following docker command
# docker build -t lidakra/fuhsen:latest .
# 2) Run a container with Fuhsen. Run the following docker command
# docker run -p 127.0.0.1:9000:9000 -i -t lidakra/fuhsen
#
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

# Install Silk Framework rel.2.7.1
WORKDIR /home/
RUN wget https://github.com/silk-framework/silk/releases/download/release-2.7.1/silk-workbench-2.7.1.tgz && \
    tar xvf silk-workbench-2.7.1.tgz && \
    rm silk-workbench-2.7.1.tgz
	
# Install Fuhsen release in /home/lidakra/
WORKDIR /home/lidakra
RUN wget https://github.com/LiDaKrA/FuhSen-reactive/releases/download/v1.0.4.4/fuhsen-1.0.4.4.tgz && \
    tar xvf fuhsen-1.0.4.4.tgz && \
    rm fuhsen-1.0.4.4.tgz

# Install the mapping file for Silk
WORKDIR /home/lidakra/mapping
RUN wget https://github.com/LiDaKrA/data-integration-workspace/releases/download/0.9.1/social_api_mappings.tar.gz && \
    tar xvf social_api_mappings.tar.gz && \
    rm social_api_mappings.tar.gz

# Run Fuhsen 
#CMD ["./fuhsen-1.0-SNAPSHOT/start_fuhsen.sh"]
