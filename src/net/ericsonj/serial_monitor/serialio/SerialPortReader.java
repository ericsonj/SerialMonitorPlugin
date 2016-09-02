package net.ericsonj.serial_monitor.serialio;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 *
 * @author ericson
 */
public class SerialPortReader implements SerialPortEventListener {

    SerialPort serialPort;
    SerialStringBuffer stringBuffer;

    public SerialPortReader(SerialPort serialPort, SerialStringBuffer sBuilder) {
        this.serialPort = serialPort;
        this.stringBuffer = sBuilder;
    }

    @Override
    public void serialEvent(SerialPortEvent spe) {
        if (spe.isRXCHAR()) {
            try {
//                byte[] b = serialPort.readBytes();
//                System.out.println("--- "+b.length);
//                String s = new String(b);
//                System.out.println(">>>" + s);
                stringBuffer.writeStringBuffer(serialPort.readBytes());

            } catch (SerialPortException ex) {
                
            }
        }
    }

}
