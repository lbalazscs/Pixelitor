package pixelitor.selection.selectionMagicWand;

import pixelitor.selection.SelectionType;

import pixelitor.tools.util.Drag;


import java.awt.*;
import java.awt.geom.GeneralPath;

public enum SelectionMagicWandShape {

    SELECTION_MAGIC_WAND("MagicWand", true){
        public Shape createMagicWandShape(Object mouseInfo, Shape oldShape) {
            Drag drag = (Drag) mouseInfo;

            if (createNewMagicWandShape(oldShape)) {
                GeneralPath p = new GeneralPath();
                p.moveTo(drag.getStartX(), drag.getStartY());
                p.lineTo(drag.getEndX(), drag.getEndY());
                return p;
            } else {
                GeneralPath gp = (GeneralPath) oldShape;
                gp.lineTo(drag.getEndX(), drag.getEndY());

                return gp;
            }
        }
    };

    private final String guiName;

    private final boolean displayWH;

    SelectionMagicWandShape(String guiName, boolean displayWH) {
        this.guiName = guiName;
        this.displayWH = displayWH;
    }

    public abstract Shape createMagicWandShape(Object mouseInfo, Shape oldShape);

    public boolean displayWidthHeight() { return displayWH; }

    private static boolean createNewMagicWandShape(Shape oldShape) {
        boolean createNew;
        if (oldShape == null) {
            createNew = true;
        } else if (oldShape instanceof GeneralPath) {
            createNew = false;
        } else { // it is an Area, meaning that a new shape has been started
            createNew = true;
        }
        return createNew;
    }

    @Override
    public String toString() { return guiName; }

    /*private Canvas mCanvas;
    private SelectionMagicWand mMagicWand;
    private BufferedImage mSelection; // Podría ser un BufferedImage en Java
    private java.awt.Shape mCachedPath; // Podría ser un GeneralPath en Java
    private double mPhase;

    public SelectionMagicWandShape() {
        // Inicialización de la varita mágica
        mMagicWand = new SelectionMagicWand();

        // Los siguientes miembros se utilizan solo cuando hay una selección
        mSelection = null;
        mCachedPath = null;
        mPhase = 0.0;
    }

    void mouseDown(MouseEvent eventoMouse) {
        // Simplemente pasa el evento del ratón a la varita mágica.
        // También le proporciona el lienzo en el que trabajar y una referencia a nosotros mismos,
        // para que pueda traducir las ubicaciones del ratón.

        // Parámetros:
        // - eventoMouse: el evento del ratón que desencadenó este método

        // Variables locales:
        // - mMagicWand: instancia de la varita mágica
        // - mCanvas: instancia del lienzo

        // Llamada al método mouseDown de la varita mágica
        mMagicWand.mouseDown(eventoMouse, this, mCanvas);
    }*/

}
