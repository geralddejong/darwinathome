// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.universe;

import org.darwinathome.Constants;
import org.darwinathome.genetics.Noise;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.math.Space3;
import org.darwinathome.geometry.math.Sphere;
import org.darwinathome.geometry.math.Vertex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * One triangular segment of the sphere's surface, which contains a certain amount of water, as well
 * as a future amount of water to allow for two generations of values.  Water flows into the future of neighboring
 * triangles.
 * <p/>
 * Each segment also has a matrix to facilitate easy checking whether a location is within the triangle
 * or not.
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class SurfacePatch {
    private List<Arrow> corners = new ArrayList<Arrow>();
    private Arrow hit = new Arrow();
    private Arrow middle = new Arrow();
    private List<SurfacePatch> adjacentPatches = new ArrayList<SurfacePatch>();
    private Space3 space = new Space3();
    private float[] water;
    private float futureWater;
    private int index;

    public SurfacePatch(Arrow locationA, Arrow locationB, Arrow locationC, float[] water) {
        corners.add(locationA);
        corners.add(locationB);
        corners.add(locationC);
        this.water = water;
        space.set(
                corners.get(0).x, corners.get(1).x, corners.get(2).x,
                corners.get(0).y, corners.get(1).y, corners.get(2).y,
                corners.get(0).z, corners.get(1).z, corners.get(2).z
        );
        space.invert();
        for (Arrow corner : corners) {
            middle.add(corner);
        }
        middle.setSpan(Constants.SURFACE_RADIUS);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean contains(Arrow arrow) {
        hit.set(arrow);
        space.transform(hit);
        return hit.x > 0 && hit.y > 0 && hit.z > 0;
    }

    public List<SurfacePatch> getAdjacent() {
        return adjacentPatches;
    }

    public void addAdjacent(Collection<SurfacePatch> adjacentList) {
        this.adjacentPatches.addAll(adjacentList);
    }

    public List<Arrow> getCorners() {
        return corners;
    }

    public Arrow getMiddle() {
        return middle;
    }

    public Arrow getWettest(Arrow location) {
        SurfacePatch wettest = this;
        double distance = location.distanceTo(wettest.getMiddle());
        if (!Constants.Calc.isAcceptableGoalDistance(distance)) {
            wettest = adjacentPatches.get(0);
        }
        for (SurfacePatch wetter : adjacentPatches) {
            distance = location.distanceTo(wetter.getMiddle());
            if (wetter.getWaterLevel() > wettest.getWaterLevel() && Constants.Calc.isAcceptableGoalDistance(distance)) {
                wettest = wetter;
            }
        }
        return wettest.getMiddle();
    }

    public Arrow getDriest(Arrow location) {
        SurfacePatch driest = this;
        double distance = location.distanceTo(driest.getMiddle());
        if (!Constants.Calc.isAcceptableGoalDistance(distance)) {
            driest = adjacentPatches.get(0);
        }
        for (SurfacePatch drier : adjacentPatches) {
            distance = location.distanceTo(drier.getMiddle());
            if (drier.getWaterLevel() < driest.getWaterLevel() && Constants.Calc.isAcceptableGoalDistance(distance)) {
                driest = drier;
            }
        }
        return driest.getMiddle();
    }

    public float getWaterLevel() {
        float value = water[index];
        if (value < 0) {
            return 0;
        }
        else if (value > 1) {
            return 1;
        }
        else {
            return value;
        }
    }

    public float consumeWater(float howMuch) {
        if (howMuch < water[index]) {
            water[index] -= howMuch;
            return howMuch;
        }
        else {
            howMuch = water[index];
            water[index] = 0;
            return howMuch;
        }
    }

    private boolean isDeep() {
        return getWaterLevel() > 0.5f;
    }

    public double prepare() {
        return futureWater = getWaterLevel();
    }

    public void distribute() {
        float water0 = getWaterLevel();
        if (water0 > 1) { // emergency!  dispense with the extra water
            float flow = (water0 - 1) / adjacentPatches.size();
            for (SurfacePatch adjacent : this.adjacentPatches) {
                flowTo(adjacent, flow);
            }
        }
        else if (water0 > 0) {
            int deepCount = 0;
            for (SurfacePatch adjacent : this.adjacentPatches) {
                if (adjacent.isDeep()) {
                    deepCount++;
                }
            }
            switch (deepCount) {
                case 0:
//                    if (isDeep()) {
                    for (SurfacePatch adjacent : this.adjacentPatches) {
                        if (adjacent.isDeep()) {
                            flowTo(adjacent, water0 / 3);
                        }
                    }
//                    }
                    break;
                case 1:
                    // i shouldn't be deep
                    for (SurfacePatch adjacent : this.adjacentPatches) {
                        if (adjacent.isDeep()) {
                            flowTo(adjacent, water0);
                        }
                    }
                    break;
                case 2:
                    // i should be extreme
                    if (isDeep()) {
                        for (SurfacePatch adjacent : this.adjacentPatches) {
                            if (adjacent.isDeep()) {
                                flowTo(adjacent, -(1 - water0) / 2);
                            }
                        }

                    }
                    else {
                        for (SurfacePatch adjacent : this.adjacentPatches) {
                            if (!adjacent.isDeep()) {
                                flowTo(adjacent, water0 / 2);
                            }
                        }

                    }
                    break;
                case 3:
                    // i should be deep too
//                    if (!isDeep()) {
                    for (SurfacePatch adjacent : this.adjacentPatches) {
                        flowTo(adjacent, -(1 - water0) / 3f);
                    }
//                    }
                    break;
            }
        }
    }

    public void rain(double amount) {
        futureWater += amount;
    }

    public void commit() {
        water[index] = futureWater;
    }

    public String toString() {
        return "Surface(" + getWaterLevel() + ")";
    }

    private void flowTo(SurfacePatch other, float flow) {
        flow *= 0.1f;
        futureWater -= flow;
        other.futureWater += flow;
    }

    public interface Holder {
        void setSurfacePatch(SurfacePatch surfacePatch);

        SurfacePatch getSurfacePatch();
    }

    private static class Key implements Comparable<Key> {
        private List<Node> nodes;

        Key(Node... nodes) {
            this.nodes = Arrays.asList(nodes);
            Arrays.sort(nodes);
        }

        public int compareTo(Key o) {
            for (int walk = 0; walk < nodes.size(); walk++) {
                int comparison = nodes.get(walk).compareTo(o.nodes.get(walk));
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        }

        public boolean isFaceConnected(Key other) {
            Set<Node> nodeSet = new TreeSet<Node>();
            addNodes(nodeSet);
            other.addNodes(nodeSet);
            return nodeSet.size() == 4;
        }

        private void addNodes(Set<Node> nodeSet) {
            nodeSet.addAll(nodes);
        }
    }

    private static class Node implements Comparable<Node> {
        private Vertex<Node> vertex;
        private Map<Key, SurfacePatch> localPatches = new TreeMap<Key, SurfacePatch>();

        public Node(Vertex<Node> vertex) {
            this.vertex = vertex;
        }

        public void buildTriangles(Map<Key, SurfacePatch> surfacePatchMap, float[] water) {
            List<Vertex<Node>> near = vertex.getNearby();
            for (int walk = 0; walk < near.size(); walk++) {
                Node current = near.get(walk).getOccupant();
                Node next = near.get((walk + 1) % near.size()).getOccupant();
                Key key = new Key(this, current, next);
                SurfacePatch surfacePatch = surfacePatchMap.get(key);
                if (surfacePatch == null) {
                    surfacePatch = new SurfacePatch(
                            this.vertex.getLocation(),
                            next.vertex.getLocation(),
                            current.vertex.getLocation(),
                            water
                    );
                    surfacePatchMap.put(key, surfacePatch);
                }
                localPatches.put(key, surfacePatch);
            }
        }

        public int compareTo(Node o) {
            return vertex.getIndex() - o.vertex.getIndex();
        }
    }

    public static float[] createWater(Noise noise) {
        Sphere<Node> sphere = new Sphere<Node>(Constants.FREQUENCY);
        float[] water = new float[sphere.getFaceCount()];
        for (int walk = 0; walk < water.length; walk++) {
            water[walk] = (float) noise.nextDouble();
        }
        return water;
    }

    public static List<SurfacePatch> createSurfacePatches(final float[] water) {
        final Map<Key, SurfacePatch> patches = new TreeMap<Key, SurfacePatch>();
        Sphere<Node> sphere = new Sphere<Node>(Constants.FREQUENCY);
        sphere.setRadius(Constants.SURFACE_RADIUS);
        sphere.admitVisitor(new Vertex.Visitor<Node>() {
            public void visit(Vertex<Node> vertex) {
                vertex.setOccupant(new Node(vertex));
            }
        });
        sphere.admitVisitor(new Vertex.Visitor<Node>() {
            public void visit(Vertex<Node> vertex) {
                vertex.getOccupant().buildTriangles(patches, water);
            }
        });
        for (Map.Entry<Key, SurfacePatch> entry : patches.entrySet()) {
            Map<Key, SurfacePatch> adjacentPatches = new TreeMap<Key, SurfacePatch>();
            for (Node node : entry.getKey().nodes) {
                for (Map.Entry<Key, SurfacePatch> nodeEntry : node.localPatches.entrySet()) {
                    if (nodeEntry.getValue() != entry.getValue() && nodeEntry.getKey().isFaceConnected(entry.getKey())) {
                        adjacentPatches.put(nodeEntry.getKey(), nodeEntry.getValue());
                    }
                }
            }
            if (adjacentPatches.size() != 3) {
                throw new RuntimeException("Always 3 adjacent patches");
            }
            entry.getValue().addAdjacent(adjacentPatches.values());
        }
        if (patches.size() != sphere.getFaceCount()) {
            throw new RuntimeException("Faces was " + patches.size() + " but should be " + sphere.getFaceCount());
        }
        List<SurfacePatch> surfacePatchList = new ArrayList<SurfacePatch>(patches.values()); // order is alphabetical using key
        int index = 0;
        for (SurfacePatch surfacePatch : surfacePatchList) {
            surfacePatch.setIndex(index++);
        }
        return surfacePatchList;
    }

    public static SurfacePatch fetchPatch(Arrow arrow, Collection<SurfacePatch> surfacePatches) {
        SurfacePatch nearest = null;
        double nearestDot = -1;
        for (SurfacePatch surfacePatch : surfacePatches) {
            double dot = surfacePatch.getMiddle().dot(arrow)/surfacePatch.getMiddle().span()/arrow.span();
            if (dot > nearestDot) {
                nearest = surfacePatch;
                nearestDot = dot;
            }
        }
        if (nearest == null) {
            throw new RuntimeException("Need surfacePatch");
        }
        return nearest;
    }

}
