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

package pixelitor.filters.animation;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.gui.utils.*;
import pixelitor.io.Dirs;
import pixelitor.utils.Messages;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.text.ParseException;

import static java.lang.String.format;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.DIRECTORY;
import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.FILE;
import static pixelitor.utils.Utils.parseDouble;

/**
 * The settings for the tweening animation output
 */
public class TweenOutputSettingsPanel extends ValidatedPanel
    implements TextFieldValidator {

    private final JTextField numSecondsTF = new JTextField("2", 5);
    private final JTextField fpsTF = new JTextField("24", 5);
    private int nrFrames;
    private final JLabel numFramesLabel = new JLabel();
    private double fps;
    private JComboBox<TimeInterpolation> ipCB;
    private JComboBox<TweenOutputType> outputTypeCB;
    private final JCheckBox pingPongCB = new JCheckBox();
    private final BrowseFilesSupport browseFilesSupport = new BrowseFilesSupport(
        Dirs.getLastSave().getAbsolutePath());
    private JTextField fileNameTF;

    public TweenOutputSettingsPanel() {
        super(new GridBagLayout());

        numSecondsTF.setName("numSecondsTF");
        fpsTF.setName("fpsTF");
        numFramesLabel.setName("numFramesLabel");

        // A single TFValidationLayerUI for all the textfields.
        LayerUI<JTextField> tfLayerUI = TFValidationLayerUI.fromValidator(this);

        var gbh = new GridBagHelper(this);

        addOutputTypeSelector(gbh);
        addAnimationLengthSelectors(tfLayerUI, gbh);
        addInterpolationSelector(gbh);
        addPingPongSelector(gbh);
        addFileSelector(tfLayerUI, gbh);
    }

    @SuppressWarnings("unchecked")
    private void addOutputTypeSelector(GridBagHelper gbh) {
        var model = new EnumComboBoxModel<>(TweenOutputType.class);

        outputTypeCB = new JComboBox<>(model);
        outputTypeCB.addActionListener(e -> outputTypeChanged());
        outputTypeChanged(); // initial setup

        gbh.addLabelAndControlNoStretch("Output Type:", outputTypeCB);
    }

    private void addAnimationLengthSelectors(LayerUI<JTextField> tfLayerUI,
                                             GridBagHelper gbh) {
        gbh.addLabelAndControlNoStretch("Number of Seconds:",
            new JLayer<>(numSecondsTF, tfLayerUI));

        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                updateCalculations();
            }
        };
        numSecondsTF.addKeyListener(keyListener);

        gbh.addLabelAndControlNoStretch("Frames per Second:",
            new JLayer<>(fpsTF, tfLayerUI));
        fpsTF.addKeyListener(keyListener);

        updateCalculations();
        gbh.addLabelAndControl("Number of Frames:", numFramesLabel);
    }

    @SuppressWarnings("unchecked")
    private void addInterpolationSelector(GridBagHelper gbh) {
        EnumComboBoxModel<TimeInterpolation> ipCBM
            = new EnumComboBoxModel<>(TimeInterpolation.class);
        ipCB = new JComboBox<>(ipCBM);

        gbh.addLabelAndControlNoStretch("Interpolation:", ipCB);
    }

    private void addPingPongSelector(GridBagHelper gbh) {
        gbh.addLabelAndControl("Ping Pong:", pingPongCB);

        pingPongCB.addActionListener(e -> updateCalculations());
    }

    private void addFileSelector(LayerUI<JTextField> tfLayerUI, GridBagHelper gbh) {
        JPanel filePanel = new JPanel(new FlowLayout());
        filePanel.setBorder(createTitledBorder("Output File/Folder"));
        fileNameTF = browseFilesSupport.getNameTF();
        filePanel.add(new JLayer<>(fileNameTF, tfLayerUI));
        filePanel.add(browseFilesSupport.getBrowseButton());
        gbh.addOnlyControl(filePanel);
    }

    private void outputTypeChanged() {
        TweenOutputType selected = getOutputType();
        if (selected.needsDirectory()) {
            browseFilesSupport.setSelectionMode(DIRECTORY);
            browseFilesSupport.setChooserDialogTitle("Select Output Folder");
        } else {
            browseFilesSupport.setSelectionMode(FILE);
            browseFilesSupport.setChooserDialogTitle("Select Output File");
            browseFilesSupport.setFileFilter(selected.getFileFilter());
        }
        if (fileNameTF != null) { // not the initial setup
            fileNameTF.repaint();
        }
    }

    private void updateCalculations() {
        try {
            numFramesLabel.setText(calculateNrFramesText());
        } catch (ParseException e) {
            // expected behaviour, we can swallow the exception
            numFramesLabel.setText("??");
        } catch (Exception e) {
            Messages.showException(e);
            numFramesLabel.setText("??");
        }
    }

    private String calculateNrFramesText() throws ParseException {
        double nrSeconds = parseDouble(numSecondsTF.getText().trim());
        fps = parseDouble(fpsTF.getText().trim());
        nrFrames = (int) (nrSeconds * fps);
        String labelText = String.valueOf(nrFrames);

        if (pingPongCB.isSelected()) {
            int totalFrames = 2 * nrFrames - 2;
            double totalSeconds = totalFrames / fps;
            labelText += format(" (with PP: %d frames, %.2f seconds)",
                totalFrames, totalSeconds);
        }
        return labelText;
    }

    @Override
    public ValidationResult validateSettings() {
        return check(numSecondsTF)
            .and(check(fpsTF))
            .and(check(fileNameTF));
    }

    @Override
    public ValidationResult check(JTextField textField) {
        if (textField == numSecondsTF) {
            return TextFieldValidator.hasPositiveDouble(textField, "Number of Seconds");
        } else if (textField == fpsTF) {
            return TextFieldValidator.hasPositiveDouble(textField, "Frames per Second");
        } else if (textField == fileNameTF) {
            TweenOutputType outputType = getOutputType();

            String fileName = textField.getText().trim();
            String errorMessage = outputType.validate(new File(fileName));
            if (errorMessage == null) {
                return ValidationResult.ok();
            } else {
                return ValidationResult.error(errorMessage);
            }
        } else {
            throw new IllegalStateException("unexpected JTextField");
        }
    }

    public void configure(TweenAnimation animation) {
        animation.setOutputType(getOutputType());

        File output = browseFilesSupport.getSelectedFile();
        animation.setOutput(output);

        animation.setNumFrames(nrFrames);
        animation.setMillisBetweenFrames((int) (1000.0 / fps));
        animation.setInterpolation((TimeInterpolation) ipCB.getSelectedItem());
        animation.setPingPong(pingPongCB.isSelected());

        if (output.isDirectory()) {
            Dirs.setLastSave(output);
        } else {
            Dirs.setLastSave(output.getParentFile());
        }
    }

    private TweenOutputType getOutputType() {
        return (TweenOutputType) outputTypeCB.getSelectedItem();
    }
}
