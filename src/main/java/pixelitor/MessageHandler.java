package pixelitor;

/**
 * Abstracts away messages sent through the GUI
 * in order to enable GUI-independent testability.
 */
public interface MessageHandler {
    void showStatusBarMessage(String msg);

    void showInfoDialog(String title, String msg);

    void showErrorDialog(String title, String msg);

    void showNotImageLayerDialog();
}
