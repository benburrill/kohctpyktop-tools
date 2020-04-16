import guru.nidi.graphviz.engine.Format;

import java.io.*;
import java.nio.file.*;

public class KohForm {
    private KohSave save;
    private Level level;

    public KohForm(Path path) throws IOException {
        this.save = KohSave.fromString(Files.readString(path));
        this.level = Level.getLevel(1);  // TODO
    }

    public static void main(String[] args) throws IOException {
        System.out.println("KohForm");

        if (args.length == 1) {
            System.out.println("Got argument " + args[0]);

            var kf = new KohForm(Paths.get(args[0]));

            // kf.save.printLayers(false);

//            System.out.println("Pins:");
//            var pins = kf.save.getPins();
//            for (int i = 0; i < pins.length; i++) {
//                System.out.println(" " + i + ") " + pins[i]);
//            }
//            System.out.println("Pins: " + Arrays.deepToString(kf.save.getPins()));

            System.out.println("Creating graph...");
            var graph = WireGraph.fromSave(kf.save);
            int origLen = Files.readString(Paths.get(args[0])).length();
            String newStr = graph.toDataString();
            System.out.println(newStr);
            float ratio = (float) origLen / newStr.length();
            System.out.printf("%.1fx smaller\n", ratio);

            System.out.println("Converting to graphviz...");
            var graphviz = graph.toGraphviz(kf.level);
            graphviz.render(Format.PNG).toFile(new File(args[0] + ".png"));

//            System.out.println(graphviz.render(Format.DOT).toString());
//            WireGraph.fromSave(kf.save).prettyPrint(kf.level);
//            System.out.println(Arrays.deepToString(WireGraph.fromSave(kf.save).getGraph()));


//            System.out.println("Searching for gate...");
//            Wire result = findGate(kf.save);
//            if (result == null) System.out.println("Didn't find anything");
//            else System.out.println("Gate at " + result);
        } else {
            System.out.println("Wrong number of arguments!");
        }
    }






    // old
    private static Wire findGate(KohSave save, Wire src, CircuitMap<Boolean> seen) {
        System.out.println("Searching " + src);
        seen.set(src, true);

        if (!save.getGateSwitches(src).isEmpty()) return src;

        for (var neighbor : save.getNeighbors(src)) {
            if (!seen.get(neighbor)) {
                Wire result = findGate(save, neighbor, seen);
                if (result != null) return result;
            }
        }

        return null;
    }

    private static Wire findGate(KohSave save) {
        var seen = new CircuitMap<>(save, false);
        for (var pin : save.getPins()) {
            var result = findGate(save, pin, seen);
            if (result != null) return result;
        }

        return null;
    }
}