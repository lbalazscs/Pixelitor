/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.util.Objects.requireNonNull;

/**
 * A message handler implementation for unit tests.
 * Can be used to verify that the correct messages are being displayed
 * and to simulate user responses.
 */
public class TestMessageHandler implements MessageHandler {
    /**
     * Types of messages that can be captured.
     */
    public enum MessageType {
        STATUS, INFO, WARNING, ERROR, EXCEPTION
    }

    public record CapturedMessage(
        MessageType type,
        String title,
        String message,
        Component parent
    ) {
    }

    private final List<CapturedMessage> capturedMessages = new ArrayList<>();
    private final Queue<Boolean> yesNoResponses = new LinkedList<>();
    private boolean throwOnErrors = true;

    public void setThrowOnErrors(boolean throwOnErrors) {
        this.throwOnErrors = throwOnErrors;
    }

    /**
     * Queues a response for the next yes/no question.
     * Responses are used in the order they are queued.
     *
     * @param response true for "yes", false for "no"
     */
    public void queueYesNoResponse(boolean response) {
        yesNoResponses.offer(response);
    }

    public List<CapturedMessage> getCapturedMessages() {
        return List.copyOf(capturedMessages);
    }

    public List<CapturedMessage> getMessagesByType(MessageType type) {
        return capturedMessages.stream()
            .filter(msg -> msg.type() == type)
            .toList();
    }

    public void reset() {
        capturedMessages.clear();
        yesNoResponses.clear();
    }

    @Override
    public void showInStatusBar(String msg) {
        requireNonNull(msg);

        captureMessage(MessageType.STATUS, null, msg, null);
    }

    @Override
    public ProgressHandler startProgress(String msg, int maxValue) {
        captureMessage(MessageType.STATUS, "Progress Started", msg, null);

        return new TestProgressHandler(maxValue);
    }

    @Override
    public void showInfo(String title, String msg, Component parent) {
        requireNonNull(title);
        requireNonNull(msg);

        captureMessage(MessageType.INFO, title, msg, parent);
    }

    @Override
    public void showWarning(String title, String msg, Component parent) {
        requireNonNull(title);
        requireNonNull(msg);

        captureMessage(MessageType.WARNING, title, msg, parent);
        throwIfEnabled("Warning shown: " + title + " - " + msg);
    }

    @Override
    public void showError(String title, String msg, Component parent) {
        requireNonNull(title);
        requireNonNull(msg);

        captureMessage(MessageType.ERROR, title, msg, parent);
        throwIfEnabled("Error shown: " + title + " - " + msg);
    }

    @Override
    public void showException(Throwable exception) {
        requireNonNull(exception);

        captureMessage(MessageType.EXCEPTION, exception.getClass().getSimpleName(), exception.getMessage(), null);
        throwIfEnabled("Exception shown", exception);
    }

    @Override
    public void showException(Throwable exception, Thread srcThread) {
        requireNonNull(exception);
        requireNonNull(srcThread);

        captureMessage(MessageType.EXCEPTION,
            "Exception in " + srcThread.getName(),
            exception.getMessage(), null);

        throwIfEnabled("Exception in " + srcThread.getName() + ": " + exception.getMessage(), exception);
    }

    @Override
    public void showExceptionOnEDT(Throwable exception) {
        // Execute immediately in unit tests rather than scheduling on EDT
        showException(exception);
    }

    @Override
    public boolean showYesNoQuestion(String title, String msg) {
        requireNonNull(title);
        requireNonNull(msg);

        captureMessage(MessageType.INFO, title, msg, null);

        // Return queued response or default to true
        return yesNoResponses.isEmpty() ? true : yesNoResponses.poll();
    }

    private void captureMessage(MessageType messageType, String title, String msg, Component parent) {
        capturedMessages.add(new CapturedMessage(
            messageType, title, msg, parent));
    }

    private void throwIfEnabled(String msg) {
        if (throwOnErrors) {
            throw new AssertionError(msg);
        }
    }

    private void throwIfEnabled(String msg, Throwable cause) {
        if (throwOnErrors) {
            throw new AssertionError(msg, cause);
        }
    }
}
