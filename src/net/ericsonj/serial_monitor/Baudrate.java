package net.ericsonj.serial_monitor;

/**
 * Baudrate Object
 * @author ejoseph
 */
public class Baudrate {
    
    private int baudrate;
    
    private String name;

    public Baudrate() {
    }

    public Baudrate(int baudrate, String name) {
        this.baudrate = baudrate;
        this.name = name;
    }

    public int getBaudrate() {
        return baudrate;
    }

    public void setBaudrate(int baudrate) {
        this.baudrate = baudrate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
   
}
