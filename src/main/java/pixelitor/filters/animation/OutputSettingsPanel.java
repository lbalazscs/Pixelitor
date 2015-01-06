/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.io.FileChoosers;
import pixelitor.utils.BrowseFilesSupport;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.TFValidationLayerUI;
import pixelitor.utils.TextFieldValidator;
import pixelitor.utils.ValidatedForm;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * The settings for the tweening animation output
 */
public class OutputSettingsPanel extends ValidatedForm implements TextFieldValidator {
    private JTextField nrSecondsTF = new JTextField("2", 3);
    private JTextField fpsTF = new JTextField("24", 3);
    private int nrFrames;
    private final JLabel nrFramesLabel;
    private double fps;
    private final JComboBox<Interpolation> ipCB;
    private final JComboBox<TweenOutputType> outputTypeCB;
    private final JCheckBox pingPongCB;
    private final BrowseFilesSupport browseFilesSupport = new BrowseFilesSupport(FileChoosers.getLastSaveDir().getAbsolutePath());
    private final JTextField fileNameTF;
    private String errorMessage;

    public OutputSettingsPanel() {
        super(new VerticalLayout());
        JPanel contentPanel = new JPanel(new GridBagLayout());

        // A single TFValidationLayerUI for all the textfields.
        LayerUI<JTextField> tfLayerUI = new TFValidationLayerUI(this);

        //noinspection unchecked
        EnumComboBoxModel<TweenOutputType> model = new EnumComboBoxModel(TweenOutputType.class);
        outputTypeCB = new JComboBox<>(model);
        outputTypeCB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputTypeChanged();
            }
        });
        outputTypeChanged(); // initial setup

        GridBagHelper gridBagHelper = new GridBagHelper(contentPanel);
        gridBagHelper.addLabelWithControl("Output Type:", outputTypeCB, 0);

        gridBagHelper.addLabelWithControl("Number of Seconds:",
                new JLayer<>(nrSecondsTF, tfLayerUI), 1);

        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                updateCalculations();
            }
        };
        nrSecondsTF.addKeyListener(keyAdapter);

        gridBagHelper.addLabelWithControl("Frames per Second:",
                new JLayer<>(fpsTF, tfLayerUI), 2);

        fpsTF.addKeyListener(keyAdapter);

        nrFramesLabel = new JLabel();
        updateCalculations();

        gridBagHelper.addLabelWithControl("Number of Frames:", nrFramesLabel, 3);

        EnumComboBoxModel<Interpolation> ipCBM = new EnumComboBoxModel<>(Interpolation.class);
        ipCB = new JComboBox<>(ipCBM);

        gridBagHelper.addLabelWithControl("Interpolation:", ipCB, 4);

        pingPongCB = new JCheckBox();
        gridBagHelper.addLabelWithControl("Ping Pong:", pingPongCB, 5);

        pingPongCB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCalculations();
            }
        });

        JPanel filePanel = new JPanel(new FlowLayout());
        filePanel.setBorder(BorderFactory.createTitledBorder("Output File/Folder"));
        fileNameTF = browseFilesSupport.getNameTF();
        filePanel.add(new JLayer<>(fileNameTF, tfLayerUI));
        filePanel.add(browseFilesSupport.getBrowseButton());
        gridBagHelper.addOnlyControlToRow(filePanel, 6);

        add(contentPanel);
    }

    private void outputTypeChanged() {
        TweenOutputType selected = (TweenOutputType) outputTypeCB.getSelectedItem();
        if(selected.needsDirectory()) {
            browseFilesSupport.setSelectDirs(true);
            browseFilesSupport.setDialogTitle("Select Output Folder");
        } else {
            browseFilesSupport.setSelectDirs(false);
            browseFilesSupport.setDialogTitle("Select Output File");
            browseFilesSupport.setFileFilter(selected.getFileFilter());
        }
        if (fileNameTF != null) { // not the initial setup
            fileNameTF.repaint();
        }
    }

    private void updateCalculations() {
        try {
            double nrSeconds = Double.parseDouble(nrSecondsTF.getText().trim());
            fps = Double.parseDouble(fpsTF.getText().trim());
            nrFrames = (int) (nrSeconds * fps);
            String labelText = String.valueOf(nrFrames);

            if (pingPongCB.isSelected()) {
                int totalFrames = 2 * nrFrames - 2;
                double totalSeconds = totalFrames / fps;
                labelText += String.format(" (with PP: %d frames, %.2f seconds)", totalFrames, totalSeconds);
            }
            nrFramesLabel.setText(labelText);
        } catch (Exception ex) {
            nrFramesLabel.setText("??");
        }
    }

    @Override
    public boolean isDataValid() {
        return isValid(nrSecondsTF) && isValid(fpsTF) && isValid(fileNameTF);
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean isValid(JTextField textField) {
        boolean valid = true;
        if (textField == nrSecondsTF || textField == fpsTF) {
            String text = textField.getText().trim();
            try {
                //noinspection ResultOfMethodCallIgnored
                Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                valid = false;
                errorMessage = text + " is not a valid number.";
            }
        } else if (textField == fileNameTF) {
            TweenOutputType outputType = (TweenOutputType) outputTypeCB.getSelectedItem();
            errorMessage = outputType.checkFile(new File(textField.getText().trim()));
            valid = (errorMessage == null);
        }
        return valid;
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
            FileChoosers.setLastSaveDir(output);
        } else {
            FileChoosers.setLastSaveDir(output.getParentFile());
        }
    }
}
