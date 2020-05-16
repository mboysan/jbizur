#!/bin/bash

wget https://github.com/sormuras/bach/raw/master/install-jdk.sh
source ./install-jdk.sh --verbose --feature ea
echo JAVA_HOME = ${JAVA_HOME}
echo PATH = ${PATH}
ls ${JAVA_HOME}
java -version