/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.animation;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.gui.utils.*;
import pixelitor.io.RecentDirs;

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.io.File;
import java.text.ParseException;

import static java.lang.String.format;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.DIRECTORY;
import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.FILE;
import static pixelitor.gui.utils.TFValidationLayerUI.wrapWithValidation;
import static pixelitor.gui.utils.TextFieldValidator.requirePositiveDouble;
import static pixelitor.utils.Utils.parseLocalizedDouble;

/**
 * A panel for configuring tweening animation output settings.
 */
public class TweenOutputSettingsPanel extends JPanel implements Validated {
    private final JTextField numSecondsTF = new JTextField("2", 5);
    private final JTextField fpsTF = new JTextField("24", 5);
    private int numFrames;
    private final JLabel numFramesLabel = new JLabel();
    private double fps;
    private JComboBox<TimeInterpolation> ipCB;
    private JComboBox<TweenOutputType> outputTypeCB;
    private final JCheckBox pingPongCB = new JCheckBox();
    private final BrowseFilesSupport browseSupport = new BrowseFilesSupport(
        RecentDirs.getLastSavePath());
    private JTextField fileNameTF;

    private final TextFieldValidator numSecondsValidator = textField ->
        requirePositiveDouble(textField, "Number of Seconds");
    private final TextFieldValidator fpsValidator = textField ->
        requirePositiveDouble(textField, "Frames per Second");
    private final TextFieldValidator fileNameValidator = textField ->
        getOutputType().validate(new File(textField.getText().trim()));

    public TweenOutputSettingsPanel() {
        super(new GridBagLayout());

        numSecondsTF.setName("numSecondsTF");
        fpsTF.setName("fpsTF");
        numFramesLabel.setName("numFramesLabel");

        var gbh = new GridBagHelper(this);
        addOutputTypeSelector(gbh);
        addAnimationLengthSelectors(gbh);
        addInterpolationSelector(gbh);
        addPingPongSelector(gbh);
        addFileSelector(gbh);
    }

    @SuppressWarnings("unchecked")
    private void addOutputTypeSelector(GridBagHelper gbh) {
        var model = new EnumComboBoxModel<>(TweenOutputType.class);
        outputTypeCB = new JComboBox<>(model);
        outputTypeCB.addActionListener(e -> outputTypeChanged());
        outputTypeChanged(); // initial setup

        gbh.addLabelAndControlNoStretch("Output Type:", outputTypeCB);
    }

    private void addAnimationLengthSelectors(GridBagHelper gbh) {
        gbh.addLabelAndControlNoStretch("Number of Seconds:",
            wrapWithValidation(numSecondsTF, numSecondsValidator));

        var docListener = new SimpleDocumentListener(e -> updateCalculations());
        numSecondsTF.getDocument().addDocumentListener(docListener);

        gbh.addLabelAndControlNoStretch("Frames per Second:",
            wrapWithValidation(fpsTF, fpsValidator));
        fpsTF.getDocument().addDocumentListener(docListener);

        updateCalculations();
        gbh.addLabelAndControl("Number of Frames:", numFramesLabel);
    }

    @SuppressWarnings("unchecked")
    private void addInterpolationSelector(GridBagHelper gbh) {
        var ipCBM = new EnumComboBoxModel<>(TimeInterpolation.class);
        ipCB = new JComboBox<>(ipCBM);

        gbh.addLabelAndControlNoStretch("Interpolation:", ipCB);
    }

    private void addPingPongSelector(GridBagHelper gbh) {
        gbh.addLabelAndControl("Ping Pong:", pingPongCB);
        pingPongCB.addActionListener(e -> updateCalculations());
    }

    private void addFileSelector(GridBagHelper gbh) {
        JPanel filePanel = new JPanel(new FlowLayout());
        filePanel.setBorder(createTitledBorder("Output File/Folder"));
        fileNameTF = browseSupport.getTextField();
        filePanel.add(wrapWithValidation(fileNameTF, fileNameValidator));
        filePanel.add(browseSupport.getBrowseButton());
        gbh.addFullRow(filePanel);
    }

    private void outputTypeChanged() {
        TweenOutputType outputType = getOutputType();
        if (outputType.needsDirectory()) {
            browseSupport.setSelectionMode(DIRECTORY);
            browseSupport.setChooserDialogTitle("Select Output Folder");
        } else {
            browseSupport.setSelectionMode(FILE);
            browseSupport.setChooserDialogTitle("Select Output File");
            browseSupport.setFileFilter(outputType.getFileFilter());
        }
        if (fileNameTF != null) { // not the initial setup
            fileNameTF.repaint();
        }
    }

    private void updateCalculations() {
        try {
            double numSeconds = parseLocalizedDouble(numSecondsTF.getText().trim());
            double currentFps = parseLocalizedDouble(fpsTF.getText().trim());

            // update instance fields only on successful parsing
            this.fps = currentFps;
            this.numFrames = (int) (numSeconds * currentFps);

            numFramesLabel.setText(createNumFramesText());
        } catch (ParseException e) {
            // expected when the user is typing in the text fields
            numFramesLabel.setText("??");
        }
    }

    private String createNumFramesText() {
        String labelText = String.valueOf(numFrames);

        if (pingPongCB.isSelected() && numFrames > 1) {
            // for ping-pong, we have forward and reverse frames, minus two duplicate end frames
            int totalFrames = 2 * numFrames - 2;
            double totalSeconds = totalFrames / fps;
            labelText += format(" (ping-pong: %d frames, %.2f s)", totalFrames, totalSeconds);
        }
        return labelText;
    }

    @Override
    public ValidationResult validateSettings() {
        return numSecondsValidator.check(numSecondsTF)
            .and(fpsValidator.check(fpsTF))
            .and(fileNameValidator.check(fileNameTF));
    }

    public void configure(TweenAnimation animation) {
        animation.setOutputType(getOutputType());
        animation.setNumFrames(numFrames);
        animation.setMillisBetweenFrames((int) (1000.0 / fps));
        animation.setInterpolation((TimeInterpolation) ipCB.getSelectedItem());
        animation.setPingPong(pingPongCB.isSelected());

        File outputLocation = browseSupport.getSelectedFile();
        animation.setOutputLocation(outputLocation);

        if (outputLocation.isDirectory()) {
            RecentDirs.setLastSave(outputLocation);
        } else {
            RecentDirs.setLastSave(outputLocation.getParentFile());
        }
    }

    private TweenOutputType getOutputType() {
        return (TweenOutputType) outputTypeCB.getSelectedItem();
    }
}
