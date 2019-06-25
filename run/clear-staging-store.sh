#! /bin/sh

# USAGE: clear-staging-store.sh DSNAME

DSNAME="$1"
if [ -z "$DSNAME" ]; then
  echo "USAGE: clear-staging-store.sh DSNAME" >&2
  exit 1
fi

DSLOGIN=cdcuser
DSHOST=dsc1
DSINST=$DSNAME
DSPATH=/opt/IBM/CDC/Oracle

# The variables above can be configured depending on the DSNAME passed.
# Passwordless SSH should be set up, allowing to run dmclearstagingstore

ssh "$DSLOGIN"@"$DSHOST" "$DSPATH/bin/dmclearstagingstore -I $DSINST"

# End Of File
