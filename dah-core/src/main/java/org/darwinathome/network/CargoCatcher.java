// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.network;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Callback for asynchronous communications
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public interface CargoCatcher {

    /**
     * Everything is okay, here's your result
     * @param dis where to fetch the result
     * @throws java.io.IOException didn't work
     */

    void catchCargo(DataInputStream dis) throws IOException;

}