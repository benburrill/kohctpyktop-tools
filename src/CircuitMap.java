// stores data about tiles
// but is 3d, there is a silicon layer and a metal layer


public class CircuitMap<T> {
    private T[][][] map;

    public CircuitMap(KohSave save, T initial) {
        @SuppressWarnings("unchecked")
        var arr = (T[][][]) new Object[save.getWidth()][save.getHeight()][2];
        for (int col = 0; col < save.getWidth(); col++) {
            for (int row = 0; row < save.getHeight(); row++) {
                arr[col][row][0] = initial;
                arr[col][row][1] = initial;
            }
        }

        this.map = arr;
    }

    public void set(Wire wire, T val) {
        map[wire.getCol()][wire.getRow()][wire.isSilicon()? 1 : 0] = val;
    }

    public T get(Wire wire) {
        return map[wire.getCol()][wire.getRow()][wire.isSilicon()? 1 : 0];
    }
}