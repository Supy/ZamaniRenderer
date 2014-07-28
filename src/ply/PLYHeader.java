package ply;

import java.nio.MappedByteBuffer;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PLYHeader {

    private static final Logger log = Logger.getLogger(PLYReader.class.getName());

    private final MappedByteBuffer buffer;
    private final String headerContent;

    private int vertexCount = -1;
    private int faceCount = -1;
    private final int dataOffset;

    private static final Pattern endHeaderPattern = Pattern.compile("[\\s\\S]*?end_header.*?\n", Pattern.MULTILINE | Pattern.UNIX_LINES);

    public PLYHeader(final MappedByteBuffer buffer) throws InvalidFileException {
        log.log(Level.FINE, "processing PLY header information");

        this.buffer = buffer;

        byte[] tmp = new byte[1024];
        this.buffer.get(tmp);

        this.headerContent = isolateHeader(new String(tmp));
        this.dataOffset = this.headerContent.length();

        log.log(Level.FINE, "header isolated. data offset is {0}", this.dataOffset);

        parse();

        log.log(Level.FINE, "extracted header counts - vertices: {0}, faces: {1}", new Object[]{this.vertexCount, this.faceCount});
    }

    // Thanks for this, Ben
    private static String isolateHeader(final String input) throws InvalidFileException {
        Matcher m = endHeaderPattern.matcher(input);
        if (!m.find()) throw new InvalidFileException("PLY header could not be found");
        return m.group();
    }

    private void parse() throws InvalidFileException {
        StringTokenizer tokenizedHeader = new StringTokenizer(this.headerContent, "\n");

        try {
            String line = tokenizedHeader.nextToken();

            if (!line.startsWith("ply")) {
                throw new InvalidFileException("Magic number PLY not present.");
            }

            do {
                line = tokenizedHeader.nextToken().trim();
                String[] parts = line.split(" ");

                log.log(Level.FINEST, "found header line {0}", line);

                if (parts.length == 3 && parts[0].equals("element")) {
                    if (parts[1].equals("vertex")) {
                        this.vertexCount = Integer.parseInt(parts[2]);
                    } else if (parts[1].equals("face")) {
                        this.faceCount = Integer.parseInt(parts[2]);
                    }
                }
            } while (tokenizedHeader.hasMoreTokens());

            if (this.faceCount < 0 || this.vertexCount < 0) {
                throw new InvalidFileException("PLY file missing vertex or face count.");
            }

        } catch (NoSuchElementException e) {
            throw new InvalidFileException();
        }
    }

    public int getVertexCount() {
        return this.vertexCount;
    }

    public int getFaceCount() {
        return this.faceCount;
    }

    public int getDataOffset() {
        return this.dataOffset;
    }
}
