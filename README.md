
# autosub application

Example implementation of a tool to automate the recovery of subscriptions in the event of DDL operation over the source tables. Uses CHCCLP embedded scripting to do its job.

Version 2.0 has been created to avoid the data loss.
There are still situations which cannot be recovered without the *Refresh* operation, the main example being the DDL -> DML -> DDL sequence on a single table.
By adding the *Refresh* operation to this sample, with some logic to detect those "nonrecoverable" situations, it is possible to support the full set of cases, at the cost of an automated Refresh (which is also probably not a best option for all customers).

Build uses Maven, third-party (Access Server) jars need to be installed into the local Maven repository with the iidr-cdc-setup-maven-libs.sh script.
Required Eclipse libraries are grabbed from central Maven repository, versions should be validated.

Application settings are specified in the cdc-autosub.properties file.
Each monitored subscription has its own configuration file with its name, source and target datastore names, and the list of columns replicated.
Staging store cleanup is implemented by calling an external script (normally clear-staging-store.sh), which gets the datastore name as its argument. It is expected that this script connect to the CDC agent's host through SSH and runs dmclearstagingstore tool.

All configuration examples are available in the "run" subdirectory.
