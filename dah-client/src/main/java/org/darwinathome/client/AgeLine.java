// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.geometry.jogl.HeadsUp;

/**
 * The text that appears to show the status of this being
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class AgeLine implements HeadsUp.Line {
    private static final String DY = "dy ";
    private static final String HR = "hr ";
    private static final String MIN = "min ";
    private static final String SEC = "sec";
    private String prompt;
    private String ageString;
    private boolean hasChanged;

    public AgeLine(String prompt) {
        this.ageString = this.prompt = prompt;
    }

    public void setAgeString(String ageString) {
        this.ageString = ageString;
        hasChanged = true;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    public String getText() {
        hasChanged = false;
        return ageString;
    }

    public void setAge(long seconds) {
        if (seconds <= 60) {
            setAgeString(prompt + seconds + SEC);
        }
        else {
            long minutes = seconds / 60;
            if (minutes <= 60) {
                seconds %= 60;
                if (seconds < 10) {
                    setAgeString(prompt + minutes + MIN + "0" + seconds + SEC);
                }
                else {
                    setAgeString(prompt + minutes + MIN + seconds + SEC);
                }
            }
            else {
                long hours = minutes / 60;
                minutes %= 60;
                if (hours <= 24) {
                    setAgeString(prompt + hours + HR + minutes + MIN);
                }
                else {
                    long days = hours / 24;
                    minutes %= 60;
                    hours %= 24;
                    setAgeString(prompt + days + DY + hours + HR + minutes + MIN);
                }
            }
        }
    }

}