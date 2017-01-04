#!/bin/bash
# Remove RUNNING_PID
find . -type f -name RUNNING_PID -exec rm -f {} \;

# Start Fuhsen
./bin/fuhsen 

