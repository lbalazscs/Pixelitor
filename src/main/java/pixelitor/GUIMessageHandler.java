package pixelitor;

import pixelitor.utils.Dialogs;

public class GUIMessageHandler implements MessageHandler {
    final PixelitorWindow pw;

    public GUIMessageHandler(PixelitorWindow pw) {
        this.pw = pw;
    }

    @Override
    public void showStatusBarMessage(String msg) {
        pw.setStatusBarMessage(msg);
    }

    @Override
    public void showInfo(String title, String msg) {
        Dialogs.showInfoDialog(title, msg);
    }

    @Override
    public void showError(String title, String msg) {
        Dialogs.showErrorDialog(title, msg);
    }

    @Override
    public void showNotImageLayerError() {
        Dialogs.showNotImageLayerDialog();
    }

    @Override
    public void showException(Throwable e) {
        Dialogs.showExceptionDialog(e);
    }
}
