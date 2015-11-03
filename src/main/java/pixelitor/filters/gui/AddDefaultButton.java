package pixelitor.filters.gui;

public enum AddDefaultButton {
    YES {
        @Override
        public boolean isYes() {
            return true;
        }
    }, NO {
        @Override
        public boolean isYes() {
            return false;
        }
    };

    public abstract boolean isYes();
}
