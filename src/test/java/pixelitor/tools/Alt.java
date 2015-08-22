package pixelitor.tools;

import java.awt.event.MouseEvent;

public enum Alt {
    YES {
        @Override
        public int modify(int in) {
            in |= MouseEvent.ALT_DOWN_MASK;
            in |= MouseEvent.ALT_MASK;
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
