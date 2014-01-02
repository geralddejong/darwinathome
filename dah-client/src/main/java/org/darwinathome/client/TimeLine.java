// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.geometry.jogl.HeadsUp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The text that appears to show the status of this being
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class TimeLine implements HeadsUp.Line {
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM");
    private String prompt;
    private String timeString;
    private boolean hasChanged;

    public TimeLine(String prompt) {
        this.prompt = prompt;
        this.timeString = prompt + "?";
    }

    public void setTimeString(String ageString) {
        this.timeString = ageString;
        hasChanged = true;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    public String getText() {
        hasChanged = false;
        return timeString;
    }

    public void setTime(long time) {
        setTimeString(prompt + dateFormat.format(new Date(time)));
    }
}
