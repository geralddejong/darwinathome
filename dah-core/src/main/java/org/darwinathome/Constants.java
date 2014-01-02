/*
 * Copyright (C)2008 Gerald de Jong - GNU General Public License
 * please see the LICENSE.TXT in this distribution for more details.
 */

package org.darwinathome;

/**
 * Define the tweakable constants
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public interface Constants {

    // fundamental dimensions
    int FREQUENCY = 20;
    double SURFACE_RADIUS = 800;
    double ORBIT_ALTITUDE = 2000;
    double ROAM_ALTITUDE = 2;
    double ROAM_DISTANCE = 13;
    double HOVER_ALTITUDE = 40;
    double WATCH_FAR = 80;
    double MIN_GOAL_DISTANCE = 12;
    double MAX_GOAL_DISTANCE = 240;

    // evolution
    double GROWTH_COST = 1.0 / 40;
    double SLOPE_TOWARDS_ORIGINAL_PATH = 0.6;
    int POPULATION_SIZE = 24;
    int BIRTH_WAVE_SIZE = 8;
    long MILLIS_PER_HOUR = 1000L * 60 * 60;
    long MILLIS_PER_ITERATION = 1000;
    long ITERATIONS_PER_HOUR = MILLIS_PER_HOUR / MILLIS_PER_ITERATION;
    long ITERATIONS_PER_SAVE = ITERATIONS_PER_HOUR / 6;
    long ITERATIONS_PER_TRAIL_POINT = ITERATIONS_PER_HOUR;
    long ITERATIONS_PER_PATCH_LIFE = ITERATIONS_PER_HOUR / 6;
    int MAX_TRAIL_SIZE = 24;
    long MIN_LIFESPAN = 2 * ITERATIONS_PER_HOUR;
    long MAX_LIFESPAN = 12 * ITERATIONS_PER_HOUR;
    long LIFESPAN_ADVANCE = ITERATIONS_PER_HOUR / 3;
    double CHANCE_OF_MUTATION = 0.01;
    long SPEECH_TIME_TO_LIVE = ITERATIONS_PER_HOUR / 6;

    // tweak factors
    double ORBIT_NAVI_SPEED_FACTOR = 0.05;
    double ORBIT_CLOSE_DOT = 0.999;
    double HOVER_CLOSE_DOT = 0.99999;
    double HOVER_KEY_FACTOR = 3.5;
    double ROAM_KEY_FACTOR = 6;
    double ROAM_FOCUS_CHASE_FACTOR = 1.2;
    double ROAM_INERTIA = 0.2;
    double WATCH_ROTATE_FACTOR = 0.2;
    double WATCH_APPROACH_FACTOR = -2.5;
    double ADJUST_UPWARDS = 0.03;
    double GRAVITY_FACTOR = 0.001;

    // timings
    int MUSCLE_DURATION = 1600;
    double CONTRACTED = 0.8;
    double EXTENDED = 1.2;
    double CONTRACTION_ENERGY = 0.00003;
    double WATER_CONSUMPTION = CONTRACTION_ENERGY * 5;
    int RAIN_LOCATIONS = 300;
    int DYING_TIME = 2000;
    long SPACE_TO_LANDING = 3000;
    long ORBIT_TO_HOVER = 3000;
    long ORBIT_TO_SPACE = 3000;
    long HOVER_TO_ORBIT = 3000;
    long HOVER_TO_ROAM = 2000;
    long ROAM_TO_HOVER = 2000;
    long WATCH_TO_ROAM = 1000;
    long ORBIT_ROTATE = 1000;
    long HOVER_ROTATE = 5000;
    long TIME_UPDATE_MILLIS = 300;
    long IDLE_MILLIS = 30000;

    // derived dimensions
    double REMOTE_RADIUS = SURFACE_RADIUS * 80;
    double FRUSTUM_FAR = REMOTE_RADIUS - SURFACE_RADIUS * 2; // just out of view to start
    double ORBIT_RADIUS = SURFACE_RADIUS + ORBIT_ALTITUDE;
    double ROAM_RADIUS = SURFACE_RADIUS + ROAM_ALTITUDE;
    double HOVER_TOP = SURFACE_RADIUS + HOVER_ALTITUDE * 6;
    double HOVER_MIDDLE = SURFACE_RADIUS + HOVER_ALTITUDE * 3;
    double HOVER_BOTTOM = SURFACE_RADIUS + HOVER_ALTITUDE;
    double SURFACE_ANGLE = Math.atan(1 / Constants.SURFACE_RADIUS);
    double MIN_SHIELD_RADIUS = 0.1;
    double MAX_SHIELD_RADIUS = 1.6;
    double TARGET_TOLERANCE = 1;

    public static class Calc {
        public static boolean isBelowOrbit(double altitude) {
            return altitude <= HOVER_TOP + 1;
        }

        public static boolean isHovering(double altitude) {
            return altitude <= HOVER_TOP && altitude > HOVER_BOTTOM - 1;
        }

        public static boolean isAboveHover(double altitude) {
            return altitude > HOVER_TOP + 1;
        }

        public static boolean isAcceptableGoalDistance(double distance) {
            return distance >= MIN_GOAL_DISTANCE && distance <= MAX_GOAL_DISTANCE;
        }

        public static boolean isCreationMode(long age) {
            return age < ITERATIONS_PER_HOUR * 24;
        }
    }
}
