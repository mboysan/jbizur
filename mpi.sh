#!/bin/sh

# Used to test the MPI functionality locally.

# build java application
mvn clean install -DskipTests

# go to target and run ee.ut.jbizur.MPIMain class
cd ./target
count=5
mpirun --oversubscribe -np $count java -cp *jar-with-dependencies.jar ee.ut.jbizur.MPIMain 2 false

# arguments:
# arg1 = the group id of the mpi nodes.
# arg2 = true if the system profiling is enabled, false otherwise.