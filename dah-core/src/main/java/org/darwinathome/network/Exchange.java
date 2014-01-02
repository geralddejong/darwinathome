// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.network;

/**
 * Callback for asynchronous communications
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public interface Exchange {

    /**
     * Everything is okay
     */

    void success();

    /**
     * Something went wrong
     * @param failure what went wrong
     */

    void fail(Failure failure);
}