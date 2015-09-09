package pixelitor;

/**
 * Abstracts away messages sent through the GUI
 * in order to enable GUI-independent testability.
 */
public interface MessageHandler {
    void showStatusBarMessage(String msg);

    void showInfo(String title, String msg);

    void showError(String title, String msg);

    void showNotImageLayerError();

    void showException(Throwable e);
}
