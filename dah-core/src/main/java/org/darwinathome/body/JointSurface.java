// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Joint;
import org.darwinathome.geometry.structure.Thing;
import org.darwinathome.universe.SurfacePatch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Attached to a joint, remembers the triangle that it might collide with.
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class JointSurface implements Thing {
    private SurfacePatch surfacePatch;

    private JointSurface(SurfacePatch surfacePatch) {
        if (surfacePatch == null) {
            throw new RuntimeException("Need surfacePatch");
        }
        this.surfacePatch = surfacePatch;
    }

    public SurfacePatch getPatch(Arrow location) {
        if (!surfacePatch.contains(location)) {
            surfacePatch = SurfacePatch.fetchPatch(location, surfacePatch.getAdjacent());
        }
        return surfacePatch;
    }
    
    public void save(DataOutputStream dos) throws IOException {
        dos.writeShort(surfacePatch.getIndex());
    }

    public String toString() {
        if (surfacePatch == null) {
            return "empty";
        }
        else {
            return surfacePatch.toString();
        }
    }

    public static class Factory implements Thing.Factory {
        private List<SurfacePatch> allSurfacePatches;
        private Fabric fabric;

        public Factory(List<SurfacePatch> allSurfacePatches) {
            this.allSurfacePatches = allSurfacePatches;
        }

        public void setFabric(Fabric fabric) {
            this.fabric = fabric;
        }

        public Thing createFresh(Object target, String descriminator) {
            Joint joint = (Joint)target;
            SurfacePatch.Holder holder = (SurfacePatch.Holder) fabric.getThing();
            SurfacePatch surfacePatch = holder.getSurfacePatch();
            if (surfacePatch == null) {
                surfacePatch = SurfacePatch.fetchPatch(joint.getLocation(), allSurfacePatches); // SLOW!
            }
            return new JointSurface(surfacePatch);
        }

        public Thing restoreExisting(DataInputStream dis, Object target) throws IOException {
            SurfacePatch surfacePatch = allSurfacePatches.get(dis.readShort());
            return new JointSurface(surfacePatch);
        }
    }


}
