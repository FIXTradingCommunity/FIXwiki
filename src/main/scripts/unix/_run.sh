#!/bin/sh
# Used by other command files.

#
# setup the classpath
#
cp=../../../../target/cameronedge-fixwiki.jar

java -version
echo on

java -Xmx256M -classpath "$cp" $*
