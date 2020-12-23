AutoSub application

IBM Data Replication sample implementation of a tool to automate 
the recovery of subscriptions in the event of DDL operation 
over the source tables.

This tool uses CHCCLP embedded scripting and CDC engine commands to do its job. 
It periodically monitors the defined list of subscriptions. 
If it detects a recoverable failure, it tries to repair it.

Environment variables are configured through autosub-config.sh.
See the provided file autosub-config.sh.sample for example values.

Global settings are defined in autosub.properties file.
See the provided file autosub.properties.sample for the list of parameters,
their definition and example values.

It is necessary to create a separate IBM CDC Access Server account
for each instance of this AutoSub tool connecting the Access Server.
Using a separate Access Server account allows the tool to operate
without interventing with the work of human administrators.
Connection credentials are specified in the autosub.properties file.

Datastore connections and the list of monitored subscriptions are specified
in the XML files located by default in the "subs" subdirectory.
Examples for different datastore types are provided in "subs-sample".

Multiple XML files are combined into a single active configuration
through the "autosub-refresh.sh" command.
This allows to safely edit the current configuration without affecting
the operations of the running monitor copy.

The tool is started through the "autosub-worker.sh" script.
It can be safely shut down though the "autosub-shutdown.sh" script.
Terminating the tool by entering Control-C or sending other signals is not
recommended, as this can leave the recovery and monitoring procedures 
uncompleted.

CDC engine commands are being run through the passwordless remote 
shell invocation, so some sort of SSH needs to be installed and configured.
This tool expects to be able to run CDC_DIR/bin/dmSOMETHING commands 
with the CDC software owner used to run the CDC instances.
No other commands are being executed, and therefore, SSH access can be
restricted to the necessary CDC commands only.

For Windows SSH access, a built-in function of Windows 10 or 
Windows Server 2019 may be used, or, for older
Windows versions, one can use the following software:
https://github.com/PowerShell/openssh-portable/releases
