// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.geometry.jogl.HeadsUp;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle key commands
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class InteractionFacts implements Core.StateListener {
    private static final HeadsUp.Pos POS = HeadsUp.Pos.BOTTOM_RIGHT;

    private HeadsUp headsUp;
    private List<HeadsUp.Fixed> lines = new ArrayList<HeadsUp.Fixed>();

    public InteractionFacts(HeadsUp headsUp) {
        this.headsUp = headsUp;
    }

    public void stateChanged(Core core) {
        lines.clear();
        switch (core.getSpokeState()) {
            case FROZEN:
                add("State: Time Frozen");
                switch (core.getShipLevel()) {
                    case TRANSITION:
                    case SPACE:
                    case ORBIT:
                        break;
                    default:
                        add("[space] Start future simulation");
                        if (core.isCreationMode()) {
                            add("[shift-space] (Re)create your Tetragotchi here");
                        }
                        else {
                            add("[shift-space] Reload central world");
                        }
                        break;
                }
                break;
            case TIME_ON:
                add("State: Time Progressing");
                if (core.isCreationMode()) {
                    add("[space] Reset and freeze");
                }
                else {
                    add(
                            "[space] Start evolution episode",
                            "[shift-space] Reset and freeze"
                    );
                }
                break;
            case EVOLVING:
                add(
                        "State: Evolution Active",
                        "[s] Start from scratch",
                        "[r] Randomize Genes",
                        "[space] Finish & Save Genome",
                        "[shift-space] Abandon"
                );
                break;
            case SAVE_GENOME:
                break;
        }
        if (core.getShipState() == Core.ShipState.STEADY || (core.getShipState() == Core.ShipState.FALLING && core.getShipLevel() == Core.ShipLevel.SPACE)) {
            switch (core.getShipLevel()) {
                case TRANSITION:
                    break;
                case SPACE:
                    add(
                            "Ship approaching planet"
                    );
                    break;
                case ORBIT:
                    add(
                            "Ship in orbit",
                            "[arrow keys] Travel",
                            "[enter] To Hover"
                    );
                    break;
                case HOVER:
                    add(
                            "Ship hovering",
                            "[arrow keys] Travel",
                            "[up-down together] Lower",
                            "[left-right together] Higher",
                            "[escape] To Orbit",
                            "[enter] To Surface"
                    );
                    break;
                case WATCH:
                    add(
                            "Ship centered on self",
                            "[left-right] Rotate",
                            "[up-down] Distance",
                            "[escape] To Hover",
                            "[enter] To Roam Freely"
                    );
                    break;
                case ROAM:
                    add(
                            "Ship roaming freely",
                            "[left-right] Rotate",
                            "[up-down] Forward-Backward",
                            "[escape] To Hover",
                            "[enter] Return to Centered"
                    );
                    break;
            }
        }
        headsUp.set(POS, lines);
    }

    private void add(String... text) {
        for (String line : text) {
            lines.add(new HeadsUp.Fixed(line));
        }
    }

}