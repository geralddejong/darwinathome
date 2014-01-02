// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.network;

/**
 * What can go wrong
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public enum Failure {
    SYSTEM,
    NETWORK_CONNECTON,
    INVALID_OPERATION,
    MARSHALLING,
    PATIENCE,
    SESSION,
    AUTHENTICATION
}

