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

            System.out.println("Creating graph...");
            var graph = WireGraph.fromSave(kf.save).minimized();
            var graphs = graph.split();
            if (graphs.length != 1) {
                System.out.println("Can split to " + graphs.length + " graphs");
                for (int i = 0; i < graphs.length; i++) {
                    graphs[i].toGraphviz(kf.level).render(Format.PNG).toFile(
                        new File(args[0] + "-" + i + ".png")
                    );
                }
            }
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

        } else {
            System.out.println("Wrong number of arguments!");
        }
    }
}