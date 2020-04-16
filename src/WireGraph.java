import com.exadel.flamingo.flex.messaging.amf.io.AMF3Serializer;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableNode;

import static guru.nidi.graphviz.model.Factory.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedList;

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
            if (pads.length != 0) {
                System.out.print(" (pads: " + Arrays.toString(pads) + ")");
            }

            System.out.print(" -- ");
            if (connections[i].length != 0) {
                System.out.println(Arrays.toString(connections[i]));
            } else {
                System.out.println("NO GATE CONNECTIONS!");
            }
        }
    }

    public IOPad[] padsForNode(Level level, int node) {
        var result = new LinkedList<IOPad>();
        for (int i = 0; i < pinMap.length; i++) {
            if (pinMap[i] == node && pinMap[i] >= 0 && level.getPin(i) != null) {
                result.add(level.getPin(i));
            }
        }

        return result.toArray(new IOPad[] {});
    }

    public int nodeForPad(IOPad pad) {
        return pinMap[pad.getPin()];
    }

    public Connection[][] getConnections() {
        return connections;
    }

    // It would be cool to have a method to split a disconnected WireGraph into
    // smaller sub-graphs that are fully connected.
    public LinkedList<WireGraph> split() {
        return null; // TODO
    }

    // Also maybe a method to minimize a graph only to the nodes that are
    // actually connected to everything.  Like with split(), we would need to
    // figure out a way to represent missing pads (or just include nodes for
    // those pads even if they are unused, but that kinda seems to defeat the
    // purpose of minimizing or splitting).  So far, I think the best option
    // would be just to represent them as node 255, or maybe we could use a
    // HashMap instead of an Array or maybe an array mapping to something other
    // than indicies (maybe adjacency list or a class to represent nodes).
    // Oh actually fun fact about that 255 idea is that Java bytes actually are
    // signed, so we could do -1 in a sensible way.  (Would be equivalent to 255
    // except byte 255 is actually shown as -1 in Java)
    public WireGraph minimized() {
        var pm = this.pinMap.clone();
        for (int i = 0; i < pm.length; i++) {
            if (pm[i] == i && connections[i].length == 0) {
                pm[i] = -1;
            }
            // No this stupid.
            // pm[pm[i]] = pm[i];

            // Our goal is to avoid nulling out pins that are shorted with
            // another one.  I don't think we can do like the above though.
            // Maybe if we did some sort of thing where we iterated backwards?

        }

        var gph = new LinkedList<LinkedList<Connection>>();

        // TODO: ...
        return null; // TODO
    }

    // TODO: graphviz
    //  https://github.com/nidi3/graphviz-java (use mutable api probably)
    //  https://stackoverflow.com/questions/3718025/graphviz-dot-how-to-insert-arrows-from-a-node-to-center-of-an-arrow#4634943
    //  https://stackoverflow.com/questions/22617837/graphviz-with-combined-edges

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
            if (pads.length > 1) mg.add(nodes[i]);
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


    // TODO: serialize
}