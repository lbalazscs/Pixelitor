package pixelitor.selection.selectionMagicWand;

import pixelitor.tools.util.PPoint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

public class SelectionMagicWandDrag implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private double imStartX;
    private double imStartY;
    private double imEndX;
    private double imEndY;


    private transient double coStartX;
    private transient double coEndX;
    private transient double coStartY;
    private transient double coEndY;
    private transient boolean hasCoCoords;

    private transient double prevCoEndX;
    private transient double prevCoEndY;

    private transient boolean dragging;
    private transient boolean canceled;
    private transient boolean startAdjusted;
    private transient boolean constrained;
    private transient boolean startFromCenter;
    private transient boolean equallySized;

    public SelectionMagicWandDrag() {hasCoCoords = false;}

    public SelectionMagicWandDrag(double imStartX, double imStartY, double imEndX, double imEndY) {
        this.imStartX = imStartX;
        this.imStartY = imStartY;
        this.imEndX = imEndX;
        this.imEndY = imEndY;

        hasCoCoords = false;
    }

    public SelectionMagicWandDrag(PPoint start, PPoint end) {
        this.imStartX = start.getImX();
        this.imStartY = start.getImY();
        this.imEndX = end.getImX();
        this.imEndY = end.getImY();

        hasCoCoords = false;
    }

    /*//Hacer aqui el codigo para la forma de la seleccion de la varita magica, solo la FORMA, no al funcionalidad
    private int targetColor;
    private int tolerance;

    public SelectionMagicWandDrag(int targetColor, int tolerance) {
        this.targetColor = targetColor;
        this.tolerance = tolerance;
    }

    // Método principal para seleccionar la región
    public void selectRegion(Point startPoint, BufferedImage image) {
        // Matriz para rastrear los píxeles visitados
        boolean[][] visited = new boolean[image.getWidth()][image.getHeight()];

        // Llama a la función auxiliar para realizar la selección
        selectRegionHelper(startPoint.x, startPoint.y, image, visited);
    }

    // Función auxiliar recursiva para seleccionar la región conectada
    private void selectRegionHelper(int x, int y, BufferedImage image, boolean[][] visited) {
        // Verifica los límites de la imagen y si el píxel ya ha sido visitado
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight() || visited[x][y]) {
            return;
        }

        // Marca el píxel como visitado
        visited[x][y] = true;

        // Obtiene el color del píxel actual
        int pixelColor = image.getRGB(x, y);

        // Verifica si el color es similar al color objetivo
        if (isColorSimilar(pixelColor, targetColor, tolerance)) {
            // Realiza acciones necesarias con el píxel seleccionado,
            // por ejemplo, cambiar el color del píxel, almacenar la posición, etc.
            image.setRGB(x, y, Color.YELLOW.getRGB());  // Cambia el color a amarillo como ejemplo

            // Llamada recursiva para píxeles vecinos
            selectRegionHelper(x - 1, y, image, visited);
            selectRegionHelper(x + 1, y, image, visited);
            selectRegionHelper(x, y - 1, image, visited);
            selectRegionHelper(x, y + 1, image, visited);
        }
    }

    // Método para verificar si dos colores son similares
    private boolean isColorSimilar(int color1, int color2, int tolerance) {
        // Descomponer los componentes de color
        int alpha1 = (color1 >> 24) & 0xFF;
        int red1 = (color1 >> 16) & 0xFF;
        int green1 = (color1 >> 8) & 0xFF;
        int blue1 = color1 & 0xFF;

        int alpha2 = (color2 >> 24) & 0xFF;
        int red2 = (color2 >> 16) & 0xFF;
        int green2 = (color2 >> 8) & 0xFF;
        int blue2 = color2 & 0xFF;

        // Calcular la diferencia total entre los componentes de color
        int colorDifference = Math.abs(red1 - red2) + Math.abs(green1 - green2) + Math.abs(blue1 - blue2);

        // Verificar si la diferencia está dentro de la tolerancia
        return alpha1 > 0 && alpha2 > 0 && colorDifference <= tolerance;
    }

    public double getImStartX() {

        return imStartX;
    }*/
}
