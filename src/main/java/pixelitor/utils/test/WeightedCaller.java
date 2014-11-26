package pixelitor.utils.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Calls back the given actions with the given weighted probability
 */
public class WeightedCaller {
    private Random random = new Random();
    private List<Runnable> tasks = new ArrayList<>();

    public void registerCallback(int weight, Runnable r) {
        for (int i = 0; i < weight; i++) {
            tasks.add(r);
        }
    }

    public void callRandomAction() {
        int size = tasks.size();
        int index = random.nextInt(size);
        tasks.get(index).run();
    }
}
