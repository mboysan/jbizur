#!/bin/bash

echo "switching to log dir: ${1}"
cd $1
LOGFILE="$(ls -t log* | head -1)"
echo "uploading ${LOGFILE} ..."
curl -s --upload-file $LOGFILE https://transfer.sh/log.txt | tee out.txt
cat out.txt