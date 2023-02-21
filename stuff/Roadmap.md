# Pixelitor Roadmap

The purpose of this document is to outline some of the significant changes that could happen to Pixelitor in the future. 
It's incomplete, a work in progress. It was last updated in February 2023.

## Non-destructive Editing

- A shape layer should be able to contain more than one shape.
- At the moment, there are very few shapes to choose from, and they are encoded in the PXC files in a way that limits future extensibility. The PXC encoding of the shapes should be changed to allow for multiple shapes to be added in the future.
- Users should be able to reorder smart filters and layers in layer groups by drag-and-drop, rather than relying solely on keyboard shortcuts.
- Layer groups should be collapsible.

## Using C/C++ Libraries

A lot of open-source code for image processing exists only as C/C++ libraries. 
To access C++ libraries, it may be worth using the [Foreign Function & Memory API](https://openjdk.java.net/jeps/424),  even though it's still in Preview state in Java 19.
Some libraries we could use:
- [ImageMagick](https://imagemagick.org/) - this is already used for import/export, but only through the command line, which is slower than necessary because all images are encoded in PNG before sending them to ImageMagick.
- [G'MIC](https://gmic.eu/) - it has hundreds of image filters, and an embedded interpreter for creating new filters.
- [GEGL](https://gegl.org/) - many of Gimp's filters are implemented here.

## PXC Format Changes

PXC is the format that can save all Pixelitor features. The current format is based on Java serialization. Although this was easy to implement, it has some disadvantages:

- Some parts of the code are hard to change without affecting compatibility.
- Importing pxc files into non-Java programs would be very difficult.

It's possible to change the format of the saved files while still being able to read the old PXC files. There could be a version that can read both formats and convert the old files to the new format.
An option would be to make the new format an extension of [OpenRaster](https://www.openraster.org/), which would allow other image editors to read at least some parts of the new PXC files.

## Preview for the PXC Format

The file open dialog doesn't currently show preview thumbnails of PXC files.
A thumbnail image should be added to PXC files, because loading and evaluating a full PXC file can take a lot of time if it has many smart filters.
The PXC version number could be incremented to indicate that a thumbnail is present before the serialized data.
This would allow old files to still be loaded compatibly.

A completely new PXC format should also include a thumbnail image, for example as a separate file inside a zip file, as the OpenRaster format does.

## Macros and Logs

Recording and replaying macros would allow automating repetitive tasks.

Logging would make it easier to report errors.
Ideally, logging would generate automatically replayable macros, which would enable developers to quickly reproduce and debug issues.

## Command-line Usage

Pixelitor should be usable from the command line, without starting the GUI.
Users should be able to open a file, run a filter and save the result.
Macros could also be executed via the command line.

## Multiline and Styled Text

It would be nice if text layers could use multiple lines of text, and even better if each character could be styled individually. The first can be achieved by using a [JTextArea](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/JTextArea.html), and the second by using a [JTextPane](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/JTextPane.html).

## Plugins

Pixelitor could be improved by implementing a plugin architecture that would allow for the addition of new filters, tools, and file formats.

## Transform Tool
            
A transform tool could be useful to interactively change the image. 
As a first step, at least [affine transformations](https://en.wikipedia.org/wiki/Affine_transformation) (translate, rotate, scale, shear) should be possible. 
The existing ```TransformBox``` class already can create ```AffineTransform``` objects to manipulate shapes and paths, this should be extended to images and selections. 
The functionality could be part of the Move Tool, or it could be a separate tool.
