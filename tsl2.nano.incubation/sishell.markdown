# SI-Shell (Thomas Schneider 2015)

{toc}

[doc/sishell-welcome.png]

[doc/sishell-toolbox.png]

The *SI-Shell* (Structured Input Shell) is a simple self-configurable console application. It is part of the tsl2.nano framework. A structured visible menu guides the user through simple keyboard input through a tree of panels. Only five types of items are available:

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
- selectors for files, content of files, class members, properties, csv-files etc.
- administration modus to create/change/remove items, change terminal properties

## The Shell

### Commands

[doc/sishell-help.png]

#### Administration

[doc/sishell-admin.png]

## The Items

### Selectors

### Actions

## Styling and Layout

## Tutorial
