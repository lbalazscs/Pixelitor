/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
package pixelitor.gui.utils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 *
 */
public class CardPanelWithCombo extends JPanel implements ItemListener {
    private final DefaultComboBoxModel<String> selectorModel;
    private final JPanel cardPanel;

    public CardPanelWithCombo() {
        super(new BorderLayout());

        selectorModel = new DefaultComboBoxModel<>();
        JComboBox<String> selector = new JComboBox<>(selectorModel);
        selector.addItemListener(this);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new FlowLayout());
        northPanel.add(selector);

        add(northPanel, BorderLayout.NORTH);

        cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout());
        add(cardPanel, BorderLayout.CENTER);
    }

    public void addNewCard(Card card) {
        String channelName = card.getCardName();
        cardPanel.add(card, channelName);
        selectorModel.addElement(channelName);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        CardLayout cl = (CardLayout) (cardPanel.getLayout());
        cl.show(cardPanel, (String) e.getItem());
    }

    public static class Card extends JPanel {
        private final String cardName;

        public Card(String cardName) {
            this.cardName = cardName;
        }

        public String getCardName() {
            return cardName;
        }
    }
}
