package net.ericsonj.serial_monitor;

import java.util.LinkedList;

/**
 *
 * @author ejoseph
 */
public class BaudrateList {

    private final LinkedList<Baudrate> baudrateList;

    public BaudrateList() {
        baudrateList = new LinkedList<Baudrate>();
        loadList();
    }

    private void loadList() {
        baudrateList.add(new Baudrate(300, "300 baud"));
        baudrateList.add(new Baudrate(1200, "1200 baud"));
        baudrateList.add(new Baudrate(2400, "2400 baud"));
        baudrateList.add(new Baudrate(4800, "4800 baud"));
        baudrateList.add(new Baudrate(9600, "9600 baud"));
        baudrateList.add(new Baudrate(19200, "19200 baud"));
        baudrateList.add(new Baudrate(38400, "38400 baud"));
        baudrateList.add(new Baudrate(57600, "57600 baud"));
        baudrateList.add(new Baudrate(115200, "115200 baud"));
    }

    public LinkedList<Baudrate> getBaudrateList() {
        return baudrateList;
    }

    public Baudrate findBaudrateByValue(int value) {
        for (Baudrate baudrate : baudrateList) {
            if (baudrate.getBaudrate() == value) {
                return baudrate;
            }
        }
        return null;
    }

}
