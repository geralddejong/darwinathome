// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Thing;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Create either kind of growth bud
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class BudFactory implements Thing.Factory {
    private LimbBud.Factory limbGrowthBudFactory;
    private TrunkBud.Factory trunkGrowthBudFactory;

    public BudFactory(Energy trunk) {
        trunkGrowthBudFactory = new TrunkBud.Factory(trunk);
        limbGrowthBudFactory = new LimbBud.Factory(null); // never used to create
    }

    public void setFabric(Fabric fabric) {
        limbGrowthBudFactory.setFabric(fabric);
        trunkGrowthBudFactory.setFabric(fabric);
    }

    public Thing createFresh(Object target, String geneName) {
        return trunkGrowthBudFactory.createFresh(target, geneName); // start with trunk
    }

    public Thing restoreExisting(DataInputStream dis, Object target) throws IOException {
        String which = dis.readUTF();
        if ("Limb".equals(which)) {
            return limbGrowthBudFactory.restoreExisting(dis, target);
        }
        else if ("Trunk".equals(which)) {
            return trunkGrowthBudFactory.restoreExisting(dis, target);
        }
        else {
            throw new RuntimeException("Which kind of growth bud");
        }
    }
}
