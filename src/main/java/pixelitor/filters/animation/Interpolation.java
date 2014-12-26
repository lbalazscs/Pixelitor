package pixelitor.filters.animation;

/**
 * http://www.wolframalpha.com/input/?i=Plot[{x%2C+x*x%2C+-x*%28x+-+2%29%2C++x*x*%283+-+2*x%29}%2C+{x%2C+0%2C+1}]
 */
public enum Interpolation {
    LINEAR("Linear (uniform)") {
        @Override
        double time2progress(double time) {
            return time;
        }
    }, QUAD_EASE_IN("Ease In (slow start)") {
        @Override
        double time2progress(double time) {
            return time * time;
        }
    }, QUAD_EASE_OUT("Ease Out (slow stop)") {
        @Override
        double time2progress(double time) {
            return -time * (time - 2);
        }
    }, EASE_IN_OUT("Ease In and Out") {
        @Override
        double time2progress(double time) {
            return time * time * (3 - 2 * time);
        }
    };

    private final String guiName;

    Interpolation(String guiName) {
        this.guiName = guiName;
    }

    /**
     * Transforms the normalized time into the normalized animation progress
     *
     * @param time a value between 0 and 1
     * @return a value between 0 and 1
     */
    abstract double time2progress(double time);

    @Override
    public String toString() {
        return guiName;
    }
}
