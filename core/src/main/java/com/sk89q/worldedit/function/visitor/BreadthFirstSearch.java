package com.sk89q.worldedit.function.visitor;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.IntegerTrio;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BreadthFirstSearch implements Operation {

    private final RegionFunction function;
    private final List<Vector> directions = new ArrayList<>();
    private final Map<Node, Integer> visited;
    private final ArrayDeque<Node> queue;
    private final int maxDepth;
    private int affected = 0;

    public BreadthFirstSearch(final RegionFunction function) {
        this(function, Integer.MAX_VALUE);
    }

    public BreadthFirstSearch(final RegionFunction function, int maxDepth) {
        this.queue = new ArrayDeque<>();
        this.visited = new LinkedHashMap<>();
        this.function = function;
        this.directions.add(new Vector(0, -1, 0));
        this.directions.add(new Vector(0, 1, 0));
        this.directions.add(new Vector(-1, 0, 0));
        this.directions.add(new Vector(1, 0, 0));
        this.directions.add(new Vector(0, 0, -1));
        this.directions.add(new Vector(0, 0, 1));
        this.maxDepth = maxDepth;
    }

    public abstract boolean isVisitable(Vector from, Vector to);

    public Collection<Vector> getDirections() {
        return this.directions;
    }

    private IntegerTrio[] getIntDirections() {
        IntegerTrio[] array = new IntegerTrio[directions.size()];
        for (int i = 0; i < array.length; i++) {
            Vector dir = directions.get(i);
            array[i] = new IntegerTrio(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
        }
        return array;
    }

    public void visit(final Vector pos) {
        Node node = new Node(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
        if (!isVisited(node)) {
            isVisitable(pos, pos); // Ignore this, just to initialize mask on this point
            queue.add(node);
            addVisited(node, 0);
        }
    }

    public void addVisited(Node node, int depth) {
        visited.put(node, depth);
    }

    public boolean isVisited(Node node) {
        return visited.containsKey(node);
    }

    public void cleanVisited(int layer) {
        if (layer == Integer.MAX_VALUE) {
            visited.clear();
            return;
        }
        int size = visited.size();
        if (size > 16384) {
            Iterator<Map.Entry<Node, Integer>> iter = visited.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Node, Integer> entry = iter.next();
                Integer val = entry.getValue();
                if (val < layer) {
                    iter.remove();
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        Node from;
        Node adjacent;
        MutableBlockVector mutable = new MutableBlockVector();
        Vector mutable2 = new Vector();
        boolean shouldTrim = false;
        IntegerTrio[] dirs = getIntDirections();
        for (int layer = 0; !queue.isEmpty() && layer <= maxDepth; layer++) {
            int size = queue.size();
            if (layer == maxDepth) {
                cleanVisited(Integer.MAX_VALUE);
                for (Node current : queue) {
                    mutable.mutX(current.getX());
                    mutable.mutY(current.getY());
                    mutable.mutZ(current.getZ());
                    function.apply(mutable);
                    affected++;
                }
                break;
            }
            for (int i = 0; i < size; i++) {
                from = queue.poll();
                mutable.mutX(from.getX());
                mutable.mutY(from.getY());
                mutable.mutZ(from.getZ());
                function.apply(mutable);
                affected++;
                for (IntegerTrio direction : dirs) {
                    mutable2.mutX(from.getX() + direction.x);
                    mutable2.mutY(from.getY() + direction.y);
                    mutable2.mutZ(from.getZ() + direction.z);
                    if (isVisitable(mutable, mutable2)) {
                        adjacent = new Node(mutable2.getBlockX(), mutable2.getBlockY(), mutable2.getBlockZ());
                        if (!isVisited(adjacent)) {
                            addVisited(adjacent, layer);
                            queue.add(adjacent);
                        }
                    }
                }
            }
            int lastLayer = layer - 1;
            cleanVisited(lastLayer);
        }
        return null;
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        messages.add(BBC.VISITOR_BLOCK.format(getAffected()));
    }

    public int getAffected() {
        return this.affected;
    }

    @Override
    public void cancel() {
    }

    public static final class Node {
        private int x,y,z;

        public Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Node(Node node) {
            this.x = node.x;
            this.y = node.y;
            this.z = node.z;
        }

        public Node() {}

        public final void set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public final void set(Node node) {
            this.x = node.x;
            this.y = node.y;
            this.z = node.z;
        }

        @Override
        public final int hashCode() {
            return (x ^ (z << 12)) ^ (y << 24);
        }

        public final int getX() {
            return x;
        }

        public final int getY() {
            return y;
        }

        public final int getZ() {
            return z;
        }

        @Override
        public String toString() {
            return x + "," + y + "," + z;
        }

        @Override
        public boolean equals(Object obj) {
            Node other = (Node) obj;
            return other.x == x && other.z == z && other.y == y;
        }
    }

    public static Class<?> inject() {
        return BreadthFirstSearch.class;
    }
}
