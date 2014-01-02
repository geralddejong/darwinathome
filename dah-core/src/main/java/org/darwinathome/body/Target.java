// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.darwinathome.geometry.math.Arrow;

/**
 * The target, which is a location with optionally a body name
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Target {
    private Arrow location;
    private String preyName;

    public Target(Arrow location, String preyName) {
        this.location = location;
        this.preyName = preyName;
    }

    public Target(Arrow location) {
        this.location = location;
        this.preyName = "";
    }

    public Arrow getLocation() {
        return location;
    }

    public String getPreyName() {
        return preyName;
    }
}
