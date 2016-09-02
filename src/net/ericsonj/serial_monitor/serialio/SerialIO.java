package net.ericsonj.serial_monitor.serialio;

import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 *
 * @author ericson
 */
public class SerialIO implements Runnable {

    private String port;
    private SerialPort serialPort;
    private final SerialStringBuffer sInput;
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int parity;
    private boolean resetDTR;

    public SerialIO(String port, int baudRate, int dataBits, int stopBits, int parity) throws SerialPortException {
        this.port = port;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.sInput = new SerialStringBuffer();
        initSerialPort();
    }

    private void initSerialPort() throws SerialPortException {
        serialPort = new SerialPort(this.port);
        if (serialPort.isOpened()) {
            System.out.println(">>>> PORT OPEN");
            return;
        }
        serialPort.openPort();
        serialPort.setParams(baudRate, dataBits, stopBits, parity);
        serialPort.setDTR(true);
    }

    public void sendReset(boolean reset) throws SerialPortException {
        resetDTR = reset;
        if (resetDTR) {
            serialPort.setDTR(false);
            serialPort.setDTR(true);
        }
    }

    public int available() {
        return sInput.available();
    }

    public String readString() {
        return new String(readBytes());
    }

    public byte[] readBytes() {
        return sInput.readBytesBuffer();
    }

    public void writeString(String string) throws SerialPortException {
        serialPort.writeString(string);
    }

    public void writeBytes(byte[] buffer) throws SerialPortException {
        serialPort.writeBytes(buffer);
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }

    public void setSerialPort(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
    }

    @Override
    public void run() {

        try {
            serialPort.addEventListener(new SerialPortReader(serialPort, sInput));
        } catch (SerialPortException ex) {
            Logger.getLogger(SerialIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void close() throws SerialPortException {
        serialPort.setDTR(false);
        serialPort.closePort();
    }

    public boolean isOpen() {
        return serialPort.isOpened();
    }

}
