package pixelitor.filters.animation;

/**
 * http://www.wolframalpha.com/input/?i=Plot[{x%2C+x*x%2C+-x*%28x+-+2%29%2C++x*x*%283+-+2*x%29}%2C+{x%2C+0%2C+1}]
 */
public enum Interpolation {
    LINEAR {
        @Override
        double time2progress(double time) {
            return time;
        }

        @Override
        public String toString() {
            return "Linear (uniform)";
        }
    }, QUAD_EASE_IN {
        @Override
        double time2progress(double time) {
            return time * time;
        }

        @Override
        public String toString() {
            return "Ease In (slow start)";
        }
    }, QUAD_EASE_OUT {
        @Override
        double time2progress(double time) {
            return -time * (time - 2);
        }

        @Override
        public String toString() {
            return "Ease Out (slow stop)";
        }
    }, EASE_IN_OUT {
        @Override
        double time2progress(double time) {
            return time * time * (3 - 2 * time);
        }

        @Override
        public String toString() {
            return "Ease In and Out";
        }
    };

    /**
     * Transforms the normalized time into the normalized animation progress
     *
     * @param time a value between 0 and 1
     * @return a value between 0 and 1
     */
    abstract double time2progress(double time);
}
