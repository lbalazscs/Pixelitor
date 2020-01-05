/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;

import static java.lang.String.format;

/**
 * The superclass of all debugging nodes that appear
 * in the "Pixelitor Internal State" JTree
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

    public String toDetailedString() {
        if (userObject == null) {
            return name + " = null";
        }

        @SuppressWarnings("unchecked")
        Enumeration<TreeNode> childrenEnum = children();

        StringBuilder sb = new StringBuilder();

        indent(sb, getLevel());
        sb.append(name).append(" {");

        while (childrenEnum.hasMoreElements()) {
            indent(sb, getLevel() + 1);

            TreeNode t = childrenEnum.nextElement();

            String info;
            if (t instanceof DebugNode) {
                DebugNode dn = (DebugNode) t;
                info = dn.toDetailedString();
            } else {
                info = t.toString();
            }
            sb.append(info);
        }

        indent(sb, getLevel());
        sb.append('}');

        return sb.toString();
    }

    public void addString(String name, String s) {
        add(new DefaultMutableTreeNode(name + " = " + s));
    }

    public void addQuotedString(String name, String s) {
        add(new DefaultMutableTreeNode(format("%s = \"%s\"", name, s)));
    }

    public void addInt(String name, int i) {
        add(new DefaultMutableTreeNode(name + " = " + i));
    }

    public void addFloat(String name, float f) {
        add(new DefaultMutableTreeNode(format("%s = %.2f", name, f)));
    }

    public void addDouble(String name, double f) {
        add(new DefaultMutableTreeNode(format("%s = %.2f", name, f)));
    }

    public void addBoolean(String name, boolean b) {
        add(new DefaultMutableTreeNode(name + " = " + b));
    }

    public void addClass() {
        add(new DefaultMutableTreeNode("class = " + userObject.getClass().getSimpleName()));
    }

    private static void indent(StringBuilder sb, int indentLevel) {
        sb.append('\n');
        for (int i = 0; i < indentLevel; i++) {
            sb.append("  ");
        }
    }
}
