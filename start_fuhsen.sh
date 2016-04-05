#!/bin/bash
# Script to run Fuhsen in a docker container.

# Run Silk in background
cd /home/silk-workbench-2.7.1/bin/
./silk-workbench -Dhttp.port=9005 -Dworkspace.provider.file.dir=/home/lidakra/mapping/SocialAPIMappings &

# Run Fuhsen in background
cd /home/lidakra/fuhsen-1.0-SNAPSHOT/bin/
./fuhsen & 
