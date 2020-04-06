public class GateSwitch {
    private Gate gate;
    private Wire swch;

    public GateSwitch(Gate gate, Wire swch) {
        this.gate = gate;
        this.swch = swch;
    }

    public Gate getGate() {
        return gate;
    }

    public Wire getSwitch() {
        return swch;
    }

    @Override
    public String toString() {
        return (
            "[" + super.toString() + ": gate: " + gate +
            ", switch: " + swch + "]"
        );
    }
}
