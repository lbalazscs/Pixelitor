package pixelitor.tools;

import java.awt.event.MouseEvent;

public enum Shift {
    YES {
        @Override
        public int modify(int in) {
            in |= MouseEvent.SHIFT_DOWN_MASK;
            in |= MouseEvent.SHIFT_MASK;

            return in;
        }
    }, NO {
        @Override
        public int modify(int in) {
            return in;
        }
    };

    public abstract int modify(int in);
}
