#! /bin/sh

set -e
set -u

JAVA_CMD=java
JAVA_FLAGS="-Xmx256M -Xms256M"
AUTOSUB_IIDR=lib-iidr

AUTOSUB_DIR=`dirname "$0"`

if [ -f "$AUTOSUB_DIR"/autosub-config.sh ]; then
    source "$AUTOSUB_DIR"/autosub-config.sh
fi

cd $AUTOSUB_DIR
$JAVA_CMD -classpath "classes:lib/*:$AUTOSUB_IIDR/*" $JAVA_FLAGS com.ibm.idrcdc.autosub.Worker $@

# End Of File