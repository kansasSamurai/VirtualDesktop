package org.jwellman.console.ui;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * A PipedInputStream that doesn't throw "Broken pipe" exceptions.
 *
 * <p>This stream will wait for new writers and data rather than throwing
 * exceptions when the pipe appears broken. This is useful for console
 * implementations where writers may come and go in different threads.</p>
 *
 * <p>Based on the original implementation in BeanShell's JConsole.</p>
 *
 * @author Rick Wellman
 */
public class BlockingPipedInputStream extends PipedInputStream {

    private boolean closed = false;

    /**
     * Create a blocking piped input stream connected to the given output stream.
     *
     * @param pout the output stream to connect to
     * @throws IOException if connection fails
     */
    public BlockingPipedInputStream(PipedOutputStream pout) throws IOException {
        super(pout);
    }

    /**
     * Create a blocking piped input stream with specified buffer size.
     *
     * @param pout the output stream to connect to
     * @param pipeSize the size of the pipe buffer
     * @throws IOException if connection fails
     */
    public BlockingPipedInputStream(PipedOutputStream pout, int pipeSize) throws IOException {
        super(pout, pipeSize);
    }

    /**
     * Read a single byte, blocking until data is available.
     *
     * <p>Unlike the standard implementation, this will not throw
     * "Broken pipe" exceptions. Instead, it waits for new data
     * to become available.</p>
     *
     * @return the byte read, or -1 if closed
     * @throws IOException if the stream is closed or interrupted
     */
    @Override
    public synchronized int read() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }

        // Wait for data to become available
        while (super.in < 0) {
            notifyAll();  // Wake up any writers
            try {
                wait(750);  // Check periodically
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        // Read the byte (same as superclass implementation)
        int ret = buffer[super.out++] & 0xFF;
        if (super.out >= buffer.length) {
            super.out = 0;
        }
        if (super.in == super.out) {
            super.in = -1;  // Buffer now empty
        }
        return ret;
    }

    /**
     * Close the stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }

    /**
     * Check if the stream has been closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

}
