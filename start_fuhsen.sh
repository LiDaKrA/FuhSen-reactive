#!/bin/bash
# Script to run Fuhsen in a docker container.

# Run Silk
cd /home/silk-workbench-2.7.1/bin/
./silk-workbench -Dhttp.port=9005 &

# Run Fuhsen
cd /home/lidakra/fuhsen-1.0-SNAPSHOT/bin/
./fuhsen & 
