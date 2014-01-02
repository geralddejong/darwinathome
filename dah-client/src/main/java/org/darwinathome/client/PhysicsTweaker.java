// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.log4j.Logger;
import org.darwinathome.geometry.structure.Physics;
import org.darwinathome.geometry.structure.PhysicsValue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Control the physics parameters, persisting them
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class PhysicsTweaker {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.000000");
    private Logger log = Logger.getLogger(getClass());
    private JPanel panel = new JPanel(new SpringLayout());
    private List<PhysicsValue> valueList;

    public PhysicsTweaker(List<PhysicsValue> valueList) {
        this.valueList = valueList;
    }

    public JPanel getPanel() {
        for (PhysicsValue value : valueList) {
            addField(value);
        }
        makeCompactGrid(panel, panel.getComponentCount()/2, 2, 5, 5, 5, 5);
        return panel;
    }

    private void addField(final Physics.Value value) {
        final JTextField field = new JTextField();
        field.setText(FORMAT.format(value.get()));
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent documentEvent) {
                checkValue(field, value);
            }

            public void removeUpdate(DocumentEvent documentEvent) {
                checkValue(field, value);
            }

            public void changedUpdate(DocumentEvent documentEvent) {
                checkValue(field, value);
            }
        });
        field.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setValue(field, value);
            }
        });
        JLabel label = new JLabel(value.getName(), JLabel.RIGHT);
        panel.add(label);
        panel.add(field);
    }

    private void checkValue(JTextField field, Physics.Value value) {
        String valueString = FORMAT.format(value.get());
        String fieldString = field.getText();
        if (valueString.equals(fieldString)) {
            field.setBackground(Color.WHITE);
        }
        else {
            field.setBackground(Color.YELLOW);
        }
    }

    private void setValue(JTextField field, Physics.Value value) {
        try {
            double fieldValue = Double.parseDouble(field.getText());
            value.set(fieldValue);
            field.setText(FORMAT.format(fieldValue));
            logStatements();

        }
        catch (NumberFormatException e) {
            field.setText(FORMAT.format(value.get()));
        }
        checkValue(field, value);
    }

    private void logStatements() {
        StringBuilder statements = new StringBuilder();
        for (PhysicsValue value : valueList) {
            statements.append("    private PhysicsValue ").append(value.getName())
                    .append(" = new PhysicsValue(")
                    .append('"').append(value.getName()).append('"').append(", ")
                    .append(FORMAT.format(value.get()))
                    .append(");\n");
        }
        log.info("Insert this\n\n"+statements);
    }

    /* Used by makeCompactGrid. */
    private static SpringLayout.Constraints getConstraintsForCell(int row, int col, Container parent, int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }

    /**
     * Aligns the first <code>rows</code> * <code>cols</code>
     * components of <code>parent</code> in
     * a grid. Each component in a column is as wide as the maximum
     * preferred width of the components in that column;
     * height is similarly determined for each row.
     * The parent is made just big enough to fit them all.
     *
     * @param parent   component containing us
     * @param rows     number of rows
     * @param cols     number of columns
     * @param initialX x location to start the grid at
     * @param initialY y location to start the grid at
     * @param xPad     x padding between cells
     * @param yPad     y padding between cells
     */
    private static void makeCompactGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        //Align all cells in each column and make them the same width.
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width, getConstraintsForCell(r, c, parent, cols).getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }

        //Align all cells in each row and make them the same height.
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height, getConstraintsForCell(r, c, parent, cols).getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }

        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.EAST, x);
    }

}
