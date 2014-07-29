package ply;

import utils.ByteSize;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PLYReader implements AutoCloseable {

    private static final Logger log = Logger.getLogger(PLYReader.class.getName());

    private final String filePath;
    private final RandomAccessFile raf;
    private final FileChannel fileChannel;
    private final MappedByteBuffer buffer;
    private final PLYHeader header;

    public float[] vertices;
    public int[] indices;

    public PLYReader(final String path) throws IOException {
        log.log(Level.FINE, "opening file located at {0}", path);

        this.filePath = path;
        this.raf = new RandomAccessFile(this.filePath, "r");
        this.fileChannel = this.raf.getChannel();
        this.buffer = this.fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, Math.min(this.fileChannel.size(), Integer.MAX_VALUE));
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.header = new PLYHeader(this.buffer);

        readVertices();

        log.log(Level.FINE, "finished reading file");
    }

    private void readVertices() {

        vertices = new float[this.header.getVertexCount() * 3];
        indices = new int[this.header.getFaceCount() * 3];

        // Set marker to start of the data.
        this.buffer.position(this.header.getDataOffset());

        // Create a view of the byte buffer as a float buffer to allow us to read all vertices
        // into our array using a single call - good sequential read.
        FloatBuffer fb = this.buffer.asFloatBuffer();
        fb.get(vertices);

        log.log(Level.FINER, "finished reading vertices");

        // Forward the marker of the original buffer past the vertex data we just read.
        this.buffer.position(fb.position() * ByteSize.FLOAT + this.header.getDataOffset());

        for (int i = 0; i < this.header.getFaceCount(); i++) {
            this.buffer.get();
            indices[i * 3] = this.buffer.getInt();
            indices[i * 3 + 1] = this.buffer.getInt();
            indices[i * 3 + 2] = this.buffer.getInt();
        }

        log.log(Level.FINER, "finished reading indices");
    }

    @Override
    public void close() throws IOException {
        log.log(Level.FINER, "closing file resources for {0}", this.filePath);
        this.fileChannel.close();
        this.raf.close();
    }
}
