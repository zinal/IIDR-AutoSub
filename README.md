# autosub application

## Overview

IBM Data Replication sample implementation of a tool to automate
the recovery of subscriptions in the event of DDL operation
over the source tables.

This tool uses CHCCLP embedded scripting and CDC engine commands to do its job.
It periodically monitors the defined list of subscriptions.
If it detects the recoverable failure, it tries to repair it.

## News

Version 2.0 has been created to avoid the data loss.
There are still situations which cannot be recovered without the *Refresh* operation,
the main example being the DDL -> DML -> DDL sequence on a single table.

By adding the *Refresh* operation to this sample, with some logic to detect
those "nonrecoverable" situations, it is possible to support the full set of cases,
at the cost of an automated Refresh (which is also probably not a best option for all customers).

The application has been tested on Oracle and Db2 data sources.
PostgreSQL currently does not work due to limitations of its replication API.

## Build

Pre-build versions are available as Releases.

Build uses Maven. Third-party (Access Server) jars need to be installed
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

https://github.com/PowerShell/openssh-portable/releases
