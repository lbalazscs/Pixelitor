[![Latest Release](https://img.shields.io/github/v/release/lbalazscs/pixelitor?include_prereleases)](https://github.com/lbalazscs/Pixelitor/releases)
[![Build Status](https://travis-ci.com/lbalazscs/Pixelitor.svg?branch=master)](https://travis-ci.com/lbalazscs/Pixelitor)
[![Build Status](https://github.com/lbalazscs/Pixelitor/actions/workflows/build.yml/badge.svg)](https://github.com/lbalazscs/Pixelitor/actions/workflows/build.yml)

This is the source code of [Pixelitor](https://pixelitor.sourceforge.io/) - an advanced Java image editor with layers, layer masks, text layers, 110+ image filters and color adjustments, multiple undo etc.

Contributions are welcome!

## Starting Pixelitor in an IDE

Pixelitor requires Java 16+ to compile. When you start the program from an IDE, use **pixelitor.Pixelitor** as the main
class.

## Building the Pixelitor jar file from the command line

1. OpenJDK 16+ has to be installed, and the environment variable JAVA_HOME must point to the OpenJDK installation
   directory.
2. Install [Maven](https://maven.apache.org/install.html)
3. Check the Maven installation with `mvn --version`
4. Execute `mvn clean package` in the main directory (where the pom.xml file is), this will create an executable jar in the `target` subdirectory. If you didn't change anything, or if you only changed translations/icons, then you can skip the tests by running `mvn clean package -Dmaven.test.skip=true` instead.  

## Translating the Pixelitor user interface

See [Translating](Translating.md).


