// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.darwinathome.geometry.structure.Thing;
import org.darwinathome.universe.SurfacePatch;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The middle of the shield
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class ShieldCenter implements SurfacePatch.Holder, Thing {
    private SurfacePatch surfacePatch;

    public void setSurfacePatch(SurfacePatch surfacePatch) {
        this.surfacePatch = surfacePatch;
    }

    public SurfacePatch getSurfacePatch() {
        return surfacePatch;
    }

    public void save(DataOutputStream dos) throws IOException {
    }
}
