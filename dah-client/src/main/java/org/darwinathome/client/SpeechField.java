// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.network.Spoke;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * The field for the sign above the body
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class SpeechField {
    private JTextField field;
    private Spoke spoke;
    private String text;

    public SpeechField(Spoke spoke) {
        this.spoke = spoke;
        field = new JTextField();
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                checkValue();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                checkValue();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                checkValue();
            }
        });
        field.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                commit();
            }
        });
        field.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                checkValue();
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                commit();
            }
        });
    }

    public JPanel getPanel() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEtchedBorder());
        p.add(new JLabel("Tetragotchi say:", JLabel.RIGHT), BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    public void refresh() {
        field.setText(this.text = spoke.getSpeech().getText());
    }

    private void checkValue() {
        String fieldText = field.getText();
        if (text.equals(fieldText)) {
            field.setBackground(Color.WHITE);
            field.select(0, fieldText.length());
        }
        else {
            field.setBackground(Color.YELLOW);
        }
    }

    private void commit() {
        if (!text.equals(field.getText())) {
            spoke.setSpeech(text = field.getText());
        }
        checkValue();
    }

}
