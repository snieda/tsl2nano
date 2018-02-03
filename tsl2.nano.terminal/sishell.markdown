# SI-Shell (Thomas Schneider 2015)

{toc}

[doc/sishell-welcome.png]

[doc/sishell-toolbox.png]

The *SI-Shell* (Structured Input Shell) is a simple self-configurable console application. It is part of the *tsl2.nano* framework. A structured visible menu guides the user through simple keyboard input through a tree of panels. Only five types of items are available:

- Container: holding items of any type
- Option   : options of a container to be on or off
- Input    : textual user input
- Action   : any action to be runnable with a given set of arguments, defined by other items
- Selector : an extended Container to select one item. Several extensions are available

Short description:

[doc/sishell-help.png]

## Features

- simple input and output on small screens
- input constraints check
- configuration over xml
- result will be written to a property map
- tree nodes can be selected through numbers or names (the first unique characters are enough)
- batch mode possible
- macro recording and replaying
- simplified java method calls
- variable output sizes and styles
- workflow conditions: items are active if an optional condition is true
- if an item container (a tree) has only one visible item (perhaps filtered through conditions), it delegates directly to that item
- actions get the entire environment properties (including system properties) on calling run().
- sequential mode: if true, all tree items will be asked for in a sequential mode.
- show ascii-pictures (transformed from pixel-images) for an item (description must point to an image file)
- extends itself downloading required jars from network (if {@link #useNetworkExtension} ist true)
- schedule mode: starts a scheduler for an action
- selectors for directories, files, content of files, class members, properties, csv-files etc.
- administration modus to create/change/remove items, change terminal properties

## The Shell

### Start

Start the shell with:
*java -jar sishell-<version-number>.jar*

On start, the SI-Shell will create its environment directory *.sishell* to store all configuration files and loaded libraries. If you set the system property _env.user.home_, the user.home directory will be used as parent directory for the _.sishell_ directory.

### Commands

[doc/sishell-help.png]

#### Administration

##### Adding a new Item

##### Changing an Item

##### Changing the constraints of an Item

##### Removing an Item

##### Changing Shell Properties


### Styling and Layout

[doc/sishell-admin.png]

## The Items

### Selectors

### Actions

### Variables and Definitions

#### References

### Styling and Layout

#### The Charset

If you are under linux, the standard character encoding is UTF-8. The SI-Shell uses the character Cp1252 to print a frame with bars. If you want to see that frame under a system like linux, call the SI-Shell with the following system property:
- D*file.encoding=Cp1252*
- or D*file.encoding=ISO8859-1*

So, start the SI-Shell with:
*java -Dfile.encoding=Cp1252 -jar sishell-x.y.z.jar*

where x.y.z is the used version number.

## Creating a SI-Shell programmatically

Creating an Ant Task:

==============================================================================
Container ant = new Container("Ant", null);
ant.add(new AntTaskSelector("task", "Jar", "pack given filesets to zip"));
ant.add(new PropertySelector<String>("properties", "ant task properties", MapUtil.asMap(new TreeMap(), "destFile", "mynew.jar")));
ant.add(new Input("filesets", "./:{**/*.*ml}**/*.xml;${user.dir}:{*.txt}", "filesets expression", false));
ant.add(new Action(AntRunner.class, "runTask", "task", "properties", "filesets"));
root.add(ant);
==============================================================================


Creating some file operations:

==============================================================================
Container file = new Container("File-Operation", null);
file.add(new DirSelector("directory", "${user.dir}", ".*"));
file.add(new Input("file", "**/[\\w]+\\.txt", "regular expression (with ant-like path **) as file filter"));
file.add(new Input("destination", "${user.dir}", "destination directory for file operations"));
file.add(new Action("Details", FileUtil.class, "getDetails", "file"));
file.add(new FileSelector("List", "file", "directory"));
file.add(new Action("Delete", FileUtil.class, "foreach", "directory", "file", Action.createReferenceName(FileUtil.class, "DO_DELETE")));
file.add(new Action("Copy", FileUtil.class, "foreach", "directory", "file", Action.createReferenceName(FileUtil.class, "DO_COPY")));
file.add(new MainAction("Imageviewer", AsciiImage.class, "file", "image.out", "sishell.width",
    "sishell.height"));
root.add(file);
==============================================================================


Creating a SI-Shell with batch commands:

==============================================================================
InputStream in = SIShell.createBatchStream("Printing", "jobname", "test", "10", "", ":quit");
new SIShell(root, in, System.out, 79, 22, 1, defs).run();
==============================================================================

## Tutorial


## TODOs

- (x) Beim Starten simple-xml Fehler abfangen und vernünftige Fehlermeldung wegen fehlendem Netz anzeigen
- (v) env.user.home Parameter um Verzeichnis in user.home anzulegen
- (v) Charset als Parameter -Dfile.encoding (in Hilfe beschreiben)
- (v) save to xml after administration
- (v) move item
- (v) toobox: file.destination generic
- (x) file.list, file.copy, file.delete: NoSuchMethod
- Fehlermeldungen abkürzen
- (v) use messages-bundle (java + toolbox)
- action sequence as collection of other actions
- (v) CRs in Title durch spaces ersetzen
- (x) NetUtil.get hinzufuegen
- (x) PropertySelector: show key/values...
- (v) Tree --> Container
