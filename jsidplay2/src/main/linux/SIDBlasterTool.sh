#!/bin/bash
cd ~/Downloads/${project.artifactId}-${project.version}
java -jar ${project.artifactId}_sidblastertool-${project.version}.jar "$@"
