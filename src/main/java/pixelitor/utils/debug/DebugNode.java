/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import java.util.Enumeration;

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
        Enumeration<DefaultMutableTreeNode> childrenEnum = children();

        StringBuilder sb = new StringBuilder();

        addIndent(sb, getLevel());
        sb.append(name).append(" {");

        while (childrenEnum.hasMoreElements()) {
            Object o = childrenEnum.nextElement();
            addIndent(sb, getLevel() + 1);

            DefaultMutableTreeNode t = (DefaultMutableTreeNode) o;

            String info;
            if (t instanceof DebugNode) {
                DebugNode dn = (DebugNode) t;
                info = dn.toDetailedString();
            } else {
                info = t.toString();
            }
            sb.append(info);
        }

        addIndent(sb, getLevel());
        sb.append('}');

        return sb.toString();
    }

    public void addStringChild(String name, String s) {
        add(new DefaultMutableTreeNode(name + " = " + s));
    }

    public void addQuotedStringChild(String name, String s) {
        add(new DefaultMutableTreeNode(String.format("%s = \"%s\"", name, s)));
    }

    public void addIntChild(String name, int i) {
        add(new DefaultMutableTreeNode(name + " = " + i));
    }

    public void addFloatChild(String name, float f) {
        add(new DefaultMutableTreeNode(String.format("%s = %.2f", name, f)));
    }

    public void addDoubleChild(String name, double f) {
        add(new DefaultMutableTreeNode(String.format("%s = %.2f", name, f)));
    }

    public void addBooleanChild(String name, boolean b) {
        add(new DefaultMutableTreeNode(name + " = " + b));
    }

    public void addClassChild() {
        add(new DefaultMutableTreeNode("Class = " + userObject.getClass().getSimpleName()));
    }

    private static void addIndent(StringBuilder sb, int indentLevel) {
        sb.append('\n');
        for (int i = 0; i < indentLevel; i++) {
            sb.append("  ");
        }
    }
}
