#!/bin/sh

setup_git() {
  git config --global user.email "mboysan.git@gmail.com"
  git config --global user.name "mboysan"
}

get_branch() {
  cd $HOME
  git clone --depth=1 --branch=repository https://github.com/mboysan/jbizur.git jbizur-repository
  cd jbizur-repository
}

commit_files() {
  cp -avr $HOME/.m2/repository/ee/ut/jbizur/* ./ee/ut/jbizur/
  git add --all
  git commit --message "Travis build jbizur: $TRAVIS_BUILD_NUMBER"
}

upload_files() {
  git remote rm origin
  git remote add origin https://${GITHUB_TOKEN}@github.com/mboysan/jbizur.git
  git push --quiet origin repository
}

setup_git
get_branch
commit_files
upload_files