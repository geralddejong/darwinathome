// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.Constants;
import org.darwinathome.body.Being;
import org.darwinathome.geometry.jogl.HeadsUp;
import org.darwinathome.universe.World;

/**
 * The text that appears to show the status of the world
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class WorldFacts implements Core.TimeListener {
    private static final HeadsUp.Pos POS = HeadsUp.Pos.TOP_LEFT;
    private PopulationLabel populationLabel = new PopulationLabel();
    private TimeLine centralTimeLine = new TimeLine("Central Time: ");
    private TimeLine nowTimeLine = new TimeLine("Time Here: ");
    private AgeLine historyAgeLine = new AgeLine("All of History: ");
    private AgeLine futureAgeLine = new AgeLine("In the Future: ");

    public WorldFacts(HeadsUp headsUp) {
        headsUp.set(
                POS,
                new HeadsUp.Fixed("The Whole World"),
                centralTimeLine,
                historyAgeLine,
                populationLabel,
                nowTimeLine,
                futureAgeLine
        );
    }

    private class PopulationLabel implements HeadsUp.Line {
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
                text = "Population: " + count;
                this.count = count;
                hasChanged = true;
            }
        }
    }

    public void snapshot(World world) {
        populationLabel.setCount(world.getBeings().size());
        long time = System.currentTimeMillis();
        centralTimeLine.setTime(time);
        nowTimeLine.setTime(time);
        historyAgeLine.setAge(world.getAge());
        futureAgeLine.setAge(0);
    }

    @Override
    public void beingSet(Being being) {
    }

    public void timeIs(long frozenTime, long frozenWorldAge, long currentWorldAge, Being being) {
        long currentTime = frozenTime + Constants.MILLIS_PER_ITERATION * (currentWorldAge - frozenWorldAge);
        nowTimeLine.setTime(currentTime);
        futureAgeLine.setAge(currentWorldAge - frozenWorldAge);
    }
}