package pixelitor.filters.gui;

public interface RangeBasedOnImageSize {
    /**
     * Sets the maximum and default parameters according to the image size
     *
     * @param ratio the new maximum will be
     * @return itself
     */
    AbstractGUIParam adjustRangeToImageSize(double ratio);
}
