#!/bin/sh
# Used by other command files.

#
# Default directory for jars is in Maven project target directory.
# Change this if the jar is located elsewhere.
jardir=../../../../target

#
# setup the classpath.
#
cp=${jardir}/cameronedge-fixwiki-1.0.jar

#java -version
#echo java -Xmx256M -classpath "$cp" $*

java -Xmx256M -classpath "$cp" $*
