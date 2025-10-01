/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.GUIText;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    public void showInDialog(String title) {
        JTree tree = new JTree(this);
        new DialogBuilder()
            .title(title)
            .content(new JScrollPane(tree))
            .okText(GUIText.COPY_AS_JSON)
            .cancelText(GUIText.CLOSE_DIALOG)
            .validator(d -> {
                Utils.copyStringToClipboard(toJSON());
                return false; // prevents the dialog from closing on click
            })
            .show();
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns a JSON text representation of the tree.
     */
    public String toJSON() {
        // start recursion with indentation level 0
        return toJSON(0);
    }

    private String toJSON(int indentLevel) {
        if (userObject == null) {
            return "  ".repeat(indentLevel) + "\"" + name + "\": null";
        }

        String currentIndent = "  ".repeat(indentLevel);

        StringBuilder sb = new StringBuilder();

        if (isRoot()) {
            sb.append("{\n");
        } else {
            sb.append(currentIndent).append('"').append(name).append("\": {\n");
        }

        List<String> childrenJsonParts = new ArrayList<>();
        Enumeration<TreeNode> childrenEnum = children();
        while (childrenEnum.hasMoreElements()) {
            TreeNode child = childrenEnum.nextElement();
            String childJson;
            if (child instanceof DebugNode dn) {
                childJson = dn.toJSON(indentLevel + 1);
            } else if (child instanceof DefaultMutableTreeNode defaultNode) {
                StringKeyValue skv = (StringKeyValue) defaultNode.getUserObject();
                childJson = "  ".repeat(indentLevel + 1) + skv.toJSON();
            } else {
                throw new IllegalStateException("Unknown child type in DebugNode tree");
            }
            childrenJsonParts.add(childJson);
        }

        sb.append(String.join(",\n", childrenJsonParts));

        if (!childrenJsonParts.isEmpty()) {
            sb.append('\n');
        }

        if (isRoot()) {
            sb.append("}");
        } else {
            sb.append(currentIndent).append("}");
        }

        return sb.toString();
    }

    public void addString(String name, String s) {
        addNode(name, s, DebugNode::quote);
    }

    /**
     * Adds a file path, ensuring (Windows) backslashes are escaped for JSON.
     */
    public void addFilePath(String name, String path) {
        if (path == null) {
            addNullNode(name);
        } else {
            addNode(name, path, DebugNode::quoteAndEscapeBackslashes);
        }
    }

    /**
     * Adds the toString() representation of an object.
     */
    public void addAsString(String name, Object o) {
        if (o == null) {
            addNullNode(name);
        } else {
            addString(name, o.toString());
        }
    }

    /**
     * A null-safe version of adding the {@link DebugNode} created by an object.
     */
    public void addNullableDebuggable(String name, Debuggable debuggable) {
        if (debuggable == null) {
            addNullNode(name);
        } else {
            add(debuggable.createDebugNode(name));
        }
    }

    /**
     * This overload can be used if the debugged object can't implement {@link Debuggable}
     */
    public <T> void addNullableDebuggable(String name, T debugged,
                                          BiFunction<String, T, DebugNode> transformer) {
        if (debugged == null) {
            addNullNode(name);
        } else {
            add(transformer.apply(name, debugged));
        }
    }

    /**
     * Reports the presence or absence (null vs. non-null) of an object.
     */
    public void addPresence(String name, Object nullable) {
        addString("has " + name, nullable == null ? "no" : "yes");
    }

    public void addQuotedString(String name, String s) {
        addValidJsonNode(name, quote(s));
    }

    public void addAsQuotedString(String name, Object o) {
        if (o == null) {
            addNullNode(name);
        } else {
            addQuotedString(name, o.toString());
        }
    }

    public void addInt(String name, int i) {
        addValidJsonNode(name, String.valueOf(i));
    }

    public void addFloat(String name, float f) {
        addValidJsonNode(name, format("%.2f", f));
    }

    public void addDouble(String name, double f) {
        addValidJsonNode(name, format("%.2f", f));
    }

    public void addBoolean(String name, boolean b) {
        addValidJsonNode(name, String.valueOf(b));
    }

    public void addColor(String name, Color c) {
        addNode(name, Colors.toHTMLHex(c, true), DebugNode::quote);
    }

    public void addClass() {
        addNode("class", userObject.getClass().getSimpleName(), DebugNode::quote);
    }

    public void addAsClass(String name, Object o) {
        if (o == null) {
            addNullNode(name);
        } else {
            addString(name, o.getClass().getName());
        }
    }

    private void addNullNode(String name) {
        addValidJsonNode(name, "null");
    }

    // adds a value which is already a valid JSON literal
    private void addValidJsonNode(String key, String value) {
        addNode(key, value, Function.identity());
    }

    private void addNode(String key, String value, Function<String, String> jsonValueConverter) {
        add(new DefaultMutableTreeNode(new StringKeyValue(key, value, jsonValueConverter)));
    }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }

    private static String quoteAndEscapeBackslashes(String s) {
        // in JSON, a backslash must be escaped with another backslash
        String escaped = s.replace("\\", "\\\\");
        return quote(escaped);
    }

    /**
     * Allow a leaf node to have two string representations: a GUI text and a JSON.
     */
    private record StringKeyValue(String key, String value, Function<String, String> jsonValueConverter) {
        String toJSON() {
            String jsonValue = jsonValueConverter.apply(value);
            return "\"" + key + "\": " + jsonValue;
        }

        @Override
        public String toString() {
            // the value is stored exactly as it should appear in the GUI
            return key + " = " + value;
        }
    }
}
