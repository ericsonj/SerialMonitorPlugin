package net.ericsonj.serial_monitor.serialio;

/**
 * Serial String Buffer for read and write Multithreading
 * @author ejoseph
 */
public class SerialStringBuffer {

    private StringBuffer buffer;
    private int numBytes;

    public SerialStringBuffer() {
        this.buffer = new StringBuffer();
        this.numBytes = 0;
    }

    public synchronized void writeStringBuffer(byte[] byteBuffer) {
        numBytes = byteBuffer.length;
        buffer.append(new String(byteBuffer));
    }

    @Deprecated
    public synchronized StringBuffer readStringBuffer() {
        StringBuffer a = buffer;
        numBytes = 0;
        buffer = new StringBuffer();
        return a;
    }

    public synchronized byte[] readBytesBuffer() {
        numBytes = 0;
        byte[] resp = buffer.toString().getBytes();
        buffer = new StringBuffer();
        return resp;
    }

    public synchronized int available() {
        return numBytes;
    }

}
