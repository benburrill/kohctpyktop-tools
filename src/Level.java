import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.LinkedList;

// TODO: lots of bad / duplicate code here, maybe improve?
//  I didn't want to intially, but one way to maybe improve a few things would
//  be to add an enum to IOPad that stores whether it is an input or output and
//  just generally rework things from there.

public class Level {
    private IOPad[] pinMap;
    private byte[] inputs;
    private byte[] outputs;

    public Level(IOPad[] inputs, IOPad[] outputs) {
        this.pinMap = new IOPad[9];
        this.inputs = new byte[inputs.length];
        this.outputs = new byte[outputs.length];

        this.pinMap[0] = IOPad.VCC;

        for (int i = 0; i < inputs.length; i++) {
            byte pin = inputs[i].getPin();
            this.inputs[i] = pin;
            if (this.pinMap[pin] != null) throw new IllegalArgumentException(
                "Duplicate pin " + pin
            );

            this.pinMap[pin] = inputs[i];
        }

        for (int i = 0; i < outputs.length; i++) {
            byte pin = outputs[i].getPin();
            this.outputs[i] = pin;
            if (this.pinMap[pin] != null) throw new IllegalArgumentException(
                "Duplicate pin " + pin
            );

            this.pinMap[pin] = outputs[i];
        }
    }

    // Yeah I'm really starting to think I'm overengineering things...
    public IOPad[] getInputs(boolean includeVCC) {
        int start = includeVCC? 1 : 0;
        var result = new IOPad[inputs.length + start];
        if (includeVCC) result[0] = pinMap[0];
        for (int i = 0; i < inputs.length; i++) {
            result[start + i] = pinMap[inputs[i]];
        }

        return result;
    }

    public IOPad[] getInputs() {
        return getInputs(false);
    }

    public IOPad[] getOutputs() {
        var result = new IOPad[outputs.length];
        for (int i = 0; i < outputs.length; i++) {
            result[i] = pinMap[outputs[i]];
        }

        return result;
    }

    public IOPad[] getAllPads() {
        var inputs = getInputs(true);
        var outputs = getOutputs();

        var result = new IOPad[inputs.length + outputs.length];
        System.arraycopy(inputs, 0, result, 0, inputs.length);
        System.arraycopy(outputs, 0, result, inputs.length, outputs.length);
        return result;
    }

    public Wire[] getWiresFor(IOPad pad) {
        int pin = pad.getPin();
        pad = pinMap[pin];
        if (pad == null) return null; // TODO: decide null or new int[][] {}

        int[][] tiles;
        if (pin == 0) {
            return new Wire[] {
                // pls just kill me
            };
        } else {
            return new Wire[] {
                // 9
            };
        }

        // perhaps would just be better to do the inverse: get pad for tile
        // we can then iterate 1 tile per physical pad and start there,
        // determining whether it is connected to a pad or.... idk.

        // but then we'd want a way to determine if a pad is input or output,
        // which is something I've designed against :(
        // so uh... yay...
    }

    public static Level getLevel(Path path) {
        try (var br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            var inputs = new LinkedList<IOPad>();
            var outputs = new LinkedList<IOPad>();

            for(int pin = 0; (line = br.readLine()) != null;) {
                if ((line = line.trim()).isEmpty()) continue;
                pin++;

                String[] parts = line.split(" ", 2);

                LinkedList<IOPad> cur;
                switch (parts[0]) {
                    case "N/C":
                        continue;
                    case "IN":
                        cur = inputs;
                        break;
                    case "OUT":
                        cur = outputs;
                        break;
                    default:
                        throw new RuntimeException("Invalid syntax: " + line);
                }

                cur.add(new IOPad(pin, parts[1]));
            }

            return new Level(
                inputs.toArray(new IOPad[inputs.size()]),
                outputs.toArray(new IOPad[outputs.size()])
            );
        } catch(IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Level getLevel(int level) {
        return getLevel(Paths.get(Level.class.getResource(
            "levels/level" + level + ".pins"
        ).getFile()));
    }
}