/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.curves;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ToneCurvesTest {

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    void initialState() {
        ToneCurves curves = new ToneCurves();
        assertThat(curves.getColorSpace()).isEqualTo(ColorSpace.SRGB);
        assertThat(curves.getActiveChannel()).isEqualTo(Channel.RGB);
        assertThat(curves.getActiveAdjustment().getChannel()).isEqualTo(Channel.RGB);
        assertThat(curves.getAllAdjustments()).hasSize(Channel.values().length);
        for (Channel ch : Channel.values()) {
            ToneCurve curve = curves.getAdjustment(ch);
            assertThat(curve.curveData.x).containsExactly(0.0f, 1.0f);
            assertThat(curve.curveData.y).containsExactly(0.0f, 1.0f);
        }
    }

    @Test
    void resetAll() {
        ToneCurves curves = new ToneCurves();
        curves.setColorSpace(ColorSpace.OKLAB);
        curves.setActiveChannel(Channel.OK_A);
        curves.getAdjustment(Channel.OK_A).addKnot(0.3f, 0.7f, false);
        curves.getAdjustment(Channel.RED).addKnot(0.4f, 0.6f, false);

        AtomicInteger viewCount = new AtomicInteger();
        AtomicInteger listenerCount = new AtomicInteger();
        curves.setGUI(viewCount::incrementAndGet);
        curves.setAdjustmentListener(listenerCount::incrementAndGet);

        curves.resetAll();

        assertThat(curves.getColorSpace()).isEqualTo(ColorSpace.SRGB);
        assertThat(curves.getActiveChannel()).isEqualTo(Channel.RGB);
        for (Channel ch : Channel.values()) {
            ToneCurve curve = curves.getAdjustment(ch);
            assertThat(curve.curveData.x).containsExactly(0.0f, 1.0f);
            assertThat(curve.curveData.y).containsExactly(0.0f, 1.0f);
        }
        assertThat(viewCount.get()).isEqualTo(1);
        assertThat(listenerCount.get()).isEqualTo(1);
    }

    @Test
    void randomize() {
        ToneCurves curves = new ToneCurves();
        curves.setColorSpace(ColorSpace.OKLAB);
        curves.setActiveChannel(Channel.OK_B);

        // dirty another curve before randomize to verify it gets reset
        curves.getAdjustment(Channel.OK_A).addKnot(0.2f, 0.8f, false);

        AtomicInteger viewCount = new AtomicInteger();
        AtomicInteger listenerCount = new AtomicInteger();
        curves.setGUI(viewCount::incrementAndGet);
        curves.setAdjustmentListener(listenerCount::incrementAndGet);

        curves.randomize();

        // active color space and channel are preserved
        assertThat(curves.getColorSpace()).isEqualTo(ColorSpace.OKLAB);
        assertThat(curves.getActiveChannel()).isEqualTo(Channel.OK_B);

        // active curve has exactly 2 added knots (4 knots total)
        ToneCurve activeCurve = curves.getActiveAdjustment();
        assertThat(activeCurve.curveData.x).hasSize(4);
        assertThat(activeCurve.curveData.y).hasSize(4);

        // all other curves are reset to identity (2 knots)
        for (Channel ch : Channel.values()) {
            if (ch != Channel.OK_B) {
                ToneCurve otherCurve = curves.getAdjustment(ch);
                assertThat(otherCurve.curveData.x).containsExactly(0.0f, 1.0f);
                assertThat(otherCurve.curveData.y).containsExactly(0.0f, 1.0f);
            }
        }

        assertThat(viewCount.get()).isEqualTo(1);
        assertThat(listenerCount.get()).isEqualTo(1);
    }

    @Test
    void loadStateFrom_missingChannelKeysGiveIdentityCurves() {
        ToneCurves curves = new ToneCurves();
        // dirty curves first
        for (Channel ch : Channel.values()) {
            curves.getAdjustment(ch).addKnot(0.3f, 0.7f, false);
        }

        UserPreset preset = new UserPreset("test");
        preset.put(ColorSpace.PRESET_KEY, "OKLAB");
        preset.put(Channel.OK_L.getPresetKey(), "0.0,0.0#0.5,0.8#1.0,1.0");
        // OK_A and OK_B missing

        AtomicInteger viewCount = new AtomicInteger();
        AtomicInteger listenerCount = new AtomicInteger();
        curves.setGUI(viewCount::incrementAndGet);
        curves.setAdjustmentListener(listenerCount::incrementAndGet);

        curves.loadStateFrom(preset);

        assertThat(curves.getColorSpace()).isEqualTo(ColorSpace.OKLAB);
        assertThat(curves.getActiveChannel()).isEqualTo(Channel.OK_L);

        // loaded curve
        assertThat(curves.getAdjustment(Channel.OK_L).toSaveString())
            .isEqualTo("0.0,0.0#0.5,0.8#1.0,1.0");

        // missing OK_A and OK_B are identity
        assertThat(curves.getAdjustment(Channel.OK_A).curveData.x).containsExactly(0.0f, 1.0f);
        assertThat(curves.getAdjustment(Channel.OK_A).curveData.y).containsExactly(0.0f, 1.0f);
        assertThat(curves.getAdjustment(Channel.OK_B).curveData.x).containsExactly(0.0f, 1.0f);
        assertThat(curves.getAdjustment(Channel.OK_B).curveData.y).containsExactly(0.0f, 1.0f);

        // sRGB channels are also identity
        for (Channel ch : ColorSpace.SRGB.getChannels()) {
            assertThat(curves.getAdjustment(ch).curveData.x).containsExactly(0.0f, 1.0f);
            assertThat(curves.getAdjustment(ch).curveData.y).containsExactly(0.0f, 1.0f);
        }

        assertThat(viewCount.get()).isEqualTo(1);
        assertThat(listenerCount.get()).isEqualTo(1);
    }

    @Test
    void notificationsPerWholeStateOperation() {
        ToneCurves curves = new ToneCurves();
        List<String> events = new ArrayList<>();
        curves.setGUI(() -> events.add("view"));
        curves.setAdjustmentListener(() -> events.add("listener"));

        events.clear();
        curves.resetAll();
        assertThat(events).containsExactly("view", "listener");

        events.clear();
        curves.randomize();
        assertThat(events).containsExactly("view", "listener");

        events.clear();
        curves.loadStateFrom(new UserPreset("preset"));
        assertThat(events).containsExactly("view", "listener");
    }
}
