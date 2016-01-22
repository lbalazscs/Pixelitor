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

package pixelitor.filters.painters;

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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.geom.Point2D;

import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.WHITE;

/**
 * Configuration panel for SwingX effects
 */
public class EffectsPanel extends JPanel implements Resettable {
    public static final String GLOW_TAB_NAME = "Glow               ";
    public static final String INNER_GLOW_TAB_NAME = "Inner Glow     ";
    public static final String NEON_BORDER_TAB_NAME = "Neon Border ";
    public static final String DROP_SHADOW_TAB_NAME = "Drop Shadow";

    private EffectConfiguratorPanel glowConfigurator;
    private EffectConfiguratorPanel innerGlowConfigurator;
    private NeonBorderEffectConfiguratorPanel neonBorderConfigurator;
    private DropShadowEffectConfiguratorPanel dropShadowConfigurator;

    private final JTabbedPane tabs;

    private final AreaEffects returnedEffects;

    public EffectsPanel(ParamAdjustmentListener listener, AreaEffects givenEffects) {
        this.returnedEffects = new AreaEffects();

        setLayout(new BorderLayout());

        initGlowConfigurator(givenEffects);
        initInnerGlowConfigurator(givenEffects);
        initNeonBorderConfigurator(givenEffects);
        initDropShadowConfigurator(givenEffects);

        if (listener != null) {
            glowConfigurator.setAdjustmentListener(listener);
            innerGlowConfigurator.setAdjustmentListener(listener);
            neonBorderConfigurator.setAdjustmentListener(listener);
            dropShadowConfigurator.setAdjustmentListener(listener);
        }

        tabs = new JTabbedPane();
        tabs.setTabPlacement(JTabbedPane.LEFT);
        tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        addTab(GLOW_TAB_NAME, glowConfigurator);
        addTab(INNER_GLOW_TAB_NAME, innerGlowConfigurator);
        addTab(NEON_BORDER_TAB_NAME, neonBorderConfigurator);
        addTab(DROP_SHADOW_TAB_NAME, dropShadowConfigurator);

        tabs.setPreferredSize(new Dimension(530, 350)); // A width if 520 is enough on windows. TODO: calculate

        add(tabs, BorderLayout.CENTER);
    }

    private void initGlowConfigurator(AreaEffects effects) {
        boolean defaultEnabled = false;
        Color defaultColor = WHITE;
        int defaultWidth = 10;
        if (effects != null) {
            GlowPathEffect effect = effects.getGlowEffect();
            if (effect != null) {
                defaultEnabled = true;
                defaultColor = effect.getBrushColor();
                defaultWidth = effect.getEffectWidth();
            }

        }
        glowConfigurator = new SimpleEffectConfiguratorPanel(
                "Glow", defaultEnabled, defaultColor, defaultWidth);
    }

    private void initInnerGlowConfigurator(AreaEffects effects) {
        boolean defaultEnabled = false;
        Color defaultColor = WHITE;
        int defaultWidth = 10;
        if (effects != null) {
            InnerGlowPathEffect effect = effects.getInnerGlowEffect();
            if (effect != null) {
                defaultEnabled = true;
                defaultColor = effect.getBrushColor();
                defaultWidth = effect.getEffectWidth();
            }
        }
        innerGlowConfigurator = new SimpleEffectConfiguratorPanel(
                "Inner Glow", defaultEnabled, defaultColor, defaultWidth);
    }

    private void initNeonBorderConfigurator(AreaEffects effects) {
        boolean defaultEnabled = false;
        Color defaultColor = GREEN;
        Color defaultInnerColor = WHITE;
        int defaultWidth = 10;
        if (effects != null) {
            NeonBorderEffect effect = effects.getNeonBorderEffect();
            if (effect != null) {
                defaultEnabled = true;
                defaultColor = effect.getEdgeColor();
                defaultInnerColor = effect.getCenterColor();
                defaultWidth = effect.getEffectWidth();
            }
        }
        neonBorderConfigurator = new NeonBorderEffectConfiguratorPanel(
                defaultEnabled, defaultColor, defaultInnerColor, defaultWidth);
    }

