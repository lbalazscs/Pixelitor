/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.BrowseFilesSupport;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.TFValidationLayerUI;
import pixelitor.gui.utils.TextFieldValidator;
import pixelitor.gui.utils.ValidatedForm;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.io.Directories;
import pixelitor.utils.Messages;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.DIRECTORY;
import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.FILE;

/**
 * The settings for the tweening animation output
 */
public class OutputSettingsPanel extends ValidatedForm implements TextFieldValidator {
    private final JTextField numSecondsTF = new JTextField("2", 3);
    private final JTextField fpsTF = new JTextField("24", 3);
    private int nrFrames;
    private final JLabel numFramesLabel = new JLabel();
    private double fps;
    private JComboBox<Interpolation> ipCB;
    private JComboBox<TweenOutputType> outputTypeCB;
    private final JCheckBox pingPongCB = new JCheckBox();
    private final BrowseFilesSupport browseFilesSupport = new BrowseFilesSupport(Directories.getLastSaveDir().getAbsolutePath());
    private JTextField fileNameTF;

    public OutputSettingsPanel() {
        super(new GridBagLayout());

        numSecondsTF.setName("numSecondsTF");
        fpsTF.setName("fpsTF");
        numFramesLabel.setName("numFramesLabel");

        // A single TFValidationLayerUI for all the textfields.
        LayerUI<JTextField> tfLayerUI = new TFValidationLayerUI(this);

        GridBagHelper gbh = new GridBagHelper(this);

        addOutputTypeSelector(gbh);
        addAnimationLengthSelector(tfLayerUI, gbh);
        addInterpolationSelector(gbh);
        addPingPongSelector(gbh);
        addFileSelector(tfLayerUI, gbh);
    }

    private void addOutputTypeSelector(GridBagHelper gbh) {
        //noinspection unchecked
        EnumComboBoxModel<TweenOutputType> model = new EnumComboBoxModel(TweenOutputType.class);
        outputTypeCB = new JComboBox<>(model);
        outputTypeCB.addActionListener(e -> outputTypeChanged());
        outputTypeChanged(); // initial setup

        gbh.addLabelWithControl("Output Type:", outputTypeCB);
    }

    private void addAnimationLengthSelector(LayerUI<JTextField> tfLayerUI, GridBagHelper gbh) {
        gbh.addLabelWithControl("Number of Seconds:",
                new JLayer<>(numSecondsTF, tfLayerUI));

        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                updateCalculations();
            }
        };
        numSecondsTF.addKeyListener(keyListener);

        gbh.addLabelWithControl("Frames per Second:",
                new JLayer<>(fpsTF, tfLayerUI));
        fpsTF.addKeyListener(keyListener);

        updateCalculations();
        gbh.addLabelWithControl("Number of Frames:", numFramesLabel);
    }

    private void addInterpolationSelector(GridBagHelper gbh) {
        EnumComboBoxModel<Interpolation> ipCBM = new EnumComboBoxModel<>(Interpolation.class);
        ipCB = new JComboBox<>(ipCBM);

        gbh.addLabelWithControl("Interpolation:", ipCB);
    }

    private void addPingPongSelector(GridBagHelper gbh) {
        gbh.addLabelWithControl("Ping Pong:", pingPongCB);

        pingPongCB.addActionListener(e -> updateCalculations());
    }

    private void addFileSelector(LayerUI<JTextField> tfLayerUI, GridBagHelper gbh) {
        JPanel filePanel = new JPanel(new FlowLayout());
        filePanel.setBorder(BorderFactory.createTitledBorder("Output File/Folder"));
        fileNameTF = browseFilesSupport.getNameTF();
        filePanel.add(new JLayer<>(fileNameTF, tfLayerUI));
        filePanel.add(browseFilesSupport.getBrowseButton());
        gbh.addOnlyControlToRow(filePanel, 6);
    }

    private void outputTypeChanged() {
        TweenOutputType selected = (TweenOutputType) outputTypeCB.getSelectedItem();
        if(selected.needsDirectory()) {
            browseFilesSupport.setSelectionMode(DIRECTORY);
            browseFilesSupport.setFileChooserTitle("Select Output Folder");
        } else {
            browseFilesSupport.setSelectionMode(FILE);
            browseFilesSupport.setFileChooserTitle("Select Output File");
            browseFilesSupport.setFileFilter(selected.getFileFilter());
        }
        if (fileNameTF != null) { // not the initial setup
            fileNameTF.repaint();
        }
    }

    private void updateCalculations() {
        try {
            numFramesLabel.setText(calculateNrFramesText());
        } catch (NumberFormatException e) {
            // expected behaviour, we can swallow the exception
            numFramesLabel.setText("??");
        } catch (Exception e) {
            Messages.showException(e);
            numFramesLabel.setText("??");
        }
    }

    private String calculateNrFramesText() {
        double nrSeconds = Double.parseDouble(numSecondsTF.getText().trim());
        fps = Double.parseDouble(fpsTF.getText().trim());
        nrFrames = (int) (nrSeconds * fps);
        String labelText = String.valueOf(nrFrames);

        if (pingPongCB.isSelected()) {
            int totalFrames = 2 * nrFrames - 2;
            double totalSeconds = totalFrames / fps;
            labelText += String.format(" (with PP: %d frames, %.2f seconds)", totalFrames, totalSeconds);
        }
        return labelText;
    }

    @Override
    public ValidationResult checkValidity() {
        return check(numSecondsTF)
                .and(check(fpsTF))
                .and(check(fileNameTF));
    }

    @Override
    public ValidationResult check(JTextField textField) {
        if (textField == numSecondsTF || textField == fpsTF) {
            return isTextFieldWithDoubleValid(textField);
        } else if (textField == fileNameTF) {
            TweenOutputType outputType = (TweenOutputType) outputTypeCB.getSelectedItem();

            String fileName = textField.getText().trim();
            String errorMessage = outputType.isOK(new File(fileName));
            if (errorMessage == null) {
                return ValidationResult.ok();
            } else {
                return ValidationResult.error(errorMessage);
            }
        } else {
            throw new IllegalStateException("unexpected JTextField");
        }
    }

    private static ValidationResult isTextFieldWithDoubleValid(JTextField textField) {
        String text = textField.getText().trim();
        try {
            //noinspection ResultOfMethodCallIgnored
            Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return ValidationResult.error(text + " is not a valid number.");
        }
        return ValidationResult.ok();
    }

    public void copySettingsInto(TweenAnimation animation) {
        TweenOutputType type = (TweenOutputType) outputTypeCB.getSelectedItem();
        animation.setOutputType(type);

        File output = browseFilesSupport.getSelectedFile();
        animation.setOutput(output);

        animation.setNumFrames(nrFrames);
        animation.setMillisBetweenFrames((int) (1000.0 / fps));
        animation.setInterpolation((Interpolation) ipCB.getSelectedItem());
        animation.setPingPong(pingPongCB.isSelected());

        if (output.isDirectory()) {
            Directories.setLastSaveDir(output);
        } else {
            Directories.setLastSaveDir(output.getParentFile());
        }
    }
}
