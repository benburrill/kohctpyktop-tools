public class Wire {
    private boolean silicon;
    private int col;
    private int row;

    public Wire(boolean silicon, int col, int row) {
        this.silicon = silicon;
        this.col = col;
        this.row = row;
    }

    public boolean isSilicon() {
        return silicon;
    }

    public boolean isMetal() {
        return !silicon;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    @Override
    public String toString() {
        return (
            "[" + (silicon? "silicon" : "metal") +
            " wire at (" + col + ", " + row + ")]"
        );
    }

    // TODO: implement equals and hashCode?

    public enum Directions {
        NORTH(0, -1),
        SOUTH(0, +1),
        EAST(+1, 0),
        WEST(-1, 0);

        private int cof;
        private int rof;

        Directions(int cof, int rof) {
            this.cof = cof;
            this.rof = rof;
        }

        public Wire of(Wire wire) {
            // For example, NORTH.of(wire)
            // Absolutely not guaranteed to return a valid Wire, so you should
            // use save.checkWire on it.

            return new Wire(
                wire.isSilicon(),
                wire.getCol() + cof,
                wire.getRow() + rof
            );
        }

        // conh and conv give indices that can be used in sconh, etc.
        public int conh(Wire wire) {
            return wire.getCol() + (cof > 0? 0 : cof);
        }

        public int conv(Wire wire) {
            return wire.getRow() + (rof > 0? 0 : rof);
        }

        public boolean vertical() {
            return this.cof == 0;
        }

        public boolean horizontal() {
            return this.rof == 0;
        }
    }
}