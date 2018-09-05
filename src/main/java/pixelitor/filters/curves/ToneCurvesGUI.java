package pixelitor.filters.curves;

import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.*;

/**
 * The GUI for the tone curve filter
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesGUI extends FilterGUI {

    public ToneCurvesGUI(Filter filter, Drawable dr) {
        super(filter, dr);

        // listen for any change in curves to run filter preview
        ToneCurvesPanel curvesPanel = new ToneCurvesPanel();
        curvesPanel.addActionListener(e -> {
            ((ToneCurvesFilter) filter).setCurves(curvesPanel.toneCurves);
            runFilterPreview();
        });

        JPanel chartPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chartPanel.add(curvesPanel);

        JComboBox curveTypeSelect = new JComboBox<>(ToneCurveType.values());
        curveTypeSelect.setMaximumRowCount(curveTypeSelect.getItemCount());
        curveTypeSelect.setSelectedItem(ToneCurveType.RGB);
        curveTypeSelect.addActionListener(e -> {
            curvesPanel.setActiveCurve((ToneCurveType) curveTypeSelect.getSelectedItem());
        });

        JButton resetChannel = new JButton("Reset channel");
        resetChannel.addActionListener(e -> curvesPanel.resetActiveCurve());

        JButton resetAllBtn = new JButton("Reset All");
        resetAllBtn.addActionListener(e -> curvesPanel.reset());

        JPanel channelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        channelPanel.add(new JLabel("Channel:"));
        channelPanel.add(curveTypeSelect);
        channelPanel.add(resetChannel);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(e -> dr.setShowOriginal(showOriginalCB.isSelected()));
        buttonsPanel.add(showOriginalCB);
        buttonsPanel.add(resetAllBtn);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.add(channelPanel);
        mainPanel.add(chartPanel);
        mainPanel.add(buttonsPanel);

        add(mainPanel);
    }
}
