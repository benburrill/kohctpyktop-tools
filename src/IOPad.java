public class IOPad {
    private byte pin;
    private String name;

    public static IOPad VCC = new IOPad(0, "+VCC");
    public IOPad(int pin, String name) {
        this.pin = (byte) pin;
        this.name = name;
    }

    public IOPad(int pin) {
        this(pin, null);
    }

    public byte getPin() {
        return pin;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        IOPad other = (IOPad) obj;

        if (this.pin != other.pin) return false;

        // IDK if this is really a good idea...
        if (this.name != null && other.name != null) {
            return this.name.equals(other.name);
        }

        return true;
    }

    @Override
    public int hashCode() {
        return (int) pin;
    }

    @Override
    public String toString() {
        // [IOPad "Y0" (pin 5)]
        return (
            "[IOPad " + (this.name != null?
                "\"" + this.name + "\" " : ""
            ) + "(pin " + this.pin + ")]"
        );
    }
}