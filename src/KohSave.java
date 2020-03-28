import com.exadel.flamingo.flex.messaging.amf.io.AMF3Deserializer;

import java.util.Arrays;
import java.util.Base64;
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
        // TODO: serialize
        return null;
    }

    public String toString() {
        return "[TODO: show base64]";
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