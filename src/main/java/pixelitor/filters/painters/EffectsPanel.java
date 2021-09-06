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

package pixelitor.filters.painters;

import org.jdesktop.swingx.painter.effects.*;
import pixelitor.filters.gui.*;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.geom.Point2D;

import static java.awt.BorderLayout.CENTER;
import static java.awt.Color.*;
import static java.awt.FlowLayout.LEFT;

/**
 * Configuration panel for SwingX {@link AreaEffect}s.
 * it can be the {@link ParamGUI} for an {@link EffectsParam}
 */
public class EffectsPanel extends JPanel implements Resettable, ParamGUI {
    public static final String GLOW_TAB_NAME = "Glow               ";
    public static final String INNER_GLOW_TAB_NAME = "Inner Glow     ";
    public static final String NEON_BORDER_TAB_NAME = "Neon Border ";
    public static final String DROP_SHADOW_TAB_NAME = "Drop Shadow";

    private BaseEffectPanel glowPanel;
    private BaseEffectPanel innerGlowPanel;
    private NeonBorderPanel neonBorderPanel;
    private DropShadowPanel dropShadowPanel;
    private final BaseEffectPanel[] panels = new BaseEffectPanel[4];

    private final JTabbedPane tabs;

    public EffectsPanel(AreaEffects givenEffects) {
        setLayout(new BorderLayout());

        setEffects(givenEffects);
        panels[0] = glowPanel;
        panels[1] = innerGlowPanel;
        panels[2] = neonBorderPanel;
        panels[3] = dropShadowPanel;

        tabs = new JTabbedPane();
        tabs.setTabPlacement(SwingConstants.LEFT);
        tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        addTab(GLOW_TAB_NAME, glowPanel);
        addTab(INNER_GLOW_TAB_NAME, innerGlowPanel);
        addTab(NEON_BORDER_TAB_NAME, neonBorderPanel);
        addTab(DROP_SHADOW_TAB_NAME, dropShadowPanel);

        selectFirstEnabledTab();

        add(tabs, CENTER);
    }

    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        assert listener != null;
        for (BaseEffectPanel panel : panels) {
            panel.setAdjustmentListener(listener);
        }
    }

    private void selectFirstEnabledTab() {
        for (int i = 0; i < panels.length; i++) {
            BaseEffectPanel panel = panels[i];
            if (panel.isEffectEnabled()) {
                tabs.setSelectedIndex(i);
                break;
            }
        }
    }

    public void setEffects(AreaEffects effects) {
        initGlowPanel(effects);
        initInnerGlowPanel(effects);
        initNeonBorderPanel(effects);
        initDropShadowPanel(effects);

        if (tabs != null) {
            selectFirstEnabledTab();
        }
    }

    private void initGlowPanel(AreaEffects effects) {
        boolean enable = false;
        Color color = WHITE;
        double width = 10;
        float opacity = 1.0f;
        if (effects != null) {
            var effect = effects.getGlow();
            if (effect != null) {
                enable = true;
                color = effect.getBrushColor();
                width = effect.getEffectWidth();
                opacity = effect.getOpacity();
            }
        }
        if (glowPanel == null) { // first initialization
            glowPanel = new EffectWithWidthPanel(
                "Glow", enable, color, width, opacity);
        } else {
            glowPanel.setTabEnabled(enable);
            glowPanel.setBrushWidth(width);
            glowPanel.setColor(color, false);
            glowPanel.setOpacity(opacity);
        }
    }

    private void initInnerGlowPanel(AreaEffects effects) {
        boolean enable = false;
        Color color = RED;
        double width = 10;
        float opacity = 1.0f;
        if (effects != null) {
            var effect = effects.getInnerGlow();
            if (effect != null) {
                enable = true;
                color = effect.getBrushColor();
                width = effect.getEffectWidth();
                opacity = effect.getOpacity();
            }
        }
        if (innerGlowPanel == null) { // first initialization
            innerGlowPanel = new EffectWithWidthPanel(
                "Inner Glow", enable, color, width, opacity);
        } else {
            innerGlowPanel.setTabEnabled(enable);
            innerGlowPanel.setBrushWidth(width);
            innerGlowPanel.setColor(color, false);
            innerGlowPanel.setOpacity(opacity);
        }
    }

    private void initNeonBorderPanel(AreaEffects effects) {
        boolean enable = false;
        Color color = GREEN;
        Color innerColor = WHITE;
        double width = 10;
        float opacity = 1.0f;
        if (effects != null) {
            var effect = effects.getNeonBorder();
            if (effect != null) {
                enable = true;
                color = effect.getEdgeColor();
                innerColor = effect.getCenterColor();
                width = effect.getEffectWidth();
                opacity = effect.getOpacity();
            }
        }
        if (neonBorderPanel == null) { // first initialization
            neonBorderPanel = new NeonBorderPanel(
                enable, color, innerColor, width, opacity);
        } else {
            neonBorderPanel.setTabEnabled(enable);
            neonBorderPanel.setBrushWidth(width);
            neonBorderPanel.setColor(color, false);
            neonBorderPanel.setOpacity(opacity);
            neonBorderPanel.setInnerColor(innerColor, false);
        }
    }

    private void initDropShadowPanel(AreaEffects effects) {
        boolean enable = false;
        Color color = BLACK;
        int distance = 10;
        double angle = 0.7;
        double spread = 10;
        float opacity = 1.0f;
        if (effects != null) {
            var effect = effects.getDropShadow();
            if (effect != null) {
                enable = true;
                color = effect.getBrushColor();
                opacity = effect.getOpacity();

                Point2D offset = effect.getOffset();
                double x = offset.getX();
                double y = offset.getY();
                distance = (int) Math.sqrt(x * x + y * y);
                angle = Math.atan2(y, x);

                spread = effect.getEffectWidth();
            }
        }
        if (dropShadowPanel == null) { // first initialization
            dropShadowPanel = new DropShadowPanel(
                enable, color, distance, angle, spread, opacity);
        } else {
            dropShadowPanel.setTabEnabled(enable);
            dropShadowPanel.setBrushWidth(spread);
            dropShadowPanel.setColor(color, false);
            dropShadowPanel.setOpacity(opacity);
            dropShadowPanel.setAngle(angle);
            dropShadowPanel.setDistance(distance);
        }
    }

    private void updateEffectsFromGUI(AreaEffects effects) {
        updateGlowFromGUI(effects);
        updateInnerGlowFromGUI(effects);
        updateNeonBorderFromGUI(effects);
        updateDropShadowFromGUI(effects);
    }

    private void updateGlowFromGUI(AreaEffects effects) {
        GlowPathEffect glowEffect = null;
        if (glowPanel.isEffectEnabled()) {
            glowEffect = new GlowPathEffect(glowPanel.getOpacity());
            glowPanel.updateEffectColorAndBrush(glowEffect);
        }
        effects.setGlow(glowEffect);
    }

    private void updateInnerGlowFromGUI(AreaEffects effects) {
        InnerGlowPathEffect innerGlowEffect = null;
        if (innerGlowPanel.isEffectEnabled()) {
            innerGlowEffect = new InnerGlowPathEffect(innerGlowPanel.getOpacity());
            innerGlowPanel.updateEffectColorAndBrush(innerGlowEffect);
        }
        effects.setInnerGlow(innerGlowEffect);
    }

    private void updateNeonBorderFromGUI(AreaEffects effects) {
        NeonBorderEffect neonBorderEffect = null;
        if (neonBorderPanel.isEffectEnabled()) {
            Color edgeColor = neonBorderPanel.getColor();
            Color centerColor = neonBorderPanel.getInnerColor();
            double effectWidth = neonBorderPanel.getBrushWidth();

            neonBorderEffect = new NeonBorderEffect(edgeColor, centerColor, effectWidth,
                neonBorderPanel.getOpacity());
        }
        effects.setNeonBorder(neonBorderEffect);
    }

    private void updateDropShadowFromGUI(AreaEffects effects) {
        ShadowPathEffect dropShadowEffect = null;
        if (dropShadowPanel.isEffectEnabled()) {
            dropShadowEffect = new ShadowPathEffect(dropShadowPanel.getOpacity());
            dropShadowPanel.updateEffectColorAndBrush(dropShadowEffect);
            dropShadowEffect.setOffset(dropShadowPanel.getOffset());
        }
        effects.setDropShadow(dropShadowEffect);
    }

    private void addTab(String name, BaseEffectPanel configurator) {
        JPanel tabPanel = new JPanel(new FlowLayout(LEFT));
        JCheckBox tabCB = new JCheckBox();
        tabCB.setModel(configurator.getEnabledModel());
        tabCB.setName(name);
        tabPanel.add(tabCB);
        tabPanel.add(new JLabel(name));

        tabCB.addActionListener(e -> {
            if (tabCB.isSelected()) {
                tabs.setSelectedIndex(tabs.indexOfComponent(configurator));
            }
        });

        tabPanel.setOpaque(false);

        tabs.addTab(name, configurator);
        tabs.setTabComponentAt(tabs.getTabCount() - 1, tabPanel);
    }

    public void loadStateFrom(UserPreset preset) {
        AreaEffects newEffects = new AreaEffects();
        newEffects.loadStateFrom(preset);
        setEffects(newEffects);
    }

    public void saveStateTo(UserPreset preset) {
        AreaEffects effects = getEffects();
        effects.saveStateTo(preset);
    }

    public AreaEffects getEffects() {
        AreaEffects effects = new AreaEffects();
        updateEffectsFromGUI(effects);
        return effects;
    }

    @Override
    public boolean isSetToDefault() {
        for (BaseEffectPanel panel : panels) {
            if (!panel.isSetToDefault()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset(boolean trigger) {
        glowPanel.reset(false);
        innerGlowPanel.reset(false);
        neonBorderPanel.reset(false);
        dropShadowPanel.reset(trigger); // trigger at most one of them
    }

    public void randomize() {
        for (BaseEffectPanel panel : panels) {
            panel.randomize();
        }
    }

    public void setDefaultButton(DefaultButton button) {
        for (BaseEffectPanel panel : panels) {
            panel.setDefaultButton(button);
        }
    }

    @Override
    public String getResetToolTip() {
        return "Reset the default effect settings";
    }

    @Override
    public void updateGUI() {

    }

    @Override
    public void setToolTip(String tip) {

    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}

