/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.painter.effects.AreaEffect;
import org.jdesktop.swingx.painter.effects.GlowPathEffect;
import org.jdesktop.swingx.painter.effects.InnerGlowPathEffect;
import org.jdesktop.swingx.painter.effects.NeonBorderEffect;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import pixelitor.filters.gui.DefaultButton;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.Resettable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.geom.Point2D;

import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;

/**
 * Configuration panel for SwingX area effects.
 * See {@link AreaEffect}
 */
public class EffectsPanel extends JPanel implements Resettable {
    public static final String GLOW_TAB_NAME = "Glow               ";
    public static final String INNER_GLOW_TAB_NAME = "Inner Glow     ";
    public static final String NEON_BORDER_TAB_NAME = "Neon Border ";
    public static final String DROP_SHADOW_TAB_NAME = "Drop Shadow";

    private EffectPanel glowPanel;
    private EffectPanel innerGlowPanel;
    private NeonBorderPanel neonBorderPanel;
    private DropShadowPanel dropShadowPanel;

    private final JTabbedPane tabs;

    public EffectsPanel(ParamAdjustmentListener listener, AreaEffects givenEffects) {
        setLayout(new BorderLayout());

        setEffects(givenEffects);

        if (listener != null) {
            glowPanel.setAdjustmentListener(listener);
            innerGlowPanel.setAdjustmentListener(listener);
            neonBorderPanel.setAdjustmentListener(listener);
            dropShadowPanel.setAdjustmentListener(listener);
        }

        tabs = new JTabbedPane();
        tabs.setTabPlacement(JTabbedPane.LEFT);
        tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        addTab(GLOW_TAB_NAME, glowPanel);
        addTab(INNER_GLOW_TAB_NAME, innerGlowPanel);
        addTab(NEON_BORDER_TAB_NAME, neonBorderPanel);
        addTab(DROP_SHADOW_TAB_NAME, dropShadowPanel);

        add(tabs, BorderLayout.CENTER);
    }

    public void setEffects(AreaEffects effects) {
        initGlowPanel(effects);
        initInnerGlowPanel(effects);
        initNeonBorderPanel(effects);
        initDropShadowPanel(effects);
    }

    private void initGlowPanel(AreaEffects effects) {
        boolean enable = false;
        Color color = WHITE;
        int width = 10;
        if (effects != null) {
            GlowPathEffect effect = effects.getGlowEffect();
            if (effect != null) {
                enable = true;
                color = effect.getBrushColor();
                width = effect.getEffectWidth();
            }
        }
        if (glowPanel == null) { // first initialization
            glowPanel = new EffectWithWidthPanel(
                    "Glow", enable, color, width);
        } else {
            glowPanel.setTabEnabled(enable);
            glowPanel.setBrushWidth(width);
            glowPanel.setColor(color, false);
        }
    }

    private void initInnerGlowPanel(AreaEffects effects) {
        boolean enable = false;
        Color color = RED;
        int width = 10;
        if (effects != null) {
            InnerGlowPathEffect effect = effects.getInnerGlowEffect();
            if (effect != null) {
                enable = true;
                color = effect.getBrushColor();
                width = effect.getEffectWidth();
            }
        }
        if (innerGlowPanel == null) { // first initialization
            innerGlowPanel = new EffectWithWidthPanel(
                    "Inner Glow", enable, color, width);
        } else {
            innerGlowPanel.setTabEnabled(enable);
            innerGlowPanel.setBrushWidth(width);
            innerGlowPanel.setColor(color, false);
        }
    }

    private void initNeonBorderPanel(AreaEffects effects) {
        boolean enable = false;
        Color color = GREEN;
        Color innerColor = WHITE;
        int width = 10;
        if (effects != null) {
            NeonBorderEffect effect = effects.getNeonBorderEffect();
            if (effect != null) {
                enable = true;
                color = effect.getEdgeColor();
                innerColor = effect.getCenterColor();
                width = effect.getEffectWidth();
            }
        }
        if (neonBorderPanel == null) { // first initialization
            neonBorderPanel = new NeonBorderPanel(
                    enable, color, innerColor, width);
        } else {
            neonBorderPanel.setTabEnabled(enable);
            neonBorderPanel.setBrushWidth(width);
            neonBorderPanel.setColor(color, false);
            neonBorderPanel.setInnerColor(innerColor, false);
        }
    }

