/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.layers;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * A layout manager for the layers. It arranges its components vertically from the bottom upwards,
 * respecting their preferred sizes.
 */
//public class LayersLayout implements LayoutManager, MouseListener, MouseMotionListener {
public class LayersLayout implements LayoutManager {
    private final int hGap;
    private final int vGap;

    public LayersLayout(int hGap, int vGap) {
        this.hGap = hGap;
        this.vGap = vGap;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {

            Dimension dim = new Dimension(0, vGap);
            int nMembers = parent.getComponentCount();

            for (int i = 0; i < nMembers; i++) {
                Component c = parent.getComponent(i);
                dim.height += c.getPreferredSize().getHeight();
                dim.height += vGap;
                dim.width = Math.max(dim.width, (int) c.getPreferredSize().getWidth());
            }
            dim.width += 2 * hGap;

            return dim;
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            int nMembers = parent.getComponentCount();
            int parentHeight = Math.max((int) parent.getPreferredSize().getHeight(), parent.getHeight());
            int currentBaseY = parentHeight - vGap;
            int totalWidth = Math.max((int) parent.getPreferredSize().getWidth(), parent.getWidth());
            for (int i = 0; i < nMembers; i++) {
                Component c = parent.getComponent(i);
                int compHeight = (int) c.getPreferredSize().getHeight();

                int x = hGap;
                int y = currentBaseY - compHeight;
                int width = totalWidth - 2 * hGap;
                c.setBounds(x, y, width, compHeight);
                currentBaseY -= (compHeight + vGap);
            }
        }
    }

//    // mouse lister part
//    private int startX;
//    private int startY;
//    private int endX;
//    private int endY;
//    private Component draggedComponent;
//
//    @Override
//    public void mouseClicked(MouseEvent e) {
//        startX = e.getX();
//        startY = e.getY();
//
//        System.out.println("LayersLayout.mouseClicked startX = " + startX + ", startY = " + startY);
//
//    }
//
//    @Override
//    public void mousePressed(MouseEvent e) {
//
//    }
//
//    @Override
//    public void mouseReleased(MouseEvent e) {
//
//    }
//
//    @Override
//    public void mouseEntered(MouseEvent e) {
//
//    }
//
//    @Override
//    public void mouseExited(MouseEvent e) {
//
//    }
//
//    @Override
//    public void mouseDragged(MouseEvent e) {
//
//    }
//
//    @Override
//    public void mouseMoved(MouseEvent e) {
//
//    }
}