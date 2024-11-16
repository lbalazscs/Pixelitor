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
 * A tree-table model for displaying hierarchical metadata information.
 */
public class MetaDataTreeTableModel extends AbstractTreeTableModel {
    private static final String[] COLUMN_NAMES = {"Name", "Value"};
    private static final int COLUMN_INDEX_NAME = 0;
    private static final int COLUMN_INDEX_VALUE = 1;

    public MetaDataTreeTableModel(TreeNode root) {
        super(root);
    }

    @Override
    public Object getValueAt(Object node, int column) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
        Object nodeData = treeNode.getUserObject();
        return switch (nodeData) {
            case String categoryName -> switch (column) {
                case COLUMN_INDEX_NAME -> categoryName;
                case COLUMN_INDEX_VALUE -> null; // category nodes don't have values
                default -> throw new IllegalStateException("Unexpected column: " + column);
            };
            case Property property -> switch (column) {
                // a leaf node was found
                case COLUMN_INDEX_NAME -> property.name();
                case COLUMN_INDEX_VALUE -> property.value();
                default -> throw new IllegalStateException("Unexpected column: " + column);
            };
            default -> throw new IllegalStateException("Unexpected type: " + nodeData.getClass().getName());
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

    /**
     * A metadata property with a name and value.
     */
    public record Property(String name, String value) {
    }
}
