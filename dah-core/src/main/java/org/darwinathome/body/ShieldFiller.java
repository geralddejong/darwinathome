// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Joint;
import org.darwinathome.geometry.structure.Thing;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Fabricate the various things that are stored in parts of the fabric
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class ShieldFiller implements Thing.Factory {
    private Thing.Factory jointThingFactory;

    public ShieldFiller(Thing.Factory jointThingFactory) {
        this.jointThingFactory = jointThingFactory;
    }

    public void setFabric(Fabric fabric) {
        jointThingFactory.setFabric(fabric);
    }

    public Thing createFresh(Object target, String descriminator) {
        if (target instanceof Fabric) {
            return new ShieldCenter();
        }
        else if (target instanceof Joint) {
            return jointThingFactory.createFresh(target, descriminator);
        }
        else {
            return null;
        }
    }

    public Thing restoreExisting(DataInputStream dis, Object target) throws IOException {
        if (target instanceof Fabric) {
            return new ShieldCenter();
        }
        else if (target instanceof Joint) {
            return jointThingFactory.restoreExisting(dis, target);
        }
        else {
            return null;
        }
    }
}