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

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The container for LayerButton objects
 */
public class LayersPanel extends JLayeredPane {
    private List<LayerButton> layerButtons = new ArrayList<>();
    private final ButtonGroup buttonGroup = new ButtonGroup();

    public LayersPanel() {
//        LayersLayout layersLayout = new LayersLayout(1, 1);
//        setLayout(layersLayout);

//        addMouseListener(layersLayout);
//        addMouseMotionListener(layersLayout);
//        setBorder(BorderFactory.createLineBorder(Color.BLUE));
    }

    public void addLayerButton(LayerButton button, int newLayerIndex) {
        if (button == null) {
            throw new IllegalArgumentException("button is null");
        }

        buttonGroup.add(button);
        addButton(button, newLayerIndex);

        button.setUserInteraction(false);
        button.setSelected(true);
        button.setUserInteraction(true);

        revalidate();
        repaint();
    }

    public void addButton(LayerButton button, int index) {
        layerButtons.add(index, button);
        add(button, JLayeredPane.DEFAULT_LAYER);
    }

    /**
     * Override doLayout() so that when the whole window is resized, the
     * layer buttons are still laid out correctly
     */
    @Override
    public void doLayout() {
        int parentHeight = getHeight();
        for (int i = 0; i < layerButtons.size(); i++) {
            LayerButton button = layerButtons.get(i);
            int buttonHeight = button.getPreferredSize().height;
            button.setSize(getWidth(), buttonHeight);
            button.setLocation(0, parentHeight - (i + 1) * buttonHeight);
        }
    }

    public void deleteLayerButton(LayerButton button) {
        buttonGroup.remove(button);
        layerButtons.remove(button);
        remove(button);
        revalidate();
        repaint();
    }

    public void changeLayerOrder(int oldIndex, int newIndex) {
        LayerButton button = layerButtons.remove(oldIndex);
        layerButtons.add(newIndex, button);

        revalidate();
    }
}
