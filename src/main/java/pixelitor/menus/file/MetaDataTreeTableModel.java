/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The model for the metadata tree-table
 */
public class MetaDataTreeTableModel extends AbstractTreeTableModel {
    private static final String[] COLUMN_NAMES = {"Name", "Value"};
    private final List<DirNode> dirNodes = new ArrayList<>();

    public MetaDataTreeTableModel(Metadata metadata) {
        super(new Object());
        for (Directory directory : metadata.getDirectories()) {
            dirNodes.add(new DirNode(directory));
        }
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if (node instanceof DirNode dir) {
            return switch (column) {
                case 0 -> dir.name();
                case 1 -> null;
                default -> throw new IllegalStateException("Unexpected column: " + column);
            };
        } else if (node instanceof TagNode tag) {
            return switch (column) {
                case 0 -> tag.name();
                case 1 -> tag.value();
                default -> throw new IllegalStateException("Unexpected column: " + column);
            };
        }
        return null;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof DirNode dir) {
            return dir.nodes.get(index);
        }
        return dirNodes.get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof DirNode dir) {
            return dir.nodes.size();
        }
        return dirNodes.size();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((TagNode) child).index();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        return false;
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof TagNode;
    }

    static class DirNode {
        private final Directory dir;
        private final List<TagNode> nodes = new ArrayList<>();

        DirNode(Directory dir) {
            this.dir = dir;
            Collection<Tag> tags = dir.getTags();
            int tagIndex = 0;
            for (Tag tag : tags) {
                String tagName = tag.getTagName();
                if (tagName.startsWith("Unknown")) {
                    continue;
                }
                String description = tag.getDescription();
                if (description != null && description.startsWith("Unknown")) {
                    continue;
                }
                nodes.add(new TagNode(tagName, description, tagIndex));
                tagIndex++;
            }
        }

        public String name() {
            return dir.getName();
        }
    }

    private record TagNode(String name, String value, int index) {
    }
}
