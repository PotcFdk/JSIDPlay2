#!/bin/bash
cd ~Downloads/${project.artifactId}-${project.version}
export LD_LIBRARY_PATH=~/Downloads/${project.artifactId}-${project.version}
java -jar ${project.artifactId}-${project.version}.jar
