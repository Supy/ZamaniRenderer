package ply;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.ALL;

class PLYReader implements AutoCloseable {

    private static final Logger log = Logger.getLogger(PLYReader.class.getName());

    private final String filePath;
    private final RandomAccessFile raf;
    private final FileChannel fileChannel;
    private final MappedByteBuffer buffer;
    private final PLYHeader header;

    public PLYReader(final String path) throws IOException {
        log.log(Level.FINE, "opening file located at {0}", path);

        this.filePath = path;
        this.raf = new RandomAccessFile(this.filePath, "r");
        this.fileChannel = this.raf.getChannel();
        this.buffer = this.fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, Math.min(this.fileChannel.size(), Integer.MAX_VALUE));
        this.header = new PLYHeader(this.buffer);
    }

    @Override
    public void close() throws Exception {
        log.log(Level.FINE, "closing file resources for {0}", this.filePath);
        this.fileChannel.close();
        this.raf.close();
    }

    public static void main(String[] args) {

        // Set all logging.
        Logger root = Logger.getLogger("");
        root.setLevel(ALL);
        for (Handler handler : root.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(ALL);
            }
        }

        try {
            new PLYReader(args[0]);
        } catch (IOException ignored) {
        }
    }
}
