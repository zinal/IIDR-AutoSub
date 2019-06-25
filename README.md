
# autosub application

Example implementation of a tool to automate the recovery of subscriptions in the event of DDL operation over the source tables. Uses CHCCLP embedded scripting to do its job.

Data loss is possible (missing changes on target) between the point of the first DDL operation and the point of subscription re-start.
By re-programming this sample to use *Refresh* instead of *Mark capture point* it is possible to avoid the data loss, at the cost of an automated Refresh (which is also probably not a best option for all cases).

Build uses Maven, third-party (Access Server) jars need to be installed into the local Maven repository with the iidr-cdc-setup-maven-libs.sh script.
Required Eclipse libraries are grabbed from central Maven repository, versions should be validated.

Application settings are specified in the cdc-autosub.properties file.
Each monitored subscription has its own configuration file with its name, source and target datastore names, and the list of columns replicated.
Staging store cleanup is implemented by calling an external script (normally clear-staging-store.sh), which gets the datastore name as its argument. It is expected that this script connect to the CDC agent's host through SSH and runs dmclearstagingstore tool.

All configuration examples are available in the "run" subdirectory.
