#!/bin/bash

cd ../config/log
LOGFILE="$(ls -t log* | head -1)"
echo "uploading ${LOGFILE} ..."
curl -s --upload-file $LOGFILE https://transfer.sh/log.txt