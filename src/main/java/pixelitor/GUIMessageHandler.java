package pixelitor;

import pixelitor.utils.Dialogs;

public class GUIMessageHandler implements MessageHandler {
    PixelitorWindow pw;

    public GUIMessageHandler(PixelitorWindow pw) {
        this.pw = pw;
    }

    @Override
    public void showStatusBarMessage(String msg) {
        pw.setStatusBarMessage(msg);
    }

    @Override
    public void showInfoDialog(String title, String msg) {
        Dialogs.showInfoDialog(title, msg);
    }

    @Override
    public void showErrorDialog(String title, String msg) {
        Dialogs.showErrorDialog(title, msg);
    }

    @Override
    public void showNotImageLayerDialog() {
        Dialogs.showNotImageLayerDialog();
    }
}
