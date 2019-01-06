/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2018 the original author or authors.
 */

package pixelitor.guitest;

import org.assertj.swing.dependency.jsr305.Nonnull;

import javax.swing.*;
import java.lang.ref.WeakReference;

import static javax.swing.SwingUtilities.isEventDispatchThread;
import static org.assertj.core.util.Preconditions.checkNotNull;

/**
 * A temporary class until assertj-swing releases a version
 * which contains https://github.com/joel-costigliola/assertj-swing/pull/228
 */
abstract class FixedCheckThreadViolationRepaintManager extends RepaintManager {
    // Should always be turned on because it shouldn't matter
    // whether the component is showing (realized) or not.
    // This flag exists only for historical reasons, see
    // https://stackoverflow.com/questions/491323/is-it-safe-to-construct-swing-awt-widgets-not-on-the-event-dispatch-thread
    private final boolean completeCheck;

    private WeakReference<JComponent> lastComponent;

    FixedCheckThreadViolationRepaintManager() {
        // it is recommended to pass the complete check
        this(true);
    }

    FixedCheckThreadViolationRepaintManager(boolean completeCheck) {
        this.completeCheck = completeCheck;
    }

    @Override
    public synchronized void addInvalidComponent(JComponent component) {
        checkThreadViolations(checkNotNull(component));
        super.addInvalidComponent(component);
    }

    @Override
    public void addDirtyRegion(JComponent component, int x, int y, int w, int h) {
        checkThreadViolations(checkNotNull(component));
        super.addDirtyRegion(component, x, y, w, h);
    }

    /**
     * Rules enforced by this method:
     * (1) it is always OK to reach this method on the Event Dispatch Thread.
     * (2) it is generally not OK to reach this method outside the Event Dispatch Thread.
     * (3) (exception form rule 2) except when we get here from a repaint() call, because repaint() is thread-safe
     * (4) (exception from rule 3) it is not OK if swing code calls repaint() outside the EDT, because swing code should be called on the EDT.
     * (5) (exception from rule 4) using SwingWorker subclasses should not be considered swing code.
     */
    private void checkThreadViolations(@Nonnull JComponent c) {
        if (!isEventDispatchThread() && (completeCheck || c.isShowing())) {
            boolean imageUpdate = false;
            boolean repaint = false;
            boolean fromSwing = false; // whether we were in a swing method before before the repaint() call
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement st : stackTrace) {
                if (repaint
                    && st.getClassName().startsWith("javax.swing.")
                    && !st.getClassName().startsWith("javax.swing.SwingWorker")) {
                    fromSwing = true;
                }
                if (repaint && "imageUpdate".equals(st.getMethodName())) {
                    imageUpdate = true;
                }
                if ("repaint".equals(st.getMethodName())) {
                    repaint = true;
                    fromSwing = false;
                }
            }
            if (imageUpdate) {
                // assuming it is java.awt.image.ImageObserver.imageUpdate(...)
                // image was asynchronously updated, that's ok
                return;
            }
            if (repaint && !fromSwing) {
                // no problems here, since repaint() is thread safe
                return;
            }
            // ignore the last processed component
            if (lastComponent != null && c == lastComponent.get()) {
                return;
            }
            lastComponent = new WeakReference<>(c);
            violationFound(c, stackTrace);
        }
    }

    abstract void violationFound(@Nonnull JComponent c, @Nonnull StackTraceElement[] stackTrace);
}
