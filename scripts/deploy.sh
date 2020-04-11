#!/bin/sh

PROJ_VERSION=$(mvn -q \
    -Dexec.executable=echo \
    -Dexec.args='${project.version}' \
    --non-recursive \
    exec:exec)

export PROJ_VERSION

# setup git
git config --global user.email "mboysan.git@gmail.com"
git config --global user.name "mboysan"

# get branch
cd $HOME
git clone --depth=1 --branch=repository https://github.com/mboysan/jbizur.git jbizur-repository
cd jbizur-repository

# clean dir
rm -vr ./ee/ut/jbizur/*

# commit files
cp -avr $HOME/.m2/repository/ee/ut/jbizur/* ./ee/ut/jbizur/
git add --all
git commit --message "Travis build jbizur: $TRAVIS_BUILD_NUMBER"

# upload files
git remote rm origin
git remote add origin https://${GITHUB_TOKEN}@github.com/mboysan/jbizur.git
git push --quiet origin repository

# tag and push
git tag v$PROJ_VERSION.$TRAVIS_BUILD_NUMBER
git push origin --tags