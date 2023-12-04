package pixelitor.selection.selectionMagicWand;

import pixelitor.Composition;
import pixelitor.selection.Selection;
import pixelitor.selection.ShapeCombinator;

public class SelectionMagicWandBuilder {
    private final ShapeCombinator combinator;

    public SelectionMagicWandBuilder(ShapeCombinator combinator, Composition comp) {
        this.combinator = combinator;
        Selection existingSelection = comp.getSelection();

        if (existingSelection == null) {
            return;
        }

        assert existingSelection.isAlive() : "dead selection";
    }
}
