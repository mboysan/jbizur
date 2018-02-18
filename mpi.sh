#!/bin/sh

# build java application
mvn clean install

# go to target and run MainMPI class
cd ./target
mpirun java -cp *.jar MainMPI