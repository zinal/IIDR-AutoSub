AutoSub application

1. OVERVIEW

AutoSub is the sample implementation of a tool to automate 
the recovery of subscriptions of IBM Data Replication product
in the event of DDL operation over the replicated tables
in the source databases.

This tool uses CHCCLP embedded scripting and CDC engine
commands to do its job. It periodically monitors the defined
list of subscriptions. If it detects a recoverable failure
of the replication process (subscription), it tries to fix it.


2. INSTALLATION AND BASIC SETUP

IBM CDC Access Server installation is a pre-requisite for AutoSub,
because AutoSub uses the Access Server libraries to connect to
the actual Access Server (locally or remotely) and to manage
the subscriptions.

There are some required environment variables which are configured
in the autosub-config.sh file. Settings to be configured there:
* JAVA_CMD - command to start Java applications;
* JAVA_FLAGS - Java runtime parameters, like memory settings;
* AUTOSUB_IIDR - path to CDC Access Server or Management Console
  library directory containing the CDC product jar files.
See the provided file autosub-config.sh.sample for example contents.

Global AutoSub settings are defined in autosub.properties file.
See the provided file autosub.properties.sample for the list
of parameters, their definition and example values.

Access Server connection parameters, including the login credentials,
are specified in the autosub.properties file.

It is necessary to create a separate Access Server account
for each instance of AutoSub tool connecting the Access Server.
Using a separate Access Server account allows the tool to operate
without interventing with the work of human administrators
and other tools.

CDC engine commands are being run through the passwordless remote 
shell invocation, so some sort of SSH or RSH needs to be installed 
and configured:
* SSH/RSH server needs to be running on the hosts where the CDC 
  source and target agents are running;
* the corresponding SSH or RSH client tool (or tools) needs
  to be installed on the host where the AutoSub application
  is running;
* keys, certificates and/or other authentication settings should
  allow AutoSub to run CDC engine commands, as described below.

AutoSub expects to be able to run CDC_DIR/bin/dmSOMETHING commands 
with the CDC software owner used to run the CDC instances.
No other shell commands are being executed, so the remote shell
access can be restricted to the required CDC commands only.

For Windows SSH access, a built-in function of Windows 10 or 
Windows Server 2019 may be used, or, for older Windows versions,
one can use the following software:
https://github.com/PowerShell/openssh-portable/releases


3. OPERATIONS

Datastore connections and the list of monitored subscriptions
are defined in the XML files, which are located by default in
the "subs" subdirectory. Examples for different datastore
types are provided in "subs-sample" subdirectory. Please see
an overview of the format and contents below in section [4].

Before starting AutoSub for the first time, datastore and
subscription settings need to be merged into a single active
configuration file by running the "autosub-refresh.sh" command.

Any changes to the original configuration files are not
used until they are merged to the active configuration file.
This allows to safely edit the configuration without affecting
the operations of the running AutoSub.

AutoSub application is started through the "autosub-worker.sh"
script. On startup it reads the settings from the properties
file and from the active configuration file, connects to
the Access Server, and checks the initial state of the configured
CDC agents and subscriptions. After that it enters the regular
monitoring loop, fixing the subscription state when needed
and possible.

Any changes to the configuration can be re-applied to the running
AutoSub application at the desired point in time by running
the "autosub-refresh.sh" command.

AutoSub can be safely shut down though the "autosub-shutdown.sh"
script. Terminating the tool by entering Control-C or by sending
other signals is not recommended, as this can leave the recovery
and monitoring procedures in the incomplete state, requiring
the manual recovery.

The recovery actions performed to repair the failed subscriptions
are logged into the recovery report files, which are placed into
the directory specified by the "subs.recovery" global parameter
("recovery", by default). For each recovery session a new
recovery report file is created. The recovery report contains
all the CHCCLP and CDC engine commands executed, in the order
of their execution. This enables the administrators to have
the exact definition of all the automated configuration changes
applied.


4. DATASTORES AND SUBSCRIPTIONS DEFINITION

For each CDC source or target engine (e.g. each CDC "datastore")
which needs to be monitored by AutoSub, a separate file
is typically created.

Datastore configuration consists of the parameters for working
with the CDC particular agent:
* logical datastore name (should match the value shown in
  the Management Console);
* operation mode (Source, Target or Both);
* path to CDC agent software installation;
* CDC instance name (the name of the relevant subdirectory
  in the instance storage path);
* SSH or RSH client command for executing CDC engine commands
  (including the login name, host name, non-standard port
  if used, etc.).

Please see below the typical examples of the source engine
definition for Db2, Oracle and Microsoft SQL Server:

    <idrcdc-engine name="DB2SRC1" mode="Source">
      <cdc-rsh>ssh cdcuser@db2host1</cdc-rsh>
      <cdc-path>/opt/IBM/CDC/engine-db2</cdc-path>
      <cdc-instance>DEMO1</cdc-instance>
    </idrcdc-engine>

    <idrcdc-engine name='ORASRC1' mode='Source'>
        <cdc-rsh>ssh cdcuser@orahost1</cdc-rsh>
        <cdc-path>/opt/IBM/CDC/engine-ora</cdc-path>
        <cdc-instance>ORA1</cdc-instance>
    </idrcdc-engine>

    <idrcdc-engine name='MSSQL1' mode='Source'>
        <cdc-rsh>ssh Administrator@wind</cdc-rsh>
        <cdc-path>C:\IBM\cdc-mssql</cdc-path>
        <cdc-instance>repl</cdc-instance>
    </idrcdc-engine>

Please see below the typical examples of the target engine
definition for Kafka and DataStage:

    <idrcdc-engine name='KAFKA1' mode='Target'>
        <cdc-rsh>ssh cdcuser@kafko</cdc-rsh>
        <cdc-path>/home/cdcuser/engine-kafka</cdc-path>
        <cdc-instance>kafka1</cdc-instance>
    </idrcdc-engine>

    <idrcdc-engine name='DS1' mode='Target'>
        <cdc-rsh>ssh cdcuser@dseng1</cdc-rsh>
        <cdc-path>/home/cdcuser/engine-ds</cdc-path>
        <cdc-instance>DS1</cdc-instance>
    </idrcdc-engine>

The list of subscriptions to be monitored is typically defined
in the configuration XML file for the source CDC agent.
For each subscription the configuration includes:
* the names of the source and target engines;
* the subscription names;
* some optional flags.
Please note that subscription names are only unique in the context
of each CDC engine, and may have duplicates in the other agents.

Please see below the example definitions of the subscriptions
to be monitored by AutoSub:

  <idrcdc-subscription name='O2K2' source='WRK1' target='KAFKA1' />
  <idrcdc-subscription name='O2M1' source='WRK1' target='META'
        skipNewBlobs='true' />

  <idrcdc-subscription name="D2K1" source="DB2SAM" target="KAFKA1"
        skipNewBlobs="false" />

  <idrcdc-subscription name="M2K1" source="MSSQL1" target="KAFKA1" 
        skipNewBlobs="true" />

There are two optional flags supported now:
* skipNewBlobs (true/false, default false) - if set to true, AutoSub
  will not automatically set the newly added BLOB/CLOB/LONG columns
  to be replicated;
* refreshMode (Never/Allow/Force, default Allow) - whether and when
  the Refresh is considered as a way to repair the subscription.
