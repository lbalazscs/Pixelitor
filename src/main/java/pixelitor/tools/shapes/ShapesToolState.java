package pixelitor.tools.shapes;

/**
 * The current state of the crop tool
 *
 * TODO possibly it could be merged with CropToolState
 */
public enum ShapesToolState {
    NO_INTERACTION, // the initial state and the state after Esc
    INITIAL_DRAG, // during the initial drag (no transform box)
    TRANSFORM // the transform box is visible
}
