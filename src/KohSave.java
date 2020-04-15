// TODO: should saves have Levels attached to them?  Could add level number to
//  save files and load, but... IDK probably a bad idea for several reasons.
//  Maybe could at least add an option to attach a level when constructing,
//  could default to a level with no IO.

import com.exadel.flamingo.flex.messaging.amf.io.AMF3Deserializer;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.zip.*;
import java.io.*;

public class KohSave {
    private int width;
    private int height;
    private int[][] silicon;
    private boolean[][] metal;
    private boolean[][] gatev;
    private boolean[][] gateh;
    private boolean[][] via;
    private boolean[][] sconh;
    private boolean[][] sconv;
    private boolean[][] mconh;
    private boolean[][] mconv;


    public KohSave(int width, int height) {
        if (width != 44 || height != 27)
            throw new IllegalArgumentException("Blasphemous dimensions!");

        this.width = width;
        this.height = height;
        this.silicon = new int[width][height];
        this.metal = new boolean[width][height];
        this.gatev = new boolean[width][height];
        this.gateh = new boolean[width][height];
        this.via = new boolean[width][height];
        this.sconh = new boolean[width][height];
        this.sconv = new boolean[width][height];
        this.mconh = new boolean[width][height];
        this.mconv = new boolean[width][height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean checkWire(Wire wire) {
        int col = wire.getCol();
        int row = wire.getRow();

        if (col < 0 || col >= width || row < 0 || row >= width) return false;
        if (wire.isSilicon() && silicon[col][row] == 0) return false;
        if (wire.isMetal() && !metal[col][row]) return false;

        return true;
    }

    public boolean hasVia(Wire wire) {
        return this.via[wire.getCol()][wire.getRow()];
    }

    public IOPad getPad(Wire wire) {
        if (!wire.isMetal()) return null;

        int col = wire.getCol();
        int row = wire.getRow();

        if (row < 2 || row >= height - 2) return null;
        if ((row - 2) % 4 == 3) return null;
        if (col < 1 || col >= width - 1) return null;
        if (col >= 4 && col < width - 4) return null;

        if (row < 5 || row >= height - 5) return IOPad.VCC;
        int pin = (row - 2) / 4;

        if (col >= width - 4) pin += (height - 3) / 4 - 2;

        // If Level is ever attached to KohSave, then we could get the proper
        // name for the pad.  Maybe doesn't matter though.
        return new IOPad(pin);
    }

    public LinkedList<GateSwitch> getGateSwitches(Wire wire) {
        var result = new LinkedList<GateSwitch>();
        if (wire.isMetal() || !checkWire(wire)) return result;

        int col = wire.getCol();
        int row = wire.getRow();

        Gate gate = Gate.passing(silicon[col][row]);

        // Checking some things I don't think should ever happen in a normal
        // save, but if they do I would like to know.
        if (gate == null || (gateh[col][row] && gatev[col][row])) {
            throw new AssertionError("WTF!?  Weird save");
        }

        for (var dir : Wire.Directions.values()) {
            var neighbor = dir.of(wire);

            // Ensure wire exists
            if (!checkWire(neighbor)) continue;

            // Ensure there is a gate in this direction
            var gated = dir.horizontal()? gateh : gatev;
            if (!gated[col][row]) continue;

            // If there is a connection, we can add the gate
            var cond = dir.horizontal()? sconh : sconv;
            if (cond[dir.conh(wire)][dir.conv(wire)]) {
                result.add(new GateSwitch(gate, neighbor));
            }
        }

        return result;
    }

    public LinkedList<Wire> getNeighbors(Wire src) {
        var result = new LinkedList<Wire>();
        if (!checkWire(src)) return result;

        int col = src.getCol(), row = src.getRow();
        boolean sil = src.isSilicon();

        if (hasVia(src)) {
            var w = new Wire(!sil, col, row);
            if (checkWire(w)) result.add(w);
        }

        // All VCC pads are connected.
        IOPad pad = getPad(src);
        if (pad != null && pad.getPin() == 0) {
            var opts = new Wire[] {
                new Wire(sil, width - col - 1, height - row - 1),
                new Wire(sil, width - col - 1, row),
                new Wire(sil, col, height - row - 1)
            };

            for (var w : opts) if (checkWire(w)) result.add(w);
        }

        var conh = sil? sconh : mconh;
        var conv = sil? sconv : mconv;

        for (var dir : Wire.Directions.values()) {
            var neighbor = dir.of(src);

            // Ensure wire exists
            if (!checkWire(neighbor)) continue;

            // Ensure there is a connection
            var cond = dir.horizontal()? conh : conv;
            if (!cond[dir.conh(src)][dir.conv(src)]) continue;

            // If it's silicon, also check for gates by ensuring that we only
            // connect to the same type of silicon (when silicon crosses over to
            // make a gate, the bottom silicon is used as the silicon type).
            int ncol = neighbor.getCol(), nrow = neighbor.getRow();
            if (sil && silicon[col][row] != silicon[ncol][nrow]) continue;

            // Otherwise, all is good, add it!
            result.add(neighbor);
        }

        return result;
    }

    public Wire[] getPins() {
        // Get a wire corresponding to each pin id.  So it only returns a single
        // VCC pin (the upper left pin) and you can do getPins()[ioPad.getPin()]
        // to get a pin location for an IOPad.

        var perSide = (height - 3) / 4 - 2;
        var result = new Wire[2 * perSide + 1];

        result[0] = new Wire(false, 2, 3);
        for (int i = 1; i <= perSide; i++) {
            int x = 2;
            int y = 3 + 4 * i;
            result[i] = new Wire(false, x, y);
            result[perSide + i] = new Wire(false, width - 1 - x, y);
        }

        for (var pin : result) {
            // Might as well check to see if for some reason any of the pins
            // don't connect to metal.
            if (!checkWire(pin)) {
                throw new AssertionError("Broken pin!");
            }
        }

        return result;
    }

    public static KohSave fromBytes(byte[] bytes) {
        KohSave save;
        Object[][][] layers = new Object[9][][];

        try (var amf = new AMF3Deserializer(new ByteArrayInputStream(bytes))) {
            save = new KohSave((int) amf.readObject(), (int) amf.readObject());
            for (int i = 0; i < layers.length; i++) {
                var cols = (Object[]) amf.readObject();
                layers[i] = new Object[cols.length][];
                for (int c = 0; c < cols.length; c++) {
                    layers[i][c] = (Object[]) cols[c];
                }
            }

            if (amf.available() != 0) {
                throw new IllegalArgumentException("More data than expected!");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        for (int c = 0; c < save.width; c++) {
            for (int r = 0; r < save.height; r++) {
                save.silicon[c][r] = (int) layers[0][c][r];
                // The metal layer is supposed to be all boolean, but sometimes
                // there's an integer here.  And it's a pain to deal with
                // because java is annoying when it comes to types.
                if (layers[1][c][r] instanceof Integer)
                    save.metal[c][r] = 0 != (int) layers[1][c][r];
                else
                    save.metal[c][r] = (boolean) layers[1][c][r];
                save.gatev[c][r] = (boolean) layers[2][c][r];
                save.gateh[c][r] = (boolean) layers[3][c][r];
                save.via[c][r] = (boolean) layers[4][c][r];
                save.sconh[c][r] = (boolean) layers[5][c][r];
                save.sconv[c][r] = (boolean) layers[6][c][r];
                save.mconh[c][r] = (boolean) layers[7][c][r];
                save.mconv[c][r] = (boolean) layers[8][c][r];
            }
        }

        return save;
    }

    public static KohSave fromString(String text){
        try {
            return fromBytes(Util.inflate(Base64.getMimeDecoder().decode(text)));
        } catch (DataFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public byte[] toBytes() {
        // TODO: serialize with AMF and compress
        return null;
    }

    public String toString() {
        return "[KohSave TODO: show base64 (needs toBytes() first)]";
    }

    private String prettyGrid(Object[] arr) {
        return Arrays.deepToString(arr).replace("], ", "],\n ");
    }

    public void printLayers(boolean all) {
        System.out.println("--- [ silicon layer ] ---");
        System.out.println(prettyGrid(silicon));
        System.out.println("--- [ metal layer ] ---");
        System.out.println(prettyGrid(metal));
        if (all) {
            System.out.println("--- [ gatev layer ] ---");
            System.out.println(prettyGrid(gatev));
            System.out.println("--- [ gateh layer ] ---");
            System.out.println(prettyGrid(gateh));
            System.out.println("--- [ via layer ] ---");
            System.out.println(prettyGrid(via));
            System.out.println("--- [ sconh layer ] ---");
            System.out.println(prettyGrid(sconh));
            System.out.println("--- [ sconv layer ] ---");
            System.out.println(prettyGrid(sconv));
            System.out.println("--- [ mconh layer ] ---");
            System.out.println(prettyGrid(mconh));
            System.out.println("--- [ mconv layer ] ---");
            System.out.println(prettyGrid(mconv));
        }
    }
}