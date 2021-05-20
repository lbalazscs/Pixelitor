package pixelitor.menus.file;

import pixelitor.NewImage;
import pixelitor.menus.PMenu;
import pixelitor.utils.Texts;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

import static pixelitor.utils.Texts.i18n;

public class ProjectIntegrationFilesMenu extends PMenu {

    public static final ProjectIntegrationFilesMenu INSTANCE = new ProjectIntegrationFilesMenu();

    private JMenuItem newImageMenuItem = null;

    private String projectDirectory = null;

    private final ArrayList<RecentFile> projectFiles;

    private ProjectIntegrationFilesMenu() {
        super(Texts.getResources().getString("project"), 'P');
        setVisible(false);

        projectFiles = new ArrayList<>();
        rebuildGUI();
    }

    public void addFile(File f) {
        if (f.exists()) {

            setVisible(true);

            System.out.println(f.getAbsolutePath());
            projectFiles.add(new RecentFile(f));
            rebuildGUI();
        }
    }

    public void setProjectDirectory(String projectDirectory) {
        this.projectDirectory = projectDirectory;

        if (projectDirectory != null) {
            newImageMenuItem = new JMenuItem(i18n("new_image"));
            newImageMenuItem.setAction(NewImage.getAction());
//            newImageMenuItem.addActionListener();
            rebuildGUI();
        }
    }

    private void removeAllMenuItems() {
        removeAll();
    }

    public ArrayList<RecentFile> getProjectFileInfosForSaving() {
        return projectFiles;
    }


    public boolean isPIActive() {
        return !projectFiles.isEmpty();
    }

    private void rebuildGUI() {
        removeAllMenuItems();

        if (newImageMenuItem != null)
            add(newImageMenuItem);

        addSeparator();

        for (int i = 0; i < projectFiles.size(); i++) {
            RecentFile recentFile = projectFiles.get(i);
            recentFile.setNr(i + 1);
            RecentFilesMenuItem item = new RecentFilesMenuItem(recentFile);
            add(item);
        }

        if (!projectFiles.isEmpty()) {
            addSeparator();
        }

    }

}
