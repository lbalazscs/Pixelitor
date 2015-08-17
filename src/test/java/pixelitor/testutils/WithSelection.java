package pixelitor.testutils;

import pixelitor.CompTester;

import java.awt.Rectangle;

/**
 * Whether there is a selection present when a test runs
 */
public enum WithSelection {
    YES {
        @Override
        public void init(CompTester tester) {
            tester.setStandardTestSelection();
            Rectangle selectionShape = tester.getStandardTestSelectionShape();
            tester.checkSelectionBounds(selectionShape);
        }
    }, NO {
        @Override
        public void init(CompTester tester) {
            // do nothing
        }
    };

    public abstract void init(CompTester tester);
}
