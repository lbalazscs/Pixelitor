
[![Build Status](https://travis-ci.com/lbalazscs/Pixelitor.svg?branch=master)](https://travis-ci.com/lbalazscs/Pixelitor)

This is the source code of [Pixelitor](http://pixelitor.sourceforge.net/) - an advanced Java image editor with layers, layer masks, text layers, 110+ image filters and color adjustments, multiple undo etc. 
When you start the program from an IDE, use **pixelitor.Pixelitor** as the main class.
It requires Java 11+ to compile.

# Building Pixelitor from the command line

1. Install [Maven](https://maven.apache.org/install.html)
2. Check the Maven installation with `mvn --version`
3. Execute `mvn clean package` in the main directory (where the pom.xml file is), this will create an executable jar in the `target` subdirectory
