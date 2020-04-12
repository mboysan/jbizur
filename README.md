[![Build Status](https://api.travis-ci.com/mboysan/jbizur.svg?branch=master)](https://travis-ci.com/mboysan/jbizur)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ee.ut.jbizur%3Ajbizur-parent&metric=alert_status)](https://sonarcloud.io/dashboard?id=ee.ut.jbizur%3Ajbizur-parent)

#### Disclaimer
This is still a work in progress! This repository is only
created for educational purposes (specifically, for my 
masters thesis).


### Introduction

This repository is created to provide a distributed in-memory
database solution written in Java that uses the Bizur consensus algorithm (see paper at: [https://arxiv.org/pdf/1702.04242.pdf](https://arxiv.org/pdf/1702.04242.pdf))


### Description

The repository is divided into 4 main branches.

* **master:** Contains the java implementation of the database.
* **examples:** Contains some basic examples of how to use the database.
* **repository:** Contains the binaries of the built java libraries including mpi and jbizur (this project).
* **benchmark:** A separate project that is used to compare the performance of different in-memory database solutions with the bizur db.


### Where to Start

Please see the ```examples``` branch to see the basic usage 
of this distributed database.

To run the examples you need:
* Java 8
* Maven

Checkout the ```examples``` branch and in the main dir 
compile with command:
```
$ mvn clean install
```
Import the code using your favourite IDE and use it as you
wish.


### Notes and TODO

* See the [issues](https://github.com/mboysan/jbizur/issues)
section for more details on what will be implemented next.
* **NB!** Since the code is constantly evolving, comments 
will be added to the implementation details once the 
project is in finalization state.
