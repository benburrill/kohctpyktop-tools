import java.io.ByteArrayOutputStream;
import java.util.zip.*;

public class Util {
    public static byte[] inflate(byte[] compressed) throws DataFormatException {
        var inflater = new Inflater();
        inflater.setInput(compressed);

        var stream = new ByteArrayOutputStream();
        var buf = new byte[1024];

        while (true) {
            int len = inflater.inflate(buf);
            if (len <= 0) break;

            stream.write(buf, 0, len);
        }

        return stream.toByteArray();
    }

    public static byte[] deflate(byte[] data, int level) {
        var deflater = new Deflater(level);
        deflater.setInput(data);
        deflater.finish();

        var stream = new ByteArrayOutputStream();
        var buf = new byte[1024];

        while (true) {
            int len = deflater.deflate(buf);
            if (len <= 0) break;

            stream.write(buf, 0, len);
        }

        return stream.toByteArray();
    }
}
