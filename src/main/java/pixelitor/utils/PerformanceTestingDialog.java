/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.utils;

import pixelitor.Build;
import pixelitor.ChangeReason;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.gui.FilterWithGUI;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class PerformanceTestingDialog extends JDialog implements ActionListener, PropertyChangeListener {
    private final JComboBox<Filter> opSelector;
    private final JProgressBar progressBar;
    private final JButton startButton;
    private final JButton stopButton;
    private final JButton closeButton;
    private TestingTask task;
    private final JTextField timesField;

    public PerformanceTestingDialog(Frame parent) {
        super(parent, "Performance testing");

        setLayout(new BorderLayout());
        JPanel northPanel = new JPanel();
        JPanel centralPanel = new JPanel();
        JPanel southPanel = new JPanel();

        northPanel.setLayout(new FlowLayout());

        opSelector = new JComboBox<>(FilterUtils.getAllFiltersSorted());
        opSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Filter op = (Filter) opSelector.getSelectedItem();
                if (op instanceof FilterWithGUI) {
                    FilterWithGUI dialogOp = (FilterWithGUI) op;
                    dialogOp.actionPerformed(null);
                }
            }
        });
        northPanel.add(new JLabel("Select op: "));
        northPanel.add(opSelector);

        centralPanel.setLayout(new BorderLayout());

        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("How many times: "));
        timesField = new IntTextField("100");
        timesField.setColumns(7);
        p.add(timesField);
        centralPanel.add(p, BorderLayout.NORTH);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        centralPanel.add(progressBar, BorderLayout.SOUTH);

        startButton = new JButton("start");
        startButton.addActionListener(this);
        southPanel.add(startButton);

        stopButton = new JButton("stop");
        stopButton.addActionListener(this);
        stopButton.setEnabled(false);
        southPanel.add(stopButton);

        closeButton = new JButton("close");
        closeButton.addActionListener(this);
        southPanel.add(closeButton);

        add(northPanel, BorderLayout.NORTH);
        add(centralPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
        pack();
        GUIUtils.centerOnScreen(this);
        setVisible(true);
    }

    private void setEnabledWhileRunning() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        closeButton.setEnabled(false);

        opSelector.setEnabled(false);
        timesField.setEnabled(false);
    }

    private void setEnabledAfterRunning() {
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        closeButton.setEnabled(true);

        opSelector.setEnabled(true);
        timesField.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            int executions = 0;
            try {
                executions = Integer.parseInt(timesField.getText().trim());
            } catch (NumberFormatException e1) {
                JOptionPane.showMessageDialog(this, '\"' + timesField.getText() + "\" is not an integer number.", "Error", JOptionPane.ERROR_MESSAGE);
            }

            if (executions > 0) {
                setEnabledWhileRunning();
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                task = new TestingTask(executions);
                task.addPropertyChangeListener(this);
                task.execute();
            }
        } else if (e.getSource() == stopButton) {
            if (task != null) {
                task.cancel(false);
            }
            setEnabledAfterRunning();
        } else if (e.getSource() == closeButton) {
            dispose();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        }
    }

    class TestingTask extends SwingWorker<Void, Void> {
        private int executions = 0;
        private Filter op;
        private long totalTime;

        private TestingTask(int testExecutions) {
            this.executions = testExecutions;
        }

        /*
        * Main testingTask. Executed in background thread.
        */
        @Override
        public Void doInBackground() {
            Build.CURRENT.setPerformanceTest(true);
            //Initialize progress property.
            int progress = 0;

            setProgress(progress);

            op = (Filter) opSelector.getSelectedItem();
            long startTime = System.nanoTime();

            long shortestTime = Long.MAX_VALUE;

            for (int i = 0; i < executions; i++) {
                long individualStartTime = System.nanoTime();

                op.execute(ChangeReason.PERFORMANCE_TEST);

                long individualTotalTime = (System.nanoTime() - individualStartTime) / 1000000;
                if (individualTotalTime < shortestTime) {
                    shortestTime = individualTotalTime;
                }

                progress = 100 * (i + 1) / executions;
                setProgress(progress);

                if (isCancelled()) {
                    totalTime = (System.nanoTime() - startTime) / 1000000;
                    String results = getReport(op.getMenuName(), (i + 1), totalTime, shortestTime);
                    showResults(results);

                    Build.CURRENT.setPerformanceTest(false);

                    return null;
                }
            }

            totalTime = (System.nanoTime() - startTime) / 1000000;
            String results = getReport(op.getMenuName(), executions, totalTime, shortestTime);
            showResults(results);

            Build.CURRENT.setPerformanceTest(false);

            return null;
        }

        private void showResults(final String results) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(PerformanceTestingDialog.this, results, "Performance Testing Results", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }

        private String getReport(String opName, int executions, long totalTime, long shortestTime) {
            return ("Executing \"" + opName
                    + "\" " + executions + " times took " + totalTime
                    + " ms, average time = " + totalTime / executions + " ms, shortest time = " + shortestTime + "ms.");
        }

        /*
        * Executed in event dispatching thread
        */
        @Override
        public void done() {
//            Toolkit.getDefaultToolkit().beep();
            setCursor(null); //turn off the wait cursor

            setEnabledAfterRunning();
        }
    }

    public static void main(String[] args) {
        GUIUtils.testJDialog(new PerformanceTestingDialog(null));
    }

}
