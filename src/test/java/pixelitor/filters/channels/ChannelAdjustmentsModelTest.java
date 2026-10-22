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

package pixelitor.filters.channels;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.ChannelLookup;
import pixelitor.filters.lookup.FastLookupOp;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;

import java.awt.image.BufferedImageOp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelAdjustmentsModelTest {
    private ChannelAdjustmentsModel<FakeAdjustment> model;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void setUp() {
        model = new ChannelAdjustmentsModel<>(FakeAdjustment::new);
    }

    @Test
    void initialState() {
        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.SRGB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.RGB);
        assertThat(model.getActiveAdjustment().getChannel()).isEqualTo(Channel.RGB);
        assertThat(model.getAllAdjustments()).hasSize(Channel.values().length);
        for (Channel ch : Channel.values()) {
            assertThat(model.getAdjustment(ch)).isNotNull();
            assertThat(model.getAdjustment(ch).getChannel()).isEqualTo(ch);
            assertThat(model.getAdjustment(ch).getValue()).isEqualTo(0);
        }
    }

    @Test
    void resetAll() {
        model.setColorSpace(ColorSpace.OKLAB);
        model.setActiveChannel(Channel.OK_A);
        for (FakeAdjustment adj : model.getAllAdjustments()) {
            adj.setValue(10);
        }

        AtomicInteger viewCount = new AtomicInteger();
        AtomicInteger listenerCount = new AtomicInteger();
        model.setGUI(viewCount::incrementAndGet);
        model.setAdjustmentListener(listenerCount::incrementAndGet);

        model.resetAll();

        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.SRGB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.RGB);
        for (FakeAdjustment adj : model.getAllAdjustments()) {
            assertThat(adj.getValue()).isEqualTo(0);
            assertThat(adj.getResetCount()).isGreaterThan(0);
        }
        assertThat(viewCount.get()).isEqualTo(1);
        assertThat(listenerCount.get()).isEqualTo(1);
    }

    @Test
    void randomize() {
        model.setColorSpace(ColorSpace.OKLAB);
        model.setActiveChannel(Channel.OK_A);
        for (FakeAdjustment adj : model.getAllAdjustments()) {
            adj.setValue(10);
        }

        AtomicInteger viewCount = new AtomicInteger();
        AtomicInteger listenerCount = new AtomicInteger();
        model.setGUI(viewCount::incrementAndGet);
        model.setAdjustmentListener(listenerCount::incrementAndGet);

        model.randomize();

        // active color space and channel are preserved
        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.OKLAB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.OK_A);

        // active adjustment is randomized
        FakeAdjustment active = model.getActiveAdjustment();
        assertThat(active.getRandomizeCount()).isEqualTo(1);
        assertThat(active.getValue()).isEqualTo(42);

        // all other adjustments are reset, not randomized
        for (FakeAdjustment adj : model.getAllAdjustments()) {
            if (adj != active) {
                assertThat(adj.getRandomizeCount()).isEqualTo(0);
                assertThat(adj.getValue()).isEqualTo(0);
                assertThat(adj.getResetCount()).isGreaterThan(0);
            }
        }

        assertThat(viewCount.get()).isEqualTo(1);
        assertThat(listenerCount.get()).isEqualTo(1);
    }

    @Test
    void saveStateTo() {
        // SRGB: only sRGB channels and color_space are saved
        model.getAdjustment(Channel.RGB).setValue(1);
        model.getAdjustment(Channel.RED).setValue(2);
        model.getAdjustment(Channel.GREEN).setValue(3);
        model.getAdjustment(Channel.BLUE).setValue(4);
        model.getAdjustment(Channel.OK_L).setValue(5);

        AtomicInteger listenerCount = new AtomicInteger();
        model.setAdjustmentListener(listenerCount::incrementAndGet);

        UserPreset srgbPreset = new UserPreset("srgb");
        model.saveStateTo(srgbPreset);

        assertThat(srgbPreset.get(ColorSpace.PRESET_KEY)).isEqualTo("SRGB");
        assertThat(srgbPreset.getInt(Channel.RGB.getPresetKey())).isEqualTo(1);
        assertThat(srgbPreset.getInt(Channel.RED.getPresetKey())).isEqualTo(2);
        assertThat(srgbPreset.getInt(Channel.GREEN.getPresetKey())).isEqualTo(3);
        assertThat(srgbPreset.getInt(Channel.BLUE.getPresetKey())).isEqualTo(4);
        assertThat(srgbPreset.get(Channel.OK_L.getPresetKey())).isNull();
        assertThat(listenerCount.get()).isZero();

        // OKLAB: only Oklab channels and color_space are saved
        model.setColorSpace(ColorSpace.OKLAB);
        model.getAdjustment(Channel.OK_A).setValue(6);
        model.getAdjustment(Channel.OK_B).setValue(7);

        UserPreset oklabPreset = new UserPreset("oklab");
        model.saveStateTo(oklabPreset);

        assertThat(oklabPreset.get(ColorSpace.PRESET_KEY)).isEqualTo("OKLAB");
        assertThat(oklabPreset.getInt(Channel.OK_L.getPresetKey())).isEqualTo(5);
        assertThat(oklabPreset.getInt(Channel.OK_A.getPresetKey())).isEqualTo(6);
        assertThat(oklabPreset.getInt(Channel.OK_B.getPresetKey())).isEqualTo(7);
        assertThat(oklabPreset.get(Channel.RGB.getPresetKey())).isNull();
        assertThat(oklabPreset.get(Channel.RED.getPresetKey())).isNull();
        assertThat(listenerCount.get()).isZero();
    }

    @Test
    void loadStateFrom_oklabAndMissingValues() {
        // dirty existing state
        for (FakeAdjustment adj : model.getAllAdjustments()) {
            adj.setValue(99);
        }

        UserPreset preset = new UserPreset("test");
        preset.put(ColorSpace.PRESET_KEY, "OKLAB");
        preset.putInt(Channel.OK_L.getPresetKey(), 10);
        // OK_A and OK_B omitted from preset

        AtomicInteger viewCount = new AtomicInteger();
        AtomicInteger listenerCount = new AtomicInteger();
        model.setGUI(viewCount::incrementAndGet);
        model.setAdjustmentListener(listenerCount::incrementAndGet);

        model.loadStateFrom(preset);

        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.OKLAB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.OK_L);

        // loaded value
        assertThat(model.getAdjustment(Channel.OK_L).getValue()).isEqualTo(10);
        // missing values reset to default 0, previous state 99 is discarded
        assertThat(model.getAdjustment(Channel.OK_A).getValue()).isEqualTo(0);
        assertThat(model.getAdjustment(Channel.OK_B).getValue()).isEqualTo(0);
        assertThat(model.getAdjustment(Channel.RGB).getValue()).isEqualTo(0);
        assertThat(model.getAdjustment(Channel.RED).getValue()).isEqualTo(0);

        assertThat(viewCount.get()).isEqualTo(1);
        assertThat(listenerCount.get()).isEqualTo(1);
    }

    @Test
    void loadStateFrom_missingColorSpaceDefaultsToSrgb() {
        model.setColorSpace(ColorSpace.OKLAB);
        model.setActiveChannel(Channel.OK_B);

        UserPreset preset = new UserPreset("no_cs");
        preset.putInt(Channel.RED.getPresetKey(), 77);

        model.loadStateFrom(preset);

        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.SRGB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.RGB);
        assertThat(model.getAdjustment(Channel.RED).getValue()).isEqualTo(77);
        assertThat(model.getAdjustment(Channel.GREEN).getValue()).isEqualTo(0);
    }

    @Test
    void wholeStateOperations_triggerExactlyOneNotificationInOrder() {
        List<String> events = new ArrayList<>();
        model.setGUI(() -> events.add("view"));
        model.setAdjustmentListener(() -> events.add("listener"));

        events.clear();
        model.resetAll();
        assertThat(events).containsExactly("view", "listener");

        events.clear();
        model.randomize();
        assertThat(events).containsExactly("view", "listener");

        events.clear();
        model.loadStateFrom(new UserPreset("empty"));
        assertThat(events).containsExactly("view", "listener");
    }

    @Test
    void fireAdjustmentChanged_runsListenerOnly() {
        AtomicInteger viewCount = new AtomicInteger();
        AtomicInteger listenerCount = new AtomicInteger();
        model.setGUI(viewCount::incrementAndGet);
        model.setAdjustmentListener(listenerCount::incrementAndGet);

        model.fireAdjustmentChanged();

        assertThat(listenerCount.get()).isEqualTo(1);
        assertThat(viewCount.get()).isZero();

        // sub-model triggering listener directly
        model.getActiveAdjustment().notifyChanged();
        assertThat(listenerCount.get()).isEqualTo(2);
        assertThat(viewCount.get()).isZero();
    }

    @Test
    void noNpeWithoutViewOrListener() {
        model.setGUI(null);
        model.setAdjustmentListener(null);

        model.resetAll();
        model.randomize();
        model.saveStateTo(new UserPreset("test"));
        model.loadStateFrom(new UserPreset("test"));
        model.fireAdjustmentChanged();
        model.resetActiveAdjustment();
    }

    @Test
    void silentMutators_doNotNotify() {
        AtomicInteger viewCount = new AtomicInteger();
        AtomicInteger listenerCount = new AtomicInteger();
        model.setGUI(viewCount::incrementAndGet);
        model.setAdjustmentListener(listenerCount::incrementAndGet);

        boolean csChanged = model.setColorSpace(ColorSpace.OKLAB);
        assertThat(csChanged).isTrue();

        boolean chChanged = model.setActiveChannel(Channel.OK_A);
        assertThat(chChanged).isTrue();

        model.getAdjustment(Channel.OK_A).setValue(50);
        model.resetActiveAdjustment();
        assertThat(model.getAdjustment(Channel.OK_A).getValue()).isEqualTo(0);

        assertThat(viewCount.get()).isZero();
        assertThat(listenerCount.get()).isZero();
    }

    @Test
    void setColorSpace_behavior() {
        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.SRGB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.RGB);

        // change to OKLAB: selects primary channel OK_L, returns true
        assertThat(model.setColorSpace(ColorSpace.OKLAB)).isTrue();
        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.OKLAB);
        assertThat(model.getActiveChannel()).isEqualTo(Channel.OK_L);

        // same space: returns false, active channel untouched
        model.setActiveChannel(Channel.OK_A);
        assertThat(model.setColorSpace(ColorSpace.OKLAB)).isFalse();
        assertThat(model.getActiveChannel()).isEqualTo(Channel.OK_A);

        // null: returns false
        assertThat(model.setColorSpace(null)).isFalse();
        assertThat(model.getColorSpace()).isEqualTo(ColorSpace.OKLAB);
    }

    @Test
    void setActiveChannel_behavior() {
        // valid change in current color space
        assertThat(model.setActiveChannel(Channel.RED)).isTrue();
        assertThat(model.getActiveChannel()).isEqualTo(Channel.RED);

        // already active: returns false
        assertThat(model.setActiveChannel(Channel.RED)).isFalse();

        // channel from another color space: ignored, returns false
        assertThat(model.setActiveChannel(Channel.OK_L)).isFalse();
        assertThat(model.getActiveChannel()).isEqualTo(Channel.RED);

        // null: ignored, returns false
        assertThat(model.setActiveChannel(null)).isFalse();
        assertThat(model.getActiveChannel()).isEqualTo(Channel.RED);
    }

    @Test
    void createOp_returnsExpectedOpPerColorSpace() {
        BufferedImageOp srgbOp = model.createOp();
        assertThat(srgbOp).isInstanceOf(FastLookupOp.class);

        model.setColorSpace(ColorSpace.OKLAB);
        BufferedImageOp oklabOp = model.createOp();
        assertThat(oklabOp.getClass().getSimpleName()).isEqualTo("OklabLookupFilter");
    }

    private static class FakeAdjustment implements ChannelAdjustment {
        private final Channel channel;
        private final Runnable listener;
        private int value;
        private int resetCount;
        private int randomizeCount;

        FakeAdjustment(Channel channel, Runnable listener) {
            this.channel = channel;
            this.listener = listener;
        }

        @Override
        public Channel getChannel() {
            return channel;
        }

        @Override
        public ChannelLookup toLookup() {
            return ChannelLookup.getIdentity();
        }

        @Override
        public void reset() {
            resetCount++;
            value = 0;
        }

        @Override
        public void randomize() {
            randomizeCount++;
            value = 42;
        }

        @Override
        public void saveTo(UserPreset preset) {
            preset.putInt(channel.getPresetKey(), value);
        }

        @Override
        public void loadFrom(UserPreset preset) {
            String val = preset.get(channel.getPresetKey());
            if (val != null) {
                value = Integer.parseInt(val);
            }
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public int getResetCount() {
            return resetCount;
        }

        public int getRandomizeCount() {
            return randomizeCount;
        }

        public void notifyChanged() {
            if (listener != null) {
                listener.run();
            }
        }
    }
}
