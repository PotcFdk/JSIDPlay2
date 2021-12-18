#!/bin/bash
pkill -f ui.tools.FingerPrintingCreator
java -server -classpath ./${project.artifactId}-${project.version}.jar ui.tools.FingerPrintingCreator  "$@"
