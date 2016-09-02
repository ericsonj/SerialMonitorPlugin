package net.ericsonj.serial_monitor;

import javax.swing.JComboBox;
import jssc.SerialPortList;

/**
 *
 * @author ejoseph
 */
public class PortsListen implements Runnable {

    private JComboBox jComboBox;
    private static String liststring;
    private volatile boolean running = true;

    public PortsListen(JComboBox JComboBox) {
        this.jComboBox = JComboBox;
        this.liststring = new String();
    }

    public static String stringArrayToString(String[] a) {
        StringBuilder resp = new StringBuilder();
        for (String a1 : a) {
            resp.append(a1);
        }
        return resp.toString();
    }

    public void terminate() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                String[] a = SerialPortList.getPortNames();
                if (a != null && a.length > 0) {
                    String aux = stringArrayToString(a);
                    System.out.println("buscando " + aux);
                    if (!aux.equals(liststring)) {
                        System.out.println("?? hay un cambio");
                        liststring = aux;
                        jComboBox.removeAllItems();
//                        for (String item : a) {
//                            jComboBox.addItem(item);
//                        }
                        for (int i = (a.length - 1); i >= 0; i--) {
                            jComboBox.addItem(a[i]);
                        }
                    }
                } else {
                    if (!liststring.equals("")) {
                        System.out.println("?? no hay nada");
                        liststring = "";
                        jComboBox.removeAllItems();
                    }
                }
                Thread.sleep(5000);
            } catch (NullPointerException | InterruptedException ex) {
            }

        }
    }

}
