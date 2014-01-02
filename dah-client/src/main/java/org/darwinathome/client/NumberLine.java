// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.geometry.jogl.HeadsUp;

import java.text.DecimalFormat;

/**
 * The text that appears to show the status of this being
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class NumberLine implements HeadsUp.Line {
    private DecimalFormat decimalFormat;
    private String prompt;
    private String numberString;
    private double number = Double.NaN;
    private boolean hasChanged;

    public NumberLine(String prompt, String formatPattern) {
        this.prompt = prompt;
        this.decimalFormat = new DecimalFormat(formatPattern);
        setNumber(0);
    }

    public void setNumberString(String ageString) {
        this.numberString = ageString;
        hasChanged = true;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    public String getText() {
        hasChanged = false;
        return numberString;
    }

    public void setNumber(double number) {
        if (number != this.number) {
            setNumberString(prompt + decimalFormat.format(number));
            this.number = number;
        }
    }
}