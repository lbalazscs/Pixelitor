package pixelitor.gui.utils;

import pixelitor.compactions.EnlargeCanvas;
import pixelitor.filters.gui.RangeParam;

import javax.swing.*;
import java.awt.*;

class UITEsts {

    JFrame frame;

    public UITEsts() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());
    }

    public void end() {
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {

        new UITEsts()
                .testEnlargeCanvasPanel()
//                .testSliderSpinner()
                .end();

    }

    UITEsts testEnlargeCanvasPanel() {

//        frame.add(
//                new EnlargeCanvas.EnlargeCanvasPanel()
//        );

        return this;
    }

    UITEsts testSliderSpinner() {
        frame.add(
                new SliderSpinner(
                        new RangeParam("North", 0, 0, 500),
                        SliderSpinner.TextPosition.BORDER,
                        false,
                        SliderSpinner.VERTICAL
                )
        );

        frame.add(
                new SliderSpinner(
                        new RangeParam("North", 0, 0, 500),
                        SliderSpinner.TextPosition.NORTH,
                        false,
                        SliderSpinner.VERTICAL
                )
        );

        frame.add(new JSlider(JSlider.VERTICAL));

        return this;
    }

}