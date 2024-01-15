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

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * The model for the metadata tree-table
 */
public class MetaDataTreeTableModel extends AbstractTreeTableModel {
    private static final String[] COLUMN_NAMES = {"Name", "Value"};

    public MetaDataTreeTableModel(TreeNode root) {
        super(root);
    }

    @Override
    public Object getValueAt(Object node, int column) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
        Object userObject = treeNode.getUserObject();
        return switch (userObject) {
            case String s -> switch (column) {
                // a String means a "directory node" with no value
                case 0 -> s;
                case 1 -> null;
                default -> throw new IllegalStateException("Unexpected column: " + column);
            };
            case NameValue nameValue -> switch (column) {
                // a leaf node was found
                case 0 -> nameValue.name();
                case 1 -> nameValue.value();
                default -> throw new IllegalStateException("Unexpected column: " + column);
            };
            default -> throw new IllegalStateException("Unexpected type: " + userObject.getClass().getName());
        };
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((TreeNode) parent).getChildAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return ((TreeNode) parent).getChildCount();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((TreeNode) parent).getIndex((TreeNode) child);
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public record NameValue(String name, String value) {
    }
}
