// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.darwinathome.genetics.Noise;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Face;
import org.darwinathome.geometry.structure.Joint;
import org.darwinathome.geometry.structure.Thing;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Fabricate the various things that are stored in parts of the fabric
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class BodyFiller implements Thing.Factory {
    private Embryo embryo;
    private Noise noise;
    private Thing.Factory jointThingFactory;
    private Thing.Factory growthBudFactory;
    private Being.Outside outside;

    public BodyFiller(Embryo embryo, Noise noise, Thing.Factory jointThingFactory, Being.Outside outside) {
        this.embryo = embryo;
        this.noise = noise;
        this.jointThingFactory = jointThingFactory;
        this.growthBudFactory = new BudFactory(embryo != null ? embryo.trunkEnergy : null);
        this.outside = outside;
    }

    public void setFabric(Fabric fabric) {
        if (this.growthBudFactory != null) {
            this.growthBudFactory.setFabric(fabric);
        }
        if (this.jointThingFactory != null) {
            jointThingFactory.setFabric(fabric);
        }
    }

    public Thing createFresh(Object target, String descriminator) {
        if (target instanceof Face) {
            if (growthBudFactory != null) {
                return growthBudFactory.createFresh(target, descriminator);
            }
            else {
                return null;
            }
        }
        else if (target instanceof Joint) {
            if (jointThingFactory != null) {
                return jointThingFactory.createFresh(target, descriminator);
            }
            else {
                return null;
            }
        }
        else if (target instanceof Fabric) {
            return Being.create(embryo, (Fabric) target, jointThingFactory, outside);
        }
        return null;
    }

    public Thing restoreExisting(DataInputStream dis, Object target) throws IOException {
        if (target instanceof Face) {
            if (growthBudFactory != null) {
                return growthBudFactory.restoreExisting(dis, target);
            }
            else {
                return null;
            }
        }
        else if (target instanceof Joint) {
            if (jointThingFactory != null) {
                return jointThingFactory.restoreExisting(dis, target);
            }
            else {
                return null;
            }
        }
        else if (target instanceof Fabric) {
            return Being.restoreExisting(dis, noise, (Fabric) target, jointThingFactory, outside);
        }
        return null;
    }
}