// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.Constants;
import org.darwinathome.geometry.jogl.HeadsUp;

/**
 * The text that appears to show the status of the world
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class EvolutionFacts implements Core.EvolutionListener {
    private static final HeadsUp.Pos POS = HeadsUp.Pos.BOTTOM_LEFT;
    private final HeadsUp headsUp;
    private NumberLine averageSpeedLine = new NumberLine("Avg Speed: ", "#0.000");
    private NumberLine topSpeedLine = new NumberLine("Top Speed: ", "#0.000");
    private TimeLine fromTimeLine = new TimeLine("From: ");
    private TimeLine toTimeLine = new TimeLine("To: ");
    private AgeLine lifespanLine = new AgeLine("Future: ");

    public EvolutionFacts(HeadsUp headsUp) {
        this.headsUp = headsUp;
    }

    public void evolutionStarted(long frozenTime, long frozenBeingAge) {
        fromTimeLine.setTime(frozenTime);
        toTimeLine.setTime(frozenTime + Constants.MIN_LIFESPAN * Constants.MILLIS_PER_ITERATION);
        averageSpeedLine.setNumber(0);
        topSpeedLine.setNumber(0);
        lifespanLine.setAge(Constants.MIN_LIFESPAN);
        headsUp.set(
                POS,
                new HeadsUp.Fixed("Evolution Episode"),
                topSpeedLine,
                averageSpeedLine,
                fromTimeLine,
                toTimeLine,
                lifespanLine
        );
    }

    public void evolutionEnded() {
        headsUp.set(POS);
    }

    public void evolutionProgress(long frozenTime, long frozenAge, long lifespan, double averageSpeed, double topSpeed) {
        long toTime = frozenTime + Constants.MILLIS_PER_ITERATION * lifespan;
        toTimeLine.setTime(toTime);
        lifespanLine.setAge(lifespan);
        averageSpeedLine.setNumber(averageSpeed);
        topSpeedLine.setNumber(topSpeed);
    }


}