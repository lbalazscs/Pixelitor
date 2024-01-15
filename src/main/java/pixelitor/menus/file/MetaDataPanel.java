/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.menus.file;

import org.jdesktop.swingx.JXTreeTable;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import pixelitor.Composition;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.PAction;
import pixelitor.menus.file.MetaDataTreeTableModel.NameValue;
import pixelitor.utils.Messages;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.FlowLayout.LEFT;
import static java.lang.String.format;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;

public class MetaDataPanel extends JPanel {
    private final JXTreeTable treeTable;

    private MetaDataPanel(MetaDataTreeTableModel model) {
        super(new BorderLayout());

        treeTable = new JXTreeTable(model);
        treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        treeTable.setRootVisible(false);
        treeTable.setHorizontalScrollEnabled(true);

        JScrollPane sp = new JScrollPane(treeTable);
        add(sp, CENTER);

        JPanel northPanel = new JPanel(new FlowLayout(LEFT));

        JButton expandButton = new JButton(new PAction(
            "Expand All", treeTable::expandAll));
        expandButton.setName("expandButton");

        JButton collapseButton = new JButton(new PAction(
            "Collapse All", treeTable::collapseAll));
        collapseButton.setName("collapseButton");

        northPanel.add(expandButton);
        northPanel.add(collapseButton);

        add(northPanel, NORTH);

        setupColumnsWidths();
    }

    private void setupColumnsWidths() {
        treeTable.getColumnModel().getColumn(0).setMinWidth(200);
        treeTable.getColumnModel().getColumn(1).setMinWidth(200);
    }

    private static TreeNode extractMetadata(File file) throws IOException {
        IIOMetadata metadata = null;
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                // pick the first available ImageReader
                ImageReader reader = readers.next();
                // attach source to the reader
                reader.setInput(iis, true);
                // read metadata of first image
                metadata = reader.getImageMetadata(0);
            }
        }
        String[] formatNames = metadata.getMetadataFormatNames();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        for (String formatName : formatNames) {
            Node node = metadata.getAsTree(formatName);
            root.add(toSwingNode(node));
        }
        return root;
    }

    private static DefaultMutableTreeNode toSwingNode(Node node) {
        DefaultMutableTreeNode swingNode;

        swingNode = new DefaultMutableTreeNode(node.getNodeName());

        NamedNodeMap map = node.getAttributes();
        if (map != null) {
            int length = map.getLength();
            if (length == 1) {
                NameValue nameValue = new NameValue(node.getNodeName(), map.item(0).getNodeValue());
                return new DefaultMutableTreeNode(nameValue);
            } else {
                for (int i = 0; i < length; i++) {
                    Node attr = map.item(i);
                    String nodeName = attr.getNodeName();
                    String nodeValue = attr.getNodeValue();
                    NameValue nameValue = new NameValue(nodeName, nodeValue);
                    swingNode.add(new DefaultMutableTreeNode(nameValue));
                }
            }
        }

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            swingNode.add(toSwingNode(childNode));
        }
        return swingNode;
    }

    public static void showInDialog(Composition comp) {
        File file = comp.getFile();
        if (file == null) {
            Dialogs.showInfoDialog(comp.getDialogParent(), "No file", format(
                "<html>There is no file for <b>%s</b>.", comp.getName()));
            return;
        }
        if (!file.exists()) {
            String msg = format(
                "<html>The metadata for <b>%s</b> cannot be shown because the file<br>" +
                    "<b>%s</b><br>" +
                    "doesn't exist anymore.",
                comp.getName(), file.getAbsolutePath());
            Messages.showError("File not found", msg, comp.getDialogParent());
            return;
        }
        TreeNode metadataRoot;
        try {
            metadataRoot = extractMetadata(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        MetaDataPanel panel = new MetaDataPanel(new MetaDataTreeTableModel(metadataRoot));
        new DialogBuilder()
            .title("Metadata for " + file.getName())
            .content(panel)
            .okText(CLOSE_DIALOG)
            .noCancelButton()
            .show();
    }
}
