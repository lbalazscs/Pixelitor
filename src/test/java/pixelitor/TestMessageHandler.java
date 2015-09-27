package pixelitor;

/**
 * A non-GUI message handler for tests
 */
public class TestMessageHandler implements MessageHandler {
    @Override
    public void showStatusMessage(String msg) {
    }

    @Override
    public void showInfo(String title, String msg) {
    }

    @Override
    public void showError(String title, String msg) {
        throw new AssertionError("error");
    }

    @Override
    public void showNotImageLayerError() {
        throw new AssertionError("not image layer");
    }

    @Override
    public void showException(Throwable e) {
        throw new AssertionError(e);
    }
}
