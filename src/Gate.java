public enum Gate {
    NPN(1),
    PNP(2);

    // passage is the silicon kind (1 for N, 2 for P) that is under the gate
    // this is important to interact with the save data.
    // Potentially though, I don't really need to store this in the enum and I
    // only need Gate.passing.  I'll need to see.
    private int passage;
    Gate(int passage) {
        this.passage = passage;
    }

    public static Gate passing(int passage) {
        switch (passage) {
            case 1:
                return NPN;
            case 2:
                return PNP;
            default:
                return null;
        }
    }

    public int getPassage() {
        return passage;
    }

    // Can current pass?
    public boolean isPassable(boolean swch) {
        return swch == (this == NPN);
    }

}