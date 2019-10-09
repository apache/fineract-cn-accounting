#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# Documentation: https://cwiki.apache.org/confluence/display/FINERACT/Fineract-CN+Artifactory

#Exit immediately if a command exits with a non-zero status.
set -e
EXIT_STATUS=0

# Builds and Publishes a SNAPSHOT
function build_snapshot() {
  echo -e "Building and publishing a snapshot out of branch [$TRAVIS_BRANCH]"
  ./gradlew -PartifactoryRepoKey=libs-snapshot-local -DbuildInfo.build.number=${TRAVIS_COMMIT::7} artifactoryPublish --stacktrace || EXIT_STATUS=$?
}

# Builds a Pull Request
function build_pullrequest() {
  echo -e "Building pull request #$TRAVIS_PULL_REQUEST of branch [$TRAVIS_BRANCH]. Won't publish anything to Artifactory."
  ./gradlew publishToMavenLocal rat || EXIT_STATUS=$?
}

# Builds Forked (non-apache) Repository
function build_fork() {
  echo -e "Building branch $TRAVIS_BRANCH of fork $TRAVIS_REPO_SLUG. Won't publish anything to Artifactory."
  ./gradlew publishToMavenLocal rat || EXIT_STATUS=$?
}

# For other branches we need to add branch name as prefix
function build_otherbranch() {
  echo -e "Building a snapshot out of branch [$TRAVIS_BRANCH] and publishing it with prefix '${TRAVIS_BRANCH}-SNAPSHOT'"
  ./gradlew -PartifactoryRepoKey=libs-snapshot-local -DbuildInfo.build.number=${TRAVIS_COMMIT::7} -PexternalVersion=${TRAVIS_BRANCH}-SNAPSHOT artifactoryPublish --stacktrace || EXIT_STATUS=$?
}

# Builds and Publishes a Tag
function build_tag() {
  echo -e "Building tag [$TRAVIS_TAG] and publishing it as a release"
  ./gradlew -PartifactoryRepoKey=libs-release-local -PexternalVersion=$TRAVIS_TAG artifactoryPublish --stacktrace || EXIT_STATUS=$?

}

echo -e "TRAVIS_REPO_SLUG=$TRAVIS_REPO_SLUG"
echo -e "TRAVIS_BRANCH=$TRAVIS_BRANCH"
echo -e "TRAVIS_TAG=$TRAVIS_TAG"
echo -e "TRAVIS_COMMIT=${TRAVIS_COMMIT::7}"
echo -e "TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST"

if [[ $TRAVIS_REPO_SLUG != apache* ]]; then
  build_fork
elif [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  build_pullrequest
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" != "$BUILD_SNAPSHOTS_BRANCH" ] && [ "$TRAVIS_TAG" == "" ]; then
  build_otherbranch
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "$BUILD_SNAPSHOTS_BRANCH" ] && [ "$TRAVIS_TAG" == "" ]; then
  build_snapshot
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" != "" ]; then
  build_tag
else
  echo -e "WARN: Unexpected env variable values => Branch [$TRAVIS_BRANCH], Tag [$TRAVIS_TAG], Pull Request [#$TRAVIS_PULL_REQUEST]"
  ./gradlew clean build
fi

exit ${EXIT_STATUS}
