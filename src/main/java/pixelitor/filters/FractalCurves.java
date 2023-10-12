/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

public class FractalCurves extends ShapeFilter {
    public static final String NAME = "Fractal Curves";

    @Serial
    private static final long serialVersionUID = 1L;

    private final EnumParam<Type> type = new EnumParam<>("Type", Type.class);
    private final RangeParam iterations = new RangeParam("Iterations", 1, 3, 7);

    private enum Type {
        // Dragon curve: works, but it becomes interesting only after the 10th iteration
/*        DRAGON("Dragon", "FA", true) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "F";
                    case 'A' -> "A+BF+";
                    case 'B' -> "-FA-B";
                    default -> throw new IllegalStateException("c = " + c);
                };
            }

            @Override
            Turtle createTurtle(int width, int height, double margin, int n) {
                return new Turtle(0, 0, 10, 0, 90);
            }
        },*/ GOSPER("Gosper/Flowsnake", "F", true) {
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
            Turtle createTurtle(int width, int height, double margin, int n) {
                return new Turtle(0, 0, 10, 0, 60);
            }
        }, HILBERT("Hilbert", "A", false) {
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
            Turtle createTurtle(int width, int height, double margin, int n) {
                double startX;
                double startY = Type.calcTopY(width, height, margin);
                if (width >= height) {
                    startX = width / 2.0 - height / 2.0 + margin;
                } else {
                    startX = margin;
                }
                double moveDistance = (Math.min(width, height) - 2 * margin) / (Math.pow(2, n) - 1);
                return new Turtle(startX, startY, moveDistance, 0, 90);
            }
        }, // Peano: works, but slow and not very interesting
        /*PEANO("Peano", "A", false) {
            @Override
            RewriteRule rewriteRule() {
                return c -> switch (c) {
                    case '+' -> "+";
                    case '-' -> "-";
                    case 'F' -> "F";
                    case 'A' -> "AFBFA-F-BFAFB+F+AFBFA";
                    case 'B' -> "BFAFB+F+AFBFA-F-BFAFB";
                    default -> throw new IllegalStateException("c = " + c);
                };
            }

            @Override
            Turtle createTurtle(int width, int height, double margin, int n) {
                double startX;
                double startY = Type.calcBottomY(width, height, margin);
                if (width >= height) {
                    startX = width / 2.0 - height / 2.0 + margin;
                } else {
                    startX = margin;
                }
                double moveDistance = (Math.min(width, height) - 2 * margin) / (Math.pow(3, n) - 1);
                return new Turtle(startX, startY, moveDistance, 0, 90);
            }
        }*/ SIERPINSKI("Sierpiński", "F--XF--F--XF", false) {
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
            Turtle createTurtle(int width, int height, double margin, int n) {
                double steps = Math.pow(2, n + 1);
                double scaling = (steps - 1) + (steps - 2) / 1.4142135623730951;

                double moveDistance = (Math.min(width, height) - 2 * margin) / scaling;
                double startX = width / 2.0 - moveDistance / 2.0;
                double startY = Type.calcBottomY(width, height, margin);
                return new Turtle(startX, startY, moveDistance, 0, 45);
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
            Turtle createTurtle(int width, int height, double margin, int n) {
                double scaling = 1 + 2 * (2 * Math.pow(2, n) - 2);
                double moveDistance = (Math.min(width, height) - 2 * margin) / scaling;

                double startX = width / 2.0 - moveDistance / 2.0;
                double startY = Type.calcTopY(width, height, margin);
                return new Turtle(startX, startY, moveDistance, 0, 90);
            }
        }, SIERPINSKI_ARROWHEAD("Sierpiński Arrowhead", "XF", true) {
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
            Turtle createTurtle(int width, int height, double margin, int n) {
                int startAngle = n % 2 == 0 ? 0 : -60;
                return new Turtle(0, 0, 10, startAngle, 60);
            }
        };

        private final String guiName;
        private final String axiom;
        private final boolean resize;

        Type(String guiName, String axiom, boolean resize) {
            this.guiName = guiName;
            this.axiom = axiom;
            this.resize = resize;
        }

        abstract RewriteRule rewriteRule();

        abstract Turtle createTurtle(int width, int height, double margin, int n);

        private static double calcTopY(int width, int height, double margin) {
            if (width >= height) {
                return margin;
            } else {
                return height / 2.0 - width / 2.0 + margin;
            }
        }

        private static double calcBottomY(int width, int height, double margin) {
            if (width >= height) {
                return height - margin;
            } else {
                return height / 2.0 + width / 2.0 + margin;
            }
        }

        @Override
        public String toString() {
            return guiName;
        }
    }

    public FractalCurves() {
        addParamsToFront(
            type,
            iterations
        );

        helpURL = "https://en.wikipedia.org/wiki/Fractal_curve";
    }

    @Override
    protected Shape createShape(int width, int height) {
        double margin = Math.max(10.0, strokeParam.getStrokeWidth() * 2.0);
        int n = iterations.getValue();

        Type fractalType = type.getSelected();
        Turtle turtle = fractalType.createTurtle(width, height, margin, n);

        String commands = rewrite(fractalType, n);
        turtle.interpret(commands);

        Path2D path = turtle.getPath();
        if (fractalType.resize) {
            return Shapes.resize(path, width, height, margin);
        }

        return path;
    }

    private static String rewrite(Type type, int order) {
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

        private final double moveDistance;
        private final Path2D path;

        public Turtle(double x, double y, double moveDistance, int startAngle, int turnAngle) {
            this.x = x;
            this.y = y;
            this.angle = startAngle;
            this.turnAngle = turnAngle;
            this.moveDistance = moveDistance;

            path = new Path2D.Double();

            path.moveTo(x, y);
        }

        public void interpret(String s) {
            for (int i = 0, n = s.length(); i < n; ++i) {
                switch (s.charAt(i)) {
                    case 'F', 'G':
                        moveForward();
                        break;
                    case '+':
                        turnLeft();
                        break;
                    case '-':
                        turnRight();
                        break;
                }
            }
        }

        private void turnLeft() {
            angle += turnAngle;
        }

        private void turnRight() {
            angle -= turnAngle;
        }

        private void moveForward() {
            double angleRadians = Math.toRadians(angle);
            x += moveDistance * FastMath.cos(angleRadians);
            y += moveDistance * FastMath.sin(angleRadians);
            path.lineTo(x, y);
        }

        public Path2D getPath() {
            return path;
        }
    }

    private interface RewriteRule {
        String replace(char c);
    }
}
