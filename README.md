# autosub application

## Overview

IBM Data Replication sample implementation of a tool to automate
the recovery of subscriptions in the event of DDL operation
over the source tables.

This tool uses CHCCLP embedded scripting and CDC engine commands to do its job.
It periodically monitors the defined list of subscriptions.
If it detects a recoverable failure, it tries to repair it.

## News

Version 2.0 has been created to avoid the data loss.
There are still situations which cannot be recovered without the Refresh operation,
the main example being the DDL -> DML -> DDL sequence on a single table.
Some of the CDC engines (e.g. PostgreSQL and MSSQL) are just not DDL-aware
enough to handle DDL operations on source tables without a Refresh.

By adding the *Refresh* operation to this sample, with some logic to detect
those "nonrecoverable" situations, it is now possible to support the
full set of cases, at the cost of an automated Refresh (which is also
probably not a best option for all customers).

The application has been tested on Oracle, Db2, MSSQL and PostgreSQL 
data sources. The primary tested target is Kafka, although some checks
have been done against DataStage and relational targets.

## Build

Pre-build versions are available as Releases.

Build uses Maven. Third-party (IBM CDC Access Server) jars need to be installed
into the local Maven repository with the iidr-cdc-setup-maven-libs.sh script.
Required Eclipse libraries are grabbed from central Maven repository,
their versions should be validated against the particular Access Server version.

## Configuration

Global application settings are specified in the cdc-autosub.properties file.
These settings cannot be changed without the tool restart.

The list of monitored subscriptions, plus the source and target datastore properties,
are defined in the XML files, which are combined into a single configuration
with the special mini-tool, which signals the main tool to reload its settings.
This means that the list of subscriptions, datastores, and their addresses/names/commands
can be altered at runtime, without the need for complete tool restart.

All configuration examples are available in the "run" subdirectory.

CDC engine commands are being run through the passwordless remote 
shell invocation, so some sort of SSH needs to be installed and configured.
This tool expects to be able to run CDC_DIR/bin/dmSOMETHING commands 
with the CDC software owner used to run the CDC instances.
No other commands are being executed, and therefore, SSH access can be
restricted to the necessary CDC commands only.

For Windows SSH access, a built-in function may be used, or, for older
Windows versions, one can use the following software:
https://github.com/PowerShell/openssh-portable/releases
