package pixelitor.selection.selectionMagicWand;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.PMouseEvent;

import java.awt.event.MouseEvent;

public class DragAdapter extends PMouseEvent {
    private Drag drag;
    private View view;
    private Composition comp;
    public DragAdapter(Drag drag, MouseEvent e){
        super(e, null);
        this.drag = drag;
    }

    public DragAdapter(Drag drag, PMouseEvent e){
        super(e.getOrigEvent(), null);
        this.drag = drag;

        //comp = new Composition(new Canvas(drag.));
    }

}