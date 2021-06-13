#!/bin/bash
pkill -f server.restful.JSIDPlay2Server
java -server -classpath ./${project.artifactId}-${project.version}.jar server.restful.JSIDPlay2Server "$@"
