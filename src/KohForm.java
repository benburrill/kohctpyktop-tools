import java.io.*;
import java.nio.file.*;

public class KohForm {
    public static void main(String[] args) throws IOException {
        System.out.println("KohForm");
        if (args.length == 1) {
            System.out.println("Got argument " + args[0]);

            var save = KohSave.fromString(Files.readString(Paths.get(args[0])));
            save.printLayers(false);
        }
    }
}