// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

/**
 * Directions correspond to movement genes.  Weird tricks are done to determine what the numerical
 * directions are.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public enum Direction { // Forwards/Backwards/Left/Right
    FFF,
    FFR,
    FRR,
    RRR,
    BRR,
    BBR,
    BBB,
    BBL,
    BLL,
    LLL,
    FLL,
    FFL;

    private double forwardness;
    private double rightness;

    Direction() {
        String name = toString();
        int right = 0;
        int forward = 0;
        for (int walk = 0; walk < name.length(); walk++) {
            switch (name.charAt(walk)) {
                case 'R':
                    right++;
                    break;
                case 'L':
                    right--;
                    break;
                case 'F':
                    forward++;
                    break;
                case 'B':
                    forward--;
                    break;
            }
        }
        rightness = right / (double) name.length();
        forwardness = forward / (double) name.length();
        double size = Math.sqrt(rightness * rightness + forwardness * forwardness);
        rightness /= size;
        forwardness /= size;
    }

    public double getForwardness() {
        return forwardness;
    }

    public double getRightness() {
        return rightness;
    }

    public double dot(double forwardness, double rightness) {
        double size = Math.sqrt(rightness * rightness + forwardness * forwardness);
        forwardness /= size;
        rightness /= size;
        return this.forwardness * forwardness + this.rightness * rightness;
    }

    public static Direction getClosest(double forwardness, double rightness) {
        double bestDot = 0;
        Direction closest = null;
        for (Direction direction : values()) {
            double dot = direction.dot(forwardness, rightness);
            if (dot > bestDot) {
                bestDot = dot;
                closest = direction;
            }
        }
        return closest;
    }
}
