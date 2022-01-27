#!/bin/bash
pkill -f server.restful.JSIDPlay2Server
java -server -classpath ./${project.artifactId}-${project.version}.jar server.restful.JSIDPlay2Server "$@"

# E.g. MySQL:
# --whatsSIDDatabaseUrl "jdbc:mysql://127.0.0.1:3306/hvscXX?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true"