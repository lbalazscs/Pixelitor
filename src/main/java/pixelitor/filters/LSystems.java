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

package pixelitor.filters;

import net.jafama.FastMath;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.Help;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Shapes;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.io.Serial;
import java.util.ArrayDeque;

/**
 * The "Render/Curves/L-Systems" filter.
 */
public class LSystems extends CurveFilter {
    public static final String NAME = "L-Systems";

    @Serial
    private static final long serialVersionUID = 1L;

    private final EnumParam<Type> type = new EnumParam<>("Type", Type.class);
    private final RangeParam iterations = new RangeParam("Iterations", 1, 3, 7);

    private enum Type {
        BORDER("Border", "XYXYXYX+XYXYXYX+XYXYXYX+XYXYXYX", false) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'F' -> "";
                    case 'X' -> "FX+FX+FXFY-FY-";
                    case 'Y' -> "+FX+FXFY-FY-FY";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                int startAngle = switch (n) {
                    case 1 -> -27;
                    case 2 -> 37;
                    case 3 -> 10;
                    case 4 -> -16;
                    case 5 -> 47;
                    case 6 -> 21;
                    case 7 -> -6;
                    default -> throw new IllegalStateException("n = " + n);
                };
                return new Turtle(startAngle, 90);
            }
        }, BOX("Box", "F+F+F+F", true) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'F' -> "FF+F+F+F+FF";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(0, 90);
            }
        }, CRYSTAL("Crystal", "F+F+F+F", true) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'F' -> "FF+F++F+F";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(0, 90);
            }
        }, PLANT("Fractal Plant", "A", false) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'A' -> "F+[[A]-A]-F[-FA]+A";
                    case 'F' -> "FF";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(-90, 25);
            }
        }, GOSPER("Gosper/Flowsnake", "F", false) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'F' -> "F-G--G+F++FF+G-";
                    case 'G' -> "+F-GG--G-F++F+G";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(0, 60);
            }
        }, HILBERT("Hilbert Curve", "A", false) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'A' -> "+BF-AFA-FB+";
                    case 'B' -> "-AF+BFB+FA-";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(0, 90);
            }
        }, PENROSE("Penrose Tiling P3", "[B]++[B]++[B]++[B]++[B]", false) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'A' -> "CF++DF----BF[-CF----AF]++";
                    case 'B' -> "+CF--DF[---AF--BF]+";
                    case 'C' -> "-AF++BF[+++CF++DF]-";
                    case 'D' -> "--CF++++AF[+DF++++BF]--BF";
                    case 'F' -> "";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(-90, 36);
            }
        }, PENTAPLEXITY("Pentaplexity", "F++F++F++F++F", true) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'F' -> "F++F++F+++++F-F++F";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(180, 36);
            }
        }, RING("Ring", "F+F+F+F", true) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'F' -> "FF+F+F+F+F+F-F";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(0, 90);
            }
        }, SIERPINSKI("Sierpiński", "F--XF--F--XF", false) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'X' -> "XF+G+XF--F--XF+G+X";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(0, 45);
            }
        }, SIERPINSKI_SQUARE("Sierpiński Square", "F+XF+F+XF", false) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'X' -> "XF-F+F-XF+F+XF-F+F-X";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(0, 90);
            }
        }, SIERPINSKI_ARROWHEAD("Sierpiński Arrowhead", "XF", false) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'X' -> "YF+XF+Y";
                    case 'Y' -> "XF-YF-X";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                int startAngle = n % 2 == 0 ? 0 : -60;
                return new Turtle(startAngle, 60);
            }
        }, SIERPINSKI_TRIANGLE("Sierpiński Triangle", "F-G-G", true) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case 'F' -> "F-G+F+G-F";
                    case 'G' -> "GG";
                    default -> null;
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(0, 120);
            }
        };

        private final String displayName;
        private final String axiom;

        // whether to draw the axiom in the first iteration
        private final boolean drawAxiom;

        Type(String displayName, String axiom, boolean drawAxiom) {
            this.displayName = displayName;
            this.axiom = axiom;
            this.drawAxiom = drawAxiom;
        }

        /**
         * Returns the rewrite rule for this L-system type.
         */
        abstract RewriteRule rewriteRule();

        /**
         * Creates a turtle configured for the specified max iteration number.
         */
        abstract Turtle createTurtle(int n);

        @Override
        public String toString() {
            return displayName;
        }
    }

    public LSystems() {
        type.setupLimitOtherToMax(iterations, selected -> 3);

        addParamsToFront(
            type,
            iterations
        );

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/L-system");
    }

    @Override
    protected Shape createCurve(int width, int height) {
        double margin = Math.max(10.0, strokeParam.getStrokeWidth() * 2.0);
        Type fractalType = type.getSelected();

        int n = iterations.getValue();
        if (fractalType.drawAxiom) {
            n--;
        }

        String commands = iterate(fractalType, n);
        Turtle turtle = fractalType.createTurtle(n);
        Path2D path = turtle.interpret(commands);

        return Shapes.resizeToFit(path, width, height, margin,
            transform.getHorOffset(width),
            transform.getVerOffset(height));
    }

    /**
     * Generates the L-system command string for the given type and number of iterations.
     */
    private static String iterate(Type type, int iterations) {
        RewriteRule rule = type.rewriteRule();
        StringBuilder in = new StringBuilder(type.axiom);

        for (int i = 0; i < iterations; i++) {
            StringBuilder out = new StringBuilder(in.length() * 4);
            for (int j = 0, n = in.length(); j < n; j++) {
                char c = in.charAt(j);
                String replacement = rule.replace(c);
                if (replacement != null) {
                    out.append(replacement);
                } else {
                    // preserve characters that don't have a rewrite rule
                    out.append(c);
                }
            }
            in = out;
        }
        return in.toString();
    }

    /**
     * Interprets L-system commands to draw a path.
     */
    private static class Turtle {
        private double x, y;

        // keep track of angles as int degrees in order to
        // avoid the accumulation of floating-point errors
        private int angle;
        private final int turnAngle;

        // the current state of the turtle
        private record State(double x, double y, int angle) {
        }

        private final ArrayDeque<State> stack = new ArrayDeque<>();

        private final double moveDistance;
        private final Path2D path;

        public Turtle(int startAngle, int turnAngle) {
            this.angle = startAngle;
            this.turnAngle = turnAngle;

            // the turtle always starts at (0, 0), but the whole path
            // will be rescaled after the Shapes.resizeToFit method
            this.x = 0;
            this.y = 0;
            this.moveDistance = 10;

            path = new Path2D.Double();
            path.moveTo(x, y);
        }

        /**
         * Processes a sequence of L-system commands and returns the resulting path.
         */
        public Path2D interpret(String s) {
            for (int i = 0, n = s.length(); i < n; i++) {
                switch (s.charAt(i)) {
                    case 'F', 'G' -> moveForward(true);
                    case '+' -> turnLeft();
                    case '-' -> turnRight();
                    case '[' -> push();
                    case ']' -> pop();
//                    case 'b' -> moveForward(false);
                }
            }
            return path;
        }

        private void turnLeft() {
            angle += turnAngle;
        }

        private void turnRight() {
            angle -= turnAngle;
        }

        private void push() {
            stack.push(new State(x, y, angle));
        }

        private void pop() {
            State state = stack.pop();
            x = state.x();
            y = state.y();
            angle = state.angle();
            path.moveTo(x, y);
        }

        private void moveForward(boolean penDown) {
            double angleRadians = Math.toRadians(angle);
            x += moveDistance * FastMath.cos(angleRadians);
            y += moveDistance * FastMath.sin(angleRadians);
            if (penDown) {
                path.lineTo(x, y);
            } else {
                path.moveTo(x, y);
            }
        }
    }

    /**
     * Defines the rewriting rules for an L-system.
     */
    private interface RewriteRule {
        /**
         * Returns the replacement string for the character, or null if it should remain unchanged.
         */
        String replace(char c);
    }
}
