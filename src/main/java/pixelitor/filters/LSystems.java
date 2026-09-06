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
        BORDER("Border", "XYXYXYX+XYXYXYX+XYXYXYX+XYXYXYX", false, 0, 90) {
            private static final int[] START_ANGLES = {-27, 37, 10, -16, 47, 21, -6};

            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'F' -> "";
                    case 'X' -> "FX+FX+FXFY-FY-";
                    case 'Y' -> "+FX+FXFY-FY-FY";
                    default -> null;
                };
            }

            @Override
            int startAngle(int n) {
                assert n >= 1 && n <= START_ANGLES.length : "Unsupported iteration: " + n;
                return START_ANGLES[n - 1];
            }
        }, BOX("Box", "F+F+F+F", true, 0, 90) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'F' -> "FF+F+F+F+FF";
                    default -> null;
                };
            }
        }, CRYSTAL("Crystal", "F+F+F+F", true, 0, 90) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'F' -> "FF+F++F+F";
                    default -> null;
                };
            }
        }, PLANT("Fractal Plant", "A", false, -90, 25) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'A' -> "F+[[A]-A]-F[-FA]+A";
                    case 'F' -> "FF";
                    default -> null;
                };
            }
        }, GOSPER("Gosper/Flowsnake", "F", false, 0, 60) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'F' -> "F-G--G+F++FF+G-";
                    case 'G' -> "+F-GG--G-F++F+G";
                    default -> null;
                };
            }
        }, HILBERT("Hilbert Curve", "A", false, 0, 90) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'A' -> "+BF-AFA-FB+";
                    case 'B' -> "-AF+BFB+FA-";
                    default -> null;
                };
            }
        }, PENROSE("Penrose Tiling P3", "[B]++[B]++[B]++[B]++[B]", false, -90, 36) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'A' -> "CF++DF----BF[-CF----AF]++";
                    case 'B' -> "+CF--DF[---AF--BF]+";
                    case 'C' -> "-AF++BF[+++CF++DF]-";
                    case 'D' -> "--CF++++AF[+DF++++BF]--BF";
                    case 'F' -> "";
                    default -> null;
                };
            }
        }, PENTAPLEXITY("Pentaplexity", "F++F++F++F++F", true, 180, 36) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'F' -> "F++F++F+++++F-F++F";
                    default -> null;
                };
            }
        }, RING("Ring", "F+F+F+F", true, 0, 90) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'F' -> "FF+F+F+F+F+F-F";
                    default -> null;
                };
            }
        }, SIERPINSKI("Sierpiński", "F--XF--F--XF", false, 0, 45) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'X' -> "XF+G+XF--F--XF+G+X";
                    default -> null;
                };
            }
        }, SIERPINSKI_SQUARE("Sierpiński Square", "F+XF+F+XF", false, 0, 90) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'X' -> "XF-F+F-XF+F+XF-F+F-X";
                    default -> null;
                };
            }
        }, SIERPINSKI_ARROWHEAD("Sierpiński Arrowhead", "XF", false, 0, 60) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'X' -> "YF+XF+Y";
                    case 'Y' -> "XF-YF-X";
                    default -> null;
                };
            }

            @Override
            int startAngle(int n) {
                return (n % 2 == 0) ? 0 : -60;
            }
        }, SIERPINSKI_TRIANGLE("Sierpiński Triangle", "F-G-G", true, 0, 120) {
            @Override
            String rewrite(char c) {
                return switch (c) {
                    case 'F' -> "F-G+F+G-F";
                    case 'G' -> "GG";
                    default -> null;
                };
            }
        };

        private final String displayName;
        private final String axiom;

        // whether to draw the axiom in the first iteration
        private final boolean drawAxiom;

        private final int defaultStartAngle;
        private final int turnAngle;

        Type(String displayName, String axiom, boolean drawAxiom,
             int defaultStartAngle, int turnAngle) {
            this.displayName = displayName;
            this.axiom = axiom;
            this.drawAxiom = drawAxiom;
            this.defaultStartAngle = defaultStartAngle;
            this.turnAngle = turnAngle;
        }

        /**
         * Rewrites the given character according to the L-system rules,
         * or returns null if the character should remain unchanged.
         */
        abstract String rewrite(char c);

        /**
         * Creates a turtle configured for the specified max iteration number.
         */
        Turtle createTurtle(int n) {
            return new Turtle(startAngle(n), turnAngle);
        }

        int startAngle(int n) {
            return defaultStartAngle;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public LSystems() {
        type.limitOtherMaxWhen(iterations, selected -> 3);

        addParamsToFront(
            type,
            iterations
        );

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/L-system");
    }

    @Override
    protected Shape createCurve(int width, int height) {
        double margin = Math.max(10.0, strokeParam.getStrokeWidth() * 2.0);
        Type fractalType = type.getValue();

        int n = iterations.getValue();
        if (fractalType.drawAxiom) {
            n--;
        }

        String commands = iterate(fractalType, n);
        Turtle turtle = fractalType.createTurtle(n);
        Path2D path = turtle.interpret(commands);

        double startX = transform.getHorOffset(width);
        double startY = transform.getVerOffset(height);
        return Shapes.resizeToFit(path, width, height, startX, startY, margin);
    }

    /**
     * Generates the L-system command string for the given type and number of iterations.
     */
    private static String iterate(Type type, int iterations) {
        StringBuilder in = new StringBuilder(type.axiom);

        for (int i = 0; i < iterations; i++) {
            StringBuilder out = new StringBuilder(in.length() * 4);
            for (int j = 0, n = in.length(); j < n; j++) {
                char c = in.charAt(j);
                String replacement = type.rewrite(c);
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
        private static final double MOVE_DISTANCE = 10.0;

        private double x, y;

        // keep track of angles as int degrees in order to
        // avoid the accumulation of floating-point errors
        private int angle;
        private final int turnAngle;

        // the current state of the turtle
        private record State(double x, double y, int angle) {
        }

        private final ArrayDeque<State> stack = new ArrayDeque<>();

        private final Path2D path;

        public Turtle(int startAngle, int turnAngle) {
            this.angle = startAngle;
            this.turnAngle = turnAngle;

            // the turtle always starts at (0, 0), but the whole path
            // will be rescaled after the Shapes.resizeToFit method
            this.x = 0;
            this.y = 0;

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
            x += MOVE_DISTANCE * FastMath.cos(angleRadians);
            y += MOVE_DISTANCE * FastMath.sin(angleRadians);
            if (penDown) {
                path.lineTo(x, y);
            } else {
                path.moveTo(x, y);
            }
        }
    }
}
