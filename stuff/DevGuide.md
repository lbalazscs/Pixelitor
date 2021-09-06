# Developer Guide

This work-in-progress document contains information that could be useful for Pixelitor contributors.

# Understanding Buffered Images

There are many good online tutorials for Swing and for the Java 2D API, but there isn't much about the internals of BufferedImage, therefore I wrote the following overview.

A [```BufferedImage```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/BufferedImage.html) consists of a [```Raster```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/Raster.html) and a [```ColorModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/ColorModel.html).   
The ```Raster``` contains the pixels, and the ```ColorModel``` knows how to interpret the pixels as colors (more exactly ```ColorModel``` interprets the pixels as color components, and its [```ColorSpace```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/color/ColorSpace.html) interprets the color components as actual colors).

## Color Models

It's important to understand that the pixel values in a ```Raster``` are not necessarily colors, [indexed-color](https://en.wikipedia.org/wiki/Indexed_color) images (using the [```IndexColorModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/IndexColorModel.html)) store the colors in a palette, and the raster only contains indexes referring to this palette.

[```PackedColorModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/PackedColorModel.html) means that the colors are packed inside the pixel values. "Packed" always means that multiple values are packed into a larger Java primitive, and they can be retrieved by bit-shifting. For example four 8-bit values could be packed in a single 32-bit int in ARGB order, and they could be retrieved like this:

```java
    int packed = ...
    int alpha = (packed >>> 24) & 0xFF;
    int red = (packed >>> 16) & 0xFF;
    int green = (packed >>> 8) & 0xFF;
    int blue = packed & 0xFF;
```

```PackedColorModel``` has a subclass specialized for RGB, called [```DirectColorModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/DirectColorModel.html).

[```ComponentColorModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/ComponentColorModel.html) means that the colors are not indexed, but also not packed, there is a 1:1 correspondence between the color components and array elements. This works with ```ComponentSampleModel``` (see bellow).

## Rasters

A ```Raster``` consists of a [```SampleModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/SampleModel.html) and a [```DataBuffer```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/DataBuffer.html).

### Data Buffers

A ```DataBuffer``` stores the image data at the lowest level, as Java arrays. These arrays are called banks (not to be confused with bands - a band usually corresponds to a color channel, for example all the red values within an RGB image). For example [```DataBufferInt```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/DataBufferInt.html) stores the data in int arrays.

### Sample Models

Each pixel consists of a number of samples, which (together with the color model and its color space) will determine the color of the pixels. ```SampleModel``` knows how to get pixel samples from a data buffer's array(s). Sample models are about converting raw Java arrays to pixel data, which may or may not be the same as the color data (depending on the color model).

A common sample model is [```SinglePixelPackedSampleModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/SinglePixelPackedSampleModel.html), which says that every pixel value is packed in an array element. ```SinglePixelPackedSampleModel``` works well with ```PackedColorModel```.

[```MultiPixelPackedSampleModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/MultiPixelPackedSampleModel.html) can be used for single channel images, where multiple pixels are packed in a single array element, for example four 8-bit grayscale pixels are packed into a 32-bit Java int.

[```ComponentSampleModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/ComponentSampleModel.html) says that a pixel is stored in multiple array elements, for example the [```BandedSampleModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/BandedSampleModel.html) subclass could mean that all red pixels are stored in one array (band=channel), and there are different green and blue arrays. The [```PixelInterleavedSampleModel```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/PixelInterleavedSampleModel.html) subclass means that the channels are interleaved in a single array, like for example [R, G, B, R, G, B, ...]. ```ComponentSampleModel``` works well with ```ComponentColorModel```.

## The Rules of the Game

Different ```ColorModel```, ```SampleModel``` and ```DataBuffer``` subclasses can't be combined in an arbitrary way, because the combinations must be compatible. For example ```ColorModel``` has methods for creating compatible sample models and rasters.

```BufferedImage``` has ```getRGB``` and ```setRGB``` methods that hide all this complexity, but they are slow (if you want to access and modify millions of pixels). Changing the raw array(s) behind ```BufferedImage``` is much faster, but then we must understand the meaning of the array elements.

## Exercises

1. Try to read a gif or indexed png file, it should give you an image with ```IndexColorModel```, and then try to modify its palette in a small project. Also examine the type of the raster and the sample model. As a first step, you could simply print the palette's colors. When you have an ```IndexColorModel```, you can get the palette colors with ```getBlues​(byte[] b)``` and similar methods. The array size is given by ```getMapSize()```. You can't just change the color model of a buffered image, you'll have to construct a new one, with the new indexed color model and the old raster.
2. Read a small jpg image, and print the contents of the actual Java array behind the resulting buffered image.

# Understanding ```BufferedImageOp``` and the JH Labs filters

There are two ways to change a buffered image. You can call ```createGraphics()``` on it to create a [```Graphics2D```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/Graphics2D.html). This allows you to draw on the image, but what you draw will not depend on the existing pixels values. If you do this, don't forget to call ```dispose()``` on the Graphics2D object when you are done.

The more powerful way to change a buffered image is  [```BufferedImageOp```](https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/image/BufferedImageOp.html), which can create a new image based on the existing pixels. Most filters in Pixelitor either use the [JH Labs filters](http://www.jhlabs.com/ip/filters/) directly or extend its base classes. Note that Pixelitor uses an improved version of the original JH Labs filters, with added multithreading, progress tracking, bugfixes, and other improvements.

The ```BufferedImageOp``` implementations in the JDK are not very useful for Pixelitor, but ```com.jhlabs.image.AbstractBufferedImageOp``` is a good base class for image filters. ```org.jdesktop.swingx.image.AbstractFilter``` is a similar idea, but its subclasses are less powerful. The most important subclasses of ```AbstractBufferedImageOp``` are the following:

1. ```PointFilter```. A superclass for point filters. A point filter means that the output pixel depends on the input pixel and the input pixel's position, but NOT on the value of other pixels.
2. ```WholeImageFilter```. A superclass for filters which need to have the whole image in memory. The output pixel values can depend on any input pixel value.
3. ```TransformFilter```. A superclass for distorting and displacing filters. These don't change the pixel colors, but move them to different places. Subclasses must implement the ```transformInverse``` method, which returns the position in the input image, given the position in the output image. The idea is to move through all the pixel positions in the output, and for each point to determine the input position, inverting our normal thinking. Since the source position will be typically *between* two input pixels, interpolation will be needed. If the source position is outside the image, then the "edge action" determines what should happen. 


