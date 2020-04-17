import com.exadel.flamingo.flex.messaging.amf.io.AMF3Serializer;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableNode;

import static guru.nidi.graphviz.model.Factory.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class WireGraph {
    private byte[] pinMap;
    private Connection[][] connections;

    public static class Connection {
        private int to;
        private int swch;
        private Gate gate;

        public Connection(int to, int swch, Gate gate) {
            this.to = to;
            this.swch = swch;
            this.gate = gate;
        }

        public Connection(int to, int swch) {
            // Use negative switch to denote PNP, -1 means PNP, switch 0.
            // Think [N]egative for N-type, [P]ositive for P-type.
            this(to, swch < 0? -swch - 1 : swch, swch < 0? Gate.PNP : Gate.NPN);
        }

        public int getConnection() {
            return to;
        }

        public int getSwitch() {
            return swch;
        }

        public int getMarkedSwitch() {
            if (gate == Gate.PNP) return -swch - 1;
            else if (gate == Gate.NPN) return swch;
            else throw new AssertionError("Bad gate");
        }

        public Gate getKind() {
            return gate;
        }

        @Override
        public String toString() {
            return (
                "[" + gate.name() + " connection to " +
                    to + " through " + swch + "]"
            );
        }
    }

    public WireGraph(byte[] pinMap, Connection[][] connections) {
        this.pinMap = pinMap;
        this.connections = connections;
    }

    public static WireGraph fromSave(KohSave save) {
        // TODO: maybe split this off into smaller and more managable methods?
        var pins = save.getPins();
        var nodes = new CircuitMap<Integer>(save, null);
        var seeds = new LinkedList<Wire>(Arrays.asList(pins));
        var gates = new HashSet<Wire>();

        int node = 0;
        while (!seeds.isEmpty()) {
            var seed = seeds.pop();
            if (nodes.get(seed) != null) continue;

            // Flood fill from seed
            var front = new LinkedList<Wire>();
            front.add(seed);

            while (!front.isEmpty()) {
                var wire = front.pop();
                var origWireNode = nodes.get(wire);
                if (origWireNode != null) {
                    // Already found, ignore
                    if (origWireNode == node) continue;

                    // From another node, weird, probably indicates bug.
                    throw new AssertionError("Wire " + wire +
                        " had group " + origWireNode + ", expected " + node
                    );
                }

                var neighbors = save.getNeighbors(wire);
                var gateSwitches = save.getGateSwitches(wire);

                if (gateSwitches.isEmpty()) {
                    // Not a gate, add neighbors
                    nodes.set(wire, node);
                    front.addAll(neighbors);
                } else {
                    // It's a gate, can't continue to neighbors with this fill,
                    // but add neighbors and gate switch(es) to seed queue for
                    // later filling if we don't find them before them.
                    gates.add(wire);
                    seeds.addAll(neighbors);
                    for (var gs : gateSwitches) seeds.add(gs.getSwitch());
                }
            }

            node++;
        }

        // Array of LinkedLists, a bit awkward due to not being allowed to use
        // generic types in array construction.
        @SuppressWarnings("unchecked")
        var graphBuilder = (LinkedList<Connection>[]) new LinkedList<?>[node];

        for (var gate : gates) {
            for (var gs : save.getGateSwitches(gate)) {
                var neighbors = save.getNeighbors(gate);
                for (var src : neighbors) {
                    var srcNode = nodes.get(src);
                    if (graphBuilder[srcNode] == null) {
                        graphBuilder[srcNode] = new LinkedList<>();
                    }

                    for (var dest : neighbors) {
                        if (src == dest) continue;

                        graphBuilder[srcNode].add(new Connection(
                            nodes.get(dest), nodes.get(gs.getSwitch()),
                            gs.getGate()
                        ));
                    }
                }
            }
        }

        var connections = new Connection[graphBuilder.length][];
        for (int i = 0; i < graphBuilder.length; i++) {
            connections[i] = new Connection[] {};
            if (graphBuilder[i] != null) {
                connections[i] = graphBuilder[i].toArray(connections[i]);
            }
        }

        var pinMap = new byte[pins.length];
        for (var i = 0; i < pins.length; i++) {
            pinMap[i] = nodes.get(pins[i]).byteValue();
        }

        return new WireGraph(pinMap, connections);
    }

    public void prettyPrint(Level level) {
        for (int i = 0; i < connections.length; i++) {
            System.out.print("Node " + i);
            var pads = padsForNode(level, i);
            if (pads.size() != 0) {
                System.out.print(" (pads: " + pads + ")");
            }

            System.out.print(" -- ");
            if (connections[i].length != 0) {
                System.out.println(Arrays.toString(connections[i]));
            } else {
                System.out.println("NO GATE CONNECTIONS!");
            }
        }
    }

    public LinkedList<Byte> pinNumsForNode(int node) {
        var result = new LinkedList<Byte>();
        for (int i = 0; i < pinMap.length; i++) {
            if (pinMap[i] == node && pinMap[i] >= 0) {
                result.add((byte) i);
            }
        }

        return result;
    }

    public LinkedList<IOPad> padsForNode(Level level, int node) {
        // TODO: repeated code...  Do I even really need this method?
        var result = new LinkedList<IOPad>();
        for (int i = 0; i < pinMap.length; i++) {
            if (pinMap[i] == node && pinMap[i] >= 0 && level.getPin(i) != null) {
                result.add(level.getPin(i));
            }
        }

        return result;
    }

    public int nodeForPad(IOPad pad) {
        return pinMap[pad.getPin()];
    }

    public Connection[][] getConnections() {
        return connections;
    }

    public WireGraph[] split() {
        // Split a graph into distinct unconnected sub-graphs

        var min = minimized();

        var nodeMap = new Integer[min.connections.length];
        var graphs = new LinkedList<Connection[][]>();
        for (int pinNode : min.pinMap) {
            if (pinNode < 0 || nodeMap[pinNode] != null) continue;
            var graphNum = graphs.size();
            var graph = new Connection[min.connections.length][];
            for (int i = 0; i < graph.length; i++) graph[i] = new Connection[0];
            graphs.add(graph);

            var queue = new LinkedList<Integer>();
            queue.add(pinNode);

            while (!queue.isEmpty()) {
                int curNode = queue.pop();
                if (nodeMap[curNode] != null) {
                    if (nodeMap[curNode] == graphNum) continue;
                    throw new AssertionError("shouldn't happen");
                }

                nodeMap[curNode] = graphNum;
                graph[curNode] = min.connections[curNode];

                for (var con : min.connections[curNode]) {
                    queue.add(con.getConnection());
                    // TODO: maybe add option to split along switches?
                    //  Could represent foreign wires with negative ints?
                    queue.add(con.getSwitch());
                }
            }
        }

        var results = new WireGraph[graphs.size()];

        int i = 0;
        for (var cons : graphs) {
            var pm = min.pinMap.clone();
            for (int pin = 0; pin < pm.length; pin++) {
                if (pm[pin]!= -1 && nodeMap[pm[pin]] != i) pm[pin] = -1;
            }

            results[i++] = new WireGraph(pm, cons).minimized();
        }

        return results;
    }

    public WireGraph minimized() {
        // Minimize a graph, removing extraneous nodes
        // Pins that are not in graph get represented in the pin map as -1 (255)

        var pm = new byte[pinMap.length];
        Arrays.fill(pm, (byte) -1);

        var gph = new LinkedList<Connection[]>();
        var mapping = new int[connections.length];
        var toAdd = new HashSet<Integer>();

        // Inefficient and inelegant... is there a better way to do all this?
        for (int i = 0; i < connections.length; i++) {
            if (pinNumsForNode(i).size() > 1 || connections[i].length > 0) {
                toAdd.add(i);

                for (var con : connections[i]) {
                    toAdd.add(con.getSwitch());
                }
            }
        }

        for (int i = 0; i < connections.length; i++) {
            if (toAdd.contains(i)) {
                mapping[i] = gph.size();
                gph.add(connections[i].clone());

                for (var pin : pinNumsForNode(i)) {
                    pm[pin] = (byte) i;
                }
            }
        }

        for (int i = 0; i < pm.length; i++) {
            if (pm[i] != -1) pm[i] = (byte) mapping[pm[i]];
        }

        for (var con : gph) {
            for (int i = 0; i < con.length; i++) {
                con[i] = new Connection(
                    mapping[con[i].getConnection()],
                    mapping[con[i].getSwitch()],
                    con[i].getKind()
                );
            }
        }

        return new WireGraph(pm, gph.toArray(new Connection[][] {}));
    }

    public Graphviz toGraphviz(Level level) {
        var mg = mutGraph("Graph").setDirected(false);
        var nodes = new MutableNode[connections.length];
        for (int i = 0; i < connections.length; i++){
            nodes[i] = mutNode("node-" + i);
            String label = "<b>Wire Group " + (i + 1) + "</b>";

            var pads = padsForNode(level, i);
            for (var pad : pads) {
                label += "<br/>" + pad.getName();
            }

            nodes[i].add(Label.html(label));

            // Ensure node added if it has direct connections to multiple pads.
            if (pads.size() > 1) mg.add(nodes[i]);
        }

        int conCount = 0;
        for (int i = 0; i < connections.length; i++) {
            for (var con : connections[i]) {
                // Avoid adding connections more than once
                if (con.to < i) continue;

                var pColor = Color.GOLDENROD;
                var nColor = Color.RED4;
                var swchColor = con.gate == Gate.PNP? nColor : pColor;
                var conColor = con.gate == Gate.PNP? pColor : nColor;
                conColor = Color.DIMGRAY;

                var connector = mutNode("connector-" + conCount++);
                connector.add(Shape.POINT, Label.of(""), swchColor);

                mg.add(nodes[i].addLink(to(connector).with(
                    Style.BOLD, conColor, LinkAttr.weight(10)
                )));

                mg.add(connector.addLink(to(nodes[con.to]).with(
                    Style.BOLD, conColor, LinkAttr.weight(10)
                )));

                mg.add(connector.addLink(to(nodes[con.swch]).with(
//                    Style.BOLD.and(Style.DASHED), swchColor
                    Style.BOLD, swchColor
                )));
            }
        }

        return Graphviz.fromGraph(mg);
    }

    public String toDataString() {
        var stream = new ByteArrayOutputStream();
        var serializer = new AMF3Serializer(stream);
        try {
            serializer.writeObject(pinMap);
            var icons = new int[connections.length][];
            for (int i = 0; i < connections.length; i++) {
                icons[i] = new int[connections[i].length * 2];
                for (int j = 0; j < connections[i].length; j++) {
                    icons[i][j * 2] = connections[i][j].getConnection();
                    icons[i][j * 2 + 1] = connections[i][j].getMarkedSwitch();
                }
            }
            serializer.writeObject(icons);
            return Base64.getMimeEncoder().encodeToString(
                Util.deflate(stream.toByteArray(), 9)
            );
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}