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

CDC Access Server installation is a pre-requisite for AutoSub,
because the Access Server libraries are used to connect to the
actual Access Server (locally or remotely) and manage the
replication.

There are some required environment variables which are configured
through autosub-config.sh file. Settings to be configured there:
* JAVA_CMD - command to run Java;
* JAVA_FLAGS - Java runtime parameters, like memory settings;
* AUTOSUB_IIDR - path to CDC Access Server or Management Console
  library directory containing the CDC product jar files.
See the provided file autosub-config.sh.sample for example contents.

Global AutoSub settings are defined in autosub.properties file.
See the provided file autosub.properties.sample for the list
of parameters, their definition and example values.

It is necessary to create a separate IBM CDC Access Server account
for each instance of this AutoSub tool connecting the Access Server.
Using a separate Access Server account allows the tool to operate
without interventing with the work of human administrators.
Connection credentials are specified in the autosub.properties file.

CDC engine commands are being run through the passwordless remote 
shell invocation, so some sort of SSH or RSH needs to be installed 
and configured:
* SSH/RSH server needs to be running on the hosts where the CDC 
  source and target agents are running;
* the corresponding SSH or RSH client tool (or tools) needs
  to be installed on the host where the AutoSub application
  is running.

AutoSub expects to be able to run CDC_DIR/bin/dmSOMETHING commands 
with the CDC software owner used to run the CDC instances.
No other shell commands are being executed, and therefore, 
remote shell access can be restricted to the necessary 
CDC commands only.

For Windows SSH access, a built-in function of Windows 10 or 
Windows Server 2019 may be used, or, for older
Windows versions, one can use the following software:
https://github.com/PowerShell/openssh-portable/releases


3. REPLICATION SETTINGS AND OPERATIONS

Datastore connections and the list of monitored subscriptions
are specified in the XML files located by default in the "subs"
subdirectory. Examples for different datastore types are provided
in "subs-sample" subdirectory.

AutoSub application reads its current settings from the single active
configuration file. This file is typically not edited manually,
instead, it is generated automatically through the "autosub-refresh.sh"
command from the set of source files described above. This allows
to safely edit the current configuration without affecting
the operations of the running AutoSub application.

AutoSub is started through the "autosub-worker.sh" script.
It can be safely shut down though the "autosub-shutdown.sh" script.
Terminating the tool by entering Control-C or sending other signals is not
recommended, as this can leave the recovery and monitoring procedures 
uncompleted.
