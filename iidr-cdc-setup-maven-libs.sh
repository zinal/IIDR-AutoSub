#! /bin/sh

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home
MVN="/Applications/NetBeans/NetBeans 8.2.app/Contents/Resources/NetBeans/java/maven/bin/mvn"

"$MVN" --version

XVER=11.4.0.2.10686

# api
# chcclp
# comms
# messaging
# online
# resources
# server


ls *.jar | while read fn; do

ARTI=`basename "$fn" .jar`
"$MVN" install:install-file -Dfile=`pwd`/"$fn" -DgroupId=com.ibm.iidr -DartifactId="$ARTI" -Dversion="$XVER" -Dpackaging=jar

done

