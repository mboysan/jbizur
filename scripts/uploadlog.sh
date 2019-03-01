#!/bin/bash

# provide log dir and switch to that dir
echo "switching to log dir: ${1}"
cd $1

# find latest log file and print on console
LOGFILE="$(ls -t * | head -1)"
echo "uploading ${LOGFILE} ..."

# upload file
#curl -s --upload-file $LOGFILE https://transfer.sh/$LOGFILE
wget --method PUT --body-file=$LOGFILE https://transfer.sh/$LOGFILE -O - -nv