    private void initDropShadowPanel(AreaEffects effects) {
        boolean enable = false;
        Color color = BLACK;
        int distance = 10;
        double angle = 0.7;
        int spread = 10;
        if (effects != null) {
            ShadowPathEffect effect = effects.getDropShadowEffect();
            if (effect != null) {
                enable = true;
                color = effect.getBrushColor();

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
                    enable, color, distance, angle, spread);
        } else {
            dropShadowPanel.setTabEnabled(enable);
            dropShadowPanel.setBrushWidth(spread);
            dropShadowPanel.setColor(color, false);
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
        if (glowPanel.isSelected()) {
            glowEffect = new GlowPathEffect(glowPanel.getOpacity());
            glowPanel.updateEffectColorAndBrush(glowEffect);
        }
        effects.setGlowEffect(glowEffect);
    }

    private void updateInnerGlowFromGUI(AreaEffects effects) {
        InnerGlowPathEffect innerGlowEffect = null;
        if (innerGlowPanel.isSelected()) {
            innerGlowEffect = new InnerGlowPathEffect(innerGlowPanel.getOpacity());
            innerGlowPanel.updateEffectColorAndBrush(innerGlowEffect);
        }
        effects.setInnerGlowEffect(innerGlowEffect);
    }

    private void updateNeonBorderFromGUI(AreaEffects effects) {
        NeonBorderEffect neonBorderEffect = null;
        if (neonBorderPanel.isSelected()) {
            Color edgeColor = neonBorderPanel.getColor();
            Color centerColor = neonBorderPanel.getInnerColor();
            int effectWidth = neonBorderPanel.getBrushWidth();

            neonBorderEffect = new NeonBorderEffect(edgeColor, centerColor, effectWidth,
                    neonBorderPanel.getOpacity());
        }
        effects.setNeonBorderEffect(neonBorderEffect);
    }

    private void updateDropShadowFromGUI(AreaEffects effects) {
        ShadowPathEffect dropShadowEffect = null;
        if (dropShadowPanel.isSelected()) {
            dropShadowEffect = new ShadowPathEffect(dropShadowPanel.getOpacity());
            dropShadowPanel.updateEffectColorAndBrush(dropShadowEffect);
            dropShadowEffect.setOffset(dropShadowPanel.getOffset());
        }
        effects.setDropShadowEffect(dropShadowEffect);
    }

    private void addTab(String name, EffectPanel configurator) {
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox tabSelectionCB = new JCheckBox();
        tabSelectionCB.setModel(configurator.getEnabledModel());
        tabPanel.add(tabSelectionCB);
        tabPanel.add(new JLabel(name));

        tabPanel.setOpaque(false);

        tabs.addTab(name, configurator);
        tabs.setTabComponentAt(tabs.getTabCount() - 1, tabPanel);
    }

    public AreaEffects getEffects() {
        AreaEffects returnedEffects = new AreaEffects();
        updateEffectsFromGUI(returnedEffects);
        return returnedEffects;
    }

    @Override
    public boolean isSetToDefault() {
        return glowPanel.isSetToDefault()
                && innerGlowPanel.isSetToDefault()
                && neonBorderPanel.isSetToDefault()
                && dropShadowPanel.isSetToDefault();
    }

    @Override
    public void reset(boolean trigger) {
        glowPanel.reset(false);
        innerGlowPanel.reset(false);
        neonBorderPanel.reset(false);
        dropShadowPanel.reset(trigger);
    }

    public void setDefaultButton(DefaultButton button) {
        glowPanel.setDefaultButton(button);
        innerGlowPanel.setDefaultButton(button);
        neonBorderPanel.setDefaultButton(button);
        dropShadowPanel.setDefaultButton(button);
    }

    @Override
    public String getResetToolTip() {
        return "Reset the default effect settings";
    }
}

