package pixelitor.tools;

import java.awt.event.MouseEvent;

public enum Ctrl {
    YES {
        @Override
        public int modify(int in) {
            in |= MouseEvent.CTRL_DOWN_MASK;
            in |= MouseEvent.CTRL_MASK;

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
