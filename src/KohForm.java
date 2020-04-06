import java.io.*;
import java.nio.file.*;

public class KohForm {
    public static void main(String[] args) throws IOException {
        System.out.println("KohForm");

        System.out.println("Searching for gate...");


        if (args.length == 1) {
            System.out.println("Got argument " + args[0]);

            var save = KohSave.fromString(Files.readString(Paths.get(args[0])));
            // save.printLayers(false);
            Wire result = findGate(save);
            if (result == null) System.out.println("Didn't find anything");
            else System.out.println("Gate at " + result);
        }
    }

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