    private void initDropShadowConfigurator(AreaEffects effects) {
        boolean defaultEnabled = false;
        Color defaultColor = BLACK;
        int defaultDistance = 10;
        double defaultAngle = 0.7;
        int defaultSpread = 10;
        if (effects != null) {
            ShadowPathEffect effect = effects.getDropShadowEffect();
            if (effect != null) {
                defaultEnabled = true;
                defaultColor = effect.getBrushColor();

                Point2D offset = effect.getOffset();
                double x = offset.getX();
                double y = offset.getY();
                defaultDistance = (int) Math.sqrt(x * x + y * y);
                defaultAngle = Math.atan2(y, x);

                defaultSpread = effect.getEffectWidth();
            }
        }
        dropShadowConfigurator = new DropShadowEffectConfiguratorPanel(
                defaultEnabled, defaultColor, defaultDistance, defaultAngle, defaultSpread);
    }

    public void updateEffectsFromGUI() {
        updateGlowFromGUI();
        updateInnerGlowFromGUI();
        updateNeonBorderFromGUI();
        updateDropShadowFromGUI();
    }

    private void updateGlowFromGUI() {
        GlowPathEffect glowEffect = null;
        if (glowConfigurator.isSelected()) {
            glowEffect = new GlowPathEffect(glowConfigurator.getOpacity());
            glowConfigurator.updateEffectColorAndBrush(glowEffect);
        }
        returnedEffects.setGlowEffect(glowEffect);
    }

    private void updateInnerGlowFromGUI() {
        InnerGlowPathEffect innerGlowEffect = null;
        if (innerGlowConfigurator.isSelected()) {
            innerGlowEffect = new InnerGlowPathEffect(innerGlowConfigurator.getOpacity());
            innerGlowConfigurator.updateEffectColorAndBrush(innerGlowEffect);
        }
        returnedEffects.setInnerGlowEffect(innerGlowEffect);
    }

    private void updateNeonBorderFromGUI() {
        NeonBorderEffect neonBorderEffect = null;
        if (neonBorderConfigurator.isSelected()) {
            Color edgeColor = neonBorderConfigurator.getColor();
            Color centerColor = neonBorderConfigurator.getInnerColor();
            int effectWidth = neonBorderConfigurator.getBrushWidth();

            neonBorderEffect = new NeonBorderEffect(edgeColor, centerColor, effectWidth,
                    neonBorderConfigurator.getOpacity());
        }
        returnedEffects.setNeonBorderEffect(neonBorderEffect);
    }

    private void updateDropShadowFromGUI() {
        ShadowPathEffect dropShadowEffect = null;
        if (dropShadowConfigurator.isSelected()) {
            dropShadowEffect = new ShadowPathEffect(dropShadowConfigurator.getOpacity());
            dropShadowConfigurator.updateEffectColorAndBrush(dropShadowEffect);
            dropShadowEffect.setOffset(dropShadowConfigurator.getOffset());
        }
        returnedEffects.setDropShadowEffect(dropShadowEffect);
    }

    private void addTab(String name, EffectConfiguratorPanel configurator) {
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
        return returnedEffects;
    }

    public int getMaxEffectThickness() {
        return returnedEffects.getMaxEffectThickness();
    }

    @Override
    public boolean isSetToDefault() {
        return glowConfigurator.isSetToDefault()
                && innerGlowConfigurator.isSetToDefault()
                && neonBorderConfigurator.isSetToDefault()
                && dropShadowConfigurator.isSetToDefault();
    }

    @Override
    public void reset(boolean triggerAction) {
        glowConfigurator.reset(false);
        innerGlowConfigurator.reset(false);
        neonBorderConfigurator.reset(false);
        dropShadowConfigurator.reset(triggerAction);
    }

    public void setDefaultButton(DefaultButton button) {
        glowConfigurator.setDefaultButton(button);
        innerGlowConfigurator.setDefaultButton(button);
        neonBorderConfigurator.setDefaultButton(button);
        dropShadowConfigurator.setDefaultButton(button);
    }
}

