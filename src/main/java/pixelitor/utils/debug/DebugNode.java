/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils.debug;

import pixelitor.colors.Colors;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.Color;
import java.util.Enumeration;

import static java.lang.String.format;

/**
 * A node that appears in the tree shown by "Help/Internal State".
 */
public class DebugNode extends DefaultMutableTreeNode {
    private final String name;

    public DebugNode(String name, Object userObject) {
        super(userObject);
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns a JSON-ish text representation of the tree.
     */
    public String toJSON() {
        if (userObject == null) {
            return "\"" + name + "\": null,";
        }

        StringBuilder sb = new StringBuilder();

        indent(sb, getLevel());
        if (isRoot()) {
            sb.append("{");
        } else {
            sb.append('"').append(name).append("\": {");
        }

        Enumeration<TreeNode> childrenEnum = children();
        while (childrenEnum.hasMoreElements()) {
            indent(sb, getLevel() + 1);

            TreeNode child = childrenEnum.nextElement();

            String text;
            if (child instanceof DebugNode dn) {
                text = dn.toJSON();
            } else if (child instanceof DefaultMutableTreeNode defaultNode) {
                text = ((StringUserObject) defaultNode.getUserObject()).toJSON();
            } else {
                throw new IllegalStateException();
            }

            sb.append(text);
        }

        indent(sb, getLevel());

        if (isRoot()) {
            sb.append('}');
        } else {
            sb.append("},");
        }

        return sb.toString();
    }

    public void addString(String name, String s) {
        addNode(name, s);
    }

    /**
     * A null-safe way of adding the toString() of an object
     */
    public void addAsString(String name, Object o) {
        addString(name, o == null ? "null" : o.toString());
    }

    /**
     * A null-safe version of adding the DebugNode created by an object
     */
    public void addNullableDebuggable(String name, Debuggable debuggable) {
        if (debuggable == null) {
            addString(name, "null");
        } else {
            add(debuggable.createDebugNode(name));
        }
    }

    public void addNullableProperty(String name, Object nullable) {
        addString("has " + name, nullable == null ? "no" : "yes");
    }

    public void addQuotedString(String name, String s) {
        addNode(name, "\"" + s + "\"");
    }

    public void addAsQuotedString(String name, Object o) {
        addQuotedString(name, o == null ? "null" : o.toString());
    }

    public void addInt(String name, int i) {
        addNode(name, String.valueOf(i));
    }

    public void addFloat(String name, float f) {
        addNode(name, format("%.2f", f));
    }

    public void addDouble(String name, double f) {
        addNode(name, format("%.2f", f));
    }

    public void addBoolean(String name, boolean b) {
        addNode(name, String.valueOf(b));
    }

    public void addColor(String name, Color c) {
        addNode(name, Colors.toHTMLHex(c, true));
    }

    public void addClass() {
        addNode("class", userObject.getClass().getSimpleName());
    }

    public void addAsClass(String name, Object o) {
        addString(name, o == null ? "null" : o.getClass().getName());
    }

    private void addNode(String key, String value) {
        add(new DefaultMutableTreeNode(new StringUserObject(key, value)));
    }

    private static void indent(StringBuilder sb, int indentLevel) {
        sb.append('\n');
        sb.append("  ".repeat(indentLevel));
    }

    /**
     * Allow a leaf node to have two string representations: a GUI text and a JSON.
     */
    private record StringUserObject(String key, String value) {
        public String toJSON() {
            return "\"" + key + "\": " + value + ",";
        }

        @Override
        public String toString() {
            return key + " = " + value;
        }
    }
}
