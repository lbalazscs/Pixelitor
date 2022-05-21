# Pixelitor Roadmap

The purpose of this document is to outline possible major future changes to Pixelitor. It's incomplete, a work in progress. You can ask questions or give feedback in the [Discord server](https://discord.gg/SXaxYnBSTv).

## Non-destructive Editing

- A smart object should be able to have more than one smart filter.
- Smart filters should be able to have their own layer masks.
- Adjustment layers should be implemented.
- A shape layer should be able to contain more than one shape.
- At the moment, there are very few shapes to choose from, and they are encoded in the PXC files in a way that limits future extensibility. The PXC-encoding of the shapes should be changed to allow extending it to multiple shapes.

## Using C/C++ Libraries

A lot of open-source code for image processing exists only as C/C++ libraries. To access C++ libraries, it may be worth using the [Foreign Function & Memory API](https://openjdk.java.net/jeps/424),  even though it will still be in Preview state in Java 19.
Some libraries we could use:
- [ImageMagick](https://imagemagick.org/) - this is already used for import/export, but only through the command line, which is slower than necessary because all images are encoded in PNG before sending them to ImageMagick.
- [G'MIC](https://gmic.eu/) - it has hundreds of image filters, and an embedded interpreter for creating new filters.
- [GEGL](https://gegl.org/) - many of Gimp's filters are implemented here.

## PXC Format Changes

PXC is the format that can save all Pixelitor features. The current format is based on Java serialization. This was easy to implement, but it has the some disadvantages:

- Some parts of the code are difficult to change in a way that keeps them compatible.
- It would be very difficult to import pxc files into non-Java programs.

It's possible to change the format of the saved files while still being able to read the old PXC files. There could be a version that can read both formats and can convert the old files to the new format.
One option is to make the new format an extension of [OpenRaster](https://www.openraster.org/). This would mean that many other image editors could at least partially read the new PXC files.

## Preview for the PXC Format

Currently, the file open dialog doesn't show preview thumbnails of PXC files. A thumbnail image should be added to PXC files (loading and evaluating the full PXC file could take a lot of time if it has many smart filters). The PXC version number could be incremented to indicate that this PXC file has a thumbnail before the serialized data, and then old files could still be loaded compatibly.
A completely new PXC format should also include a thumbnail image, for example as a separate file inside a zip file, as the OpenRaster format does.

## Macros and Logs

Recording and replaying macros would allow automating repetitive tasks.

Logging would make it easier to report errors. Ideally, logging would be in the form of automatically replayable macros.

## Command-line Usage

Pixelitor should be usable from the command line, without starting the GUI. The user should be able to open a file, run a filter and save the result. This also has to do with the macros, because it would be nice to be able to run macros from the command line.

## Flat Theme

The [Flat Look and Feel](https://github.com/JFormDesigner/FlatLaf) could be a theme option. This is how Pixelitor would look with it:

![Flat Dark Theme](roadmap_images/pixelitor_flat.png)

## Seeds in Presets

Currently, the filter presets don't save the seed(s) used for the different types of randomness, and therefore the image can look different after applying a preset.
This is a problem especially when using smart filters, because they also contain filter presets under the hood.

## Multiline and Styled Text

It would be nice if text layers could use multiple lines of text, and even better if each character could be styled individually. The first can be achieved by using a [JTextArea](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/JTextArea.html), and the second by using a [JTextPane](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/JTextPane.html).

## Plugins

It should be possible to extend Pixelitor's functionality with plugins. Filter plugins could add more filters and file plugins could add more input/output formats.

## Transform Tool

A transform tool would be very useful to interactively change the image. As a first step, at least [affine transformations](https://en.wikipedia.org/wiki/Affine_transformation) (translate, rotate, scale, shear) should be possible. The existing ```TransformBox``` class already can create ```AffineTransform``` objects to manipulate shapes and paths, this should be extended to images and selections. The functionality could be part of the Move Tool, or it could be a separate tool.
