package net.ericsonj.serial_monitor.serialio;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

/**
 *
 * @author ejoseph
 */
public class ViewSerialRead implements Runnable {

    private static final int BYTES_FOR_LINE = 16;
    private static final int BYTES_FOR_GROUP = 8;
    private static final String GROUP_SPLIT = "   ";
    private static final String BYTE_SPLIT = " ";

    private final SerialIO serial;
    private byte[] lineBuffer;
    private StringBuffer line;
    private JTextArea jTextAreaASCII;
    private JTextArea jTextAreaHEX;
    private int count;
    private int memdir;
    private volatile boolean running = true;

    public ViewSerialRead(SerialIO serial, JTextArea jTextAreaASCII, JTextArea jTextAreaHEX) {
        this.serial = serial;
        this.lineBuffer = new byte[BYTES_FOR_LINE];
        this.line = new StringBuffer();
        this.count = 0;
        this.memdir = 0;
        this.jTextAreaASCII = jTextAreaASCII;
        this.jTextAreaHEX = jTextAreaHEX;
    }

    public void terminate() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            if (serial.available() > 0) {
                byte[] buffer = serial.readBytes();
                try {
                    writeJTextBoxASCII(new String(buffer, "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(ViewSerialRead.class.getName()).log(Level.SEVERE, null, ex);
                }
                printHex(buffer, line);
            }
        }
    }

    public byte filterByte(byte src) {
        if (src <= 0x1F) {
            return 0x2e;
        } else {
            return src;
        }
    }

    public void printHex(byte[] buffer, StringBuffer line) {
        for (byte b : buffer) {
            if (count == 0) {
                line.delete(0, line.length());
                writeJTextBoxHEX(String.format("%08X", memdir));
                writeJTextBoxHEX("\t");
            }
            line.append((char) filterByte(b));
            writeJTextBoxHEX(String.format("%02X", b));
            writeJTextBoxHEX(BYTE_SPLIT);
            count++;
            if ((count % BYTES_FOR_GROUP) == 0) {
                writeJTextBoxHEX(GROUP_SPLIT);
            }

            if (count == BYTES_FOR_LINE) {
                writeJTextBoxHEX("\t");
                writeJTextBoxHEX(line.toString());
                memdir += BYTES_FOR_LINE;
                writeJTextBoxHEX("\n");
                count = 0;
            }

        }
    }

    public void writeJTextBoxASCII(String string) {
        this.jTextAreaASCII.append(string);
    }

    public void writeJTextBoxHEX(String string) {
        this.jTextAreaHEX.append(string);
    }

}
