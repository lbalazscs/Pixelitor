package pixelitor.tools;

import java.awt.event.MouseEvent;

public enum Mouse {
    LEFT {
        @Override
        public int modify(int in) {
            in |= MouseEvent.BUTTON1_DOWN_MASK;
            in |= MouseEvent.BUTTON1_MASK;

            return in;
        }
    }, RIGHT {
        @Override
        public int modify(int in) {
            in |= MouseEvent.BUTTON3_DOWN_MASK;
            in |= MouseEvent.BUTTON3_MASK;

            return in;
        }
    };

    public abstract int modify(int in);
}
