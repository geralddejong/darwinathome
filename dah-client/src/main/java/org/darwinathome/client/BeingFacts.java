// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.body.Being;
import org.darwinathome.geometry.jogl.HeadsUp;
import org.darwinathome.universe.World;

/**
 * The text that appears to show the status of this being
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class BeingFacts implements Core.TimeListener {
    private static final HeadsUp.Pos POS = HeadsUp.Pos.TOP_RIGHT;
    private HeadsUp headsUp;
    private AgeLine ageLabel = new AgeLine("Age: ");
    private NumberLine fitnessLine = new NumberLine("Fitness: ", "#0.000");
    private NumberLine energyLine = new NumberLine("Energy: ", "#0.000");
    private NumberLine distanceLine = new NumberLine("Distance: ", "#0.000");

    public BeingFacts(HeadsUp headsUp) {
        this.headsUp = headsUp;
    }

    public void snapshot(World world) {
    }

    @Override
    public void beingSet(Being being) {
        if (being != null) {
            headsUp.set(
                    POS,
                    new HeadsUp.Fixed("Your Tetragotchi"),
                    new HeadsUp.Fixed("Name: " + being.toString()),
                    ageLabel,
                    energyLine,
                    fitnessLine,
                    distanceLine
            );
        }
        else {
            headsUp.set(
                    POS,
                    new HeadsUp.Fixed("Your Tetragotchi"),
                    new HeadsUp.Fixed("Create one!")
            );
        }
    }

    public void timeIs(long frozenTime, long frozenWorldAge, long currentWorldAge, Being being) {
        if (being != null) {
            ageLabel.setAge(being.getBody().getAge());
            fitnessLine.setNumber(being.getFitness());
            energyLine.setNumber(being.getEnergy().getAmount());
            distanceLine.setNumber(being.getGeometry().getDistanceToGoal());
        }
    }

    private class ProgenyLabel implements HeadsUp.Line {
        private String text = "";
        private int count = -1;
        private boolean hasChanged;

        public boolean hasChanged() {
            return hasChanged;
        }

        public String getText() {
            return text;
        }

        public void setCount(int count) {
            if (count != this.count) {
                text = "Progeny: " + count;
                this.count = count;
                hasChanged = true;
            }
        }
    }

}
