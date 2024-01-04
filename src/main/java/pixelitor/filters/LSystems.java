/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "";
                    case 'X' -> "FX+FX+FXFY-FY-";
                    case 'Y' -> "+FX+FXFY-FY-FY";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case 'F' -> "FF+F+F+F+FF";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case 'F' -> "FF+F++F+F";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case '[' -> "[";
                    case ']' -> "]";
                    case 'A' -> "F+[[A]-A]-F[-FA]+A";
                    case 'F' -> "FF";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "F-G--G+F++FF+G-";
                    case 'G' -> "+F-GG--G-F++F+G";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "F";
                    case 'A' -> "+BF-AFA-FB+";
                    case 'B' -> "-AF+BFB+FA-";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case '[' -> "[";
                    case ']' -> "]";
                    case 'A' -> "CF++DF----BF[-CF----AF]++";
                    case 'B' -> "+CF--DF[---AF--BF]+";
                    case 'C' -> "-AF++BF[+++CF++DF]-";
                    case 'D' -> "--CF++++AF[+DF++++BF]--BF";
                    case 'F' -> "";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "F++F++F+++++F-F++F";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "FF+F+F+F+F+F-F";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "F";
                    case 'G' -> "G";
                    case 'X' -> "XF+G+XF--F--XF+G+X";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "F";
                    case 'X' -> "XF-F+F-XF+F+XF-F+F-X";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "F";
                    case 'X' -> "YF+XF+Y";
                    case 'Y' -> "XF-YF-X";
                    default -> throw new IllegalStateException("c = " + c);
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
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "F-G+F+G-F";
                    case 'G' -> "GG";
                    default -> throw new IllegalStateException("c = " + c);
                };
            }

            @Override
            Turtle createTurtle(int n) {
                return new Turtle(0, 120);
            }
        };

        private final String guiName;
        private final String axiom;

        // Whether to draw the axiom in the first iteration
        private final boolean drawAxiom;

        Type(String guiName, String axiom, boolean drawAxiom) {
            this.guiName = guiName;
            this.axiom = axiom;
            this.drawAxiom = drawAxiom;
        }

        abstract RewriteRule rewriteRule();

        abstract Turtle createTurtle(int n);

        @Override
        public String toString() {
            return guiName;
        }
    }

    public LSystems() {
        type.setupMaximizeOtherOnChange(iterations, 3);

        addParamsToFront(
            type,
            iterations
        );

        helpURL = "https://en.wikipedia.org/wiki/L-system";
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

        return Shapes.resizeToFit(path, width, height, margin);
    }

    private static String iterate(Type type, int order) {
        RewriteRule rule = type.rewriteRule();
        StringBuilder in = new StringBuilder(type.axiom);

        for (int i = 0; i < order; i++) {
            StringBuilder out = new StringBuilder();
            for (int j = 0, n = in.length(); j < n; j++) {
                out.append(rule.replace(in.charAt(j)));
            }
            in = out;
        }
        return in.toString();
    }

    private static class Turtle {
        private double x, y;

        // keep track of angles as int degrees in order to
        // avoid the accumulation of floating-point errors
        private int angle;
        private final int turnAngle;

        // The current state of the turtle.
        private record State(double x, double y, int angle) {
        }

        private final ArrayDeque<State> stack = new ArrayDeque<>();

        private final double moveDistance;
        private final Path2D path;

        public Turtle(int startAngle, int turnAngle) {
            this.angle = startAngle;
            this.turnAngle = turnAngle;
            this.x = 0;
            this.y = 0;
            this.moveDistance = 10;

            path = new Path2D.Double();
            path.moveTo(x, y);
        }

        public Path2D interpret(String s) {
            for (int i = 0, n = s.length(); i < n; i++) {
                switch (s.charAt(i)) {
                    case 'F', 'G' -> moveForward();
                    case '+' -> turnLeft();
                    case '-' -> turnRight();
                    case '[' -> push();
                    case ']' -> pop();
//                    case 'b' -> moveForwardPenUp();
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

        private void moveForward() {
            double angleRadians = Math.toRadians(angle);
            x += moveDistance * FastMath.cos(angleRadians);
            y += moveDistance * FastMath.sin(angleRadians);
            path.lineTo(x, y);
        }

        private void moveForwardPenUp() {
            double angleRadians = Math.toRadians(angle);
            x += moveDistance * FastMath.cos(angleRadians);
            y += moveDistance * FastMath.sin(angleRadians);
            path.moveTo(x, y);
        }
    }

    /**
     * Defines the rewriting rules for an L-system.
     */
    private interface RewriteRule {
        String replace(char c);
    }
}
