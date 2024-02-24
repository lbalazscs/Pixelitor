package pixelitor.filters.truchets;

import java.util.Random;

public class TruchetProceduralPattern extends TruchetConfigurablePattern {


    public void setState(ProceduralStateSpace selectedItem) {
        Random r = new Random();
        var space = selectedItem.createState(getColumns(), getRows(), 12, r);
        for (int i = 0; i < getRows(); i++) {
            for (int j = 0; j < getColumns(); j++) {
                setState(i, j, selectedItem.getState(getColumns(), getRows(), j, i, 12, r, space));
            }
        }
    }
}