package net.ericsonj.serial_monitor;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.text.DefaultCaret;
import javax.xml.bind.DatatypeConverter;
import jssc.SerialPortException;
import jssc.SerialPortList;
import net.ericsonj.serial_monitor.serialio.SerialIO;
import net.ericsonj.serial_monitor.serialio.ViewSerialRead;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//net.ericsonj.serial_monitor//SerialMonitor//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "SerialMonitorTopComponent",
        iconBase = "net/ericsonj/serial_monitor/port-icon.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "net.ericsonj.serial_monitor.SerialMonitorTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SerialMonitorAction",
        preferredID = "SerialMonitorTopComponent"
)
@Messages({
    "CTL_SerialMonitorAction=Serial Monitor",
    "CTL_SerialMonitorTopComponent=Serial Monitor Window",
    "HINT_SerialMonitorTopComponent=This is a SerialMonitor window"
})
public final class SerialMonitorTopComponent extends TopComponent {

    private static final Logger log = Logger.getLogger("SerialMonitor");

    private final String ASCII = "ASCII";

    private final String HEX = "HEX";

    private final String[] outputTypes = {ASCII, HEX};

    private final String CONNECT = "Connect";

    private final String DISCONNECT = "Disconnect";

    private final String COMP_NAME = "SerialMonitor";

    private final int SIZE_BUFFER = 100;

    private static final byte[] CRLF = {(byte) 0x0d, (byte) 0x0a};
    private static final byte[] CR = {(byte) 0x0d};
    private static final byte[] LF = {(byte) 0x0a};
    private static final byte[] ESC = {(byte) 0x1b};
    private static final byte[] TAB = {(byte) 0x09};

    public enum EndType {

        CRLFend("CR+LF end", CRLF),
        CRend("CR end", CR),
        LFend("LF end", LF),
        TABend("TAB end", TAB),
        ESCend("ESC end", ESC),
        NOend("NO end", new byte[0]);

        private final String name;
        private final byte[] value;

        private EndType(String name, byte[] value) {
            this.value = value;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    private boolean isConnect = false;

    private Thread th;

    private SerialIO serial;

    private ViewSerialRead view;

    private String device = "";

    private Baudrate baudrate;

    private StackBuffer buffer;

    private boolean JCBDeviceEventDisable = false;

    public SerialMonitorTopComponent() {
        initComponents();
        initComponentValue();
        setName(Bundle.CTL_SerialMonitorTopComponent());
        setToolTipText(Bundle.HINT_SerialMonitorTopComponent());

    }

    public void initComponentValue() {

        jLabelInfo.setText("");

        BaudrateList listBaud = new BaudrateList();
        LinkedList<Baudrate> list = listBaud.getBaudrateList();
        for (Baudrate baud : list) {
            jComboBoxBaudrate.addItem(baud);
        }
        jComboBoxBaudrate.setSelectedItem(listBaud.findBaudrateByValue(9600));

        for (String sType : outputTypes) {
            jComboBoxOutputType.addItem(sType);
        }

        for (EndType p : EndType.values()) {
            jComboBoxEnd.addItem(p);
        }

        jButtonConnect.setText(CONNECT);

        DefaultCaret caret = (DefaultCaret) jTextAreaASCII.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        caret = (DefaultCaret) jTextAreaHEX.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        jTextAreaASCII.setFont(new Font("Monospaced", Font.PLAIN, 14));
        jTextAreaHEX.setFont(new Font("Monospaced", Font.PLAIN, 14));
        jTextAreaASCII.setEditable(false);
        jTextAreaHEX.setEditable(false);

        jTextFieldSend.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String command = jTextFieldSend.getText();
                    if (command != null && !command.isEmpty()) {
                        sendMessage(command);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    jTextFieldSend.setText(buffer.getDOWN());
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    jTextFieldSend.setText(buffer.getUP());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        jTextFieldSend.requestFocusInWindow();
        jTextFieldSend.setEnabled(false);
        jButtonSend.setEnabled(false);

        this.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
                jTextFieldSend.requestFocusInWindow();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        baudrate = new Baudrate();
        jComboBoxBaudrate.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (isConnect) {
                        baudrate = (Baudrate) e.getItem();
                        disconnectDevice();
                        connectDevice();
                    }
                }
            }
        });

        jComboBoxDevice.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (!JCBDeviceEventDisable) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (isConnect) {
                            device = e.getItem().toString();
                            disconnectDevice();
                            connectDevice();
                        }
                    }
                }
            }
        });

        jComboBoxDevice.setToolTipText("Port Device");
        jComboBoxBaudrate.setToolTipText("Baud Rate");
        jButtonRefresh.setToolTipText("Refresh Device");
        jButtonConnect.setToolTipText("Open/close port");
        jButtonSend.setToolTipText("Send data now");
        jComboBoxEnd.setToolTipText("End data");
        jComboBoxOutputType.setToolTipText("Type data outgoing");

        device = new String();

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabelPort = new javax.swing.JLabel();
        jComboBoxDevice = new javax.swing.JComboBox();
        jComboBoxBaudrate = new javax.swing.JComboBox();
        jTextFieldSend = new javax.swing.JTextField();
        jButtonConnect = new javax.swing.JButton();
        jComboBoxOutputType = new javax.swing.JComboBox();
        jComboBoxEnd = new javax.swing.JComboBox();
        jButtonSend = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaASCII = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaHEX = new javax.swing.JTextArea();
        jButtonRefresh = new javax.swing.JButton();
        jLabelInfo = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(jLabelPort, org.openide.util.NbBundle.getMessage(SerialMonitorTopComponent.class, "SerialMonitorTopComponent.jLabelPort.text")); // NOI18N

        jTextFieldSend.setBackground(new java.awt.Color(254, 254, 254));
        jTextFieldSend.setText(org.openide.util.NbBundle.getMessage(SerialMonitorTopComponent.class, "SerialMonitorTopComponent.jTextFieldSend.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonConnect, org.openide.util.NbBundle.getMessage(SerialMonitorTopComponent.class, "SerialMonitorTopComponent.jButtonConnect.text")); // NOI18N
        jButtonConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConnectActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jButtonSend, org.openide.util.NbBundle.getMessage(SerialMonitorTopComponent.class, "SerialMonitorTopComponent.jButtonSend.text")); // NOI18N
        jButtonSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSendActionPerformed(evt);
            }
        });

        jTextAreaASCII.setColumns(20);
        jTextAreaASCII.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        jTextAreaASCII.setRows(5);
        jScrollPane1.setViewportView(jTextAreaASCII);

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(SerialMonitorTopComponent.class, "SerialMonitorTopComponent.jScrollPane1.TabConstraints.tabTitle"), jScrollPane1); // NOI18N

        jTextAreaHEX.setColumns(20);
        jTextAreaHEX.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        jTextAreaHEX.setRows(5);
        jScrollPane2.setViewportView(jTextAreaHEX);

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(SerialMonitorTopComponent.class, "SerialMonitorTopComponent.jScrollPane2.TabConstraints.tabTitle"), jScrollPane2); // NOI18N

        jButtonRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/ericsonj/serial_monitor/refresh_1.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButtonRefresh, org.openide.util.NbBundle.getMessage(SerialMonitorTopComponent.class, "SerialMonitorTopComponent.jButtonRefresh.text")); // NOI18N
        jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRefreshActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabelInfo, org.openide.util.NbBundle.getMessage(SerialMonitorTopComponent.class, "SerialMonitorTopComponent.jLabelInfo.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(jLabelPort)
                        .addGap(6, 6, 6)
                        .addComponent(jComboBoxDevice, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxBaudrate, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTextFieldSend, javax.swing.GroupLayout.PREFERRED_SIZE, 382, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonSend, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonConnect, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabelInfo)
                        .addContainerGap(398, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jComboBoxOutputType, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxEnd, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelPort)
                    .addComponent(jComboBoxDevice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBoxBaudrate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonConnect)
                    .addComponent(jButtonRefresh)
                    .addComponent(jLabelInfo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldSend, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBoxOutputType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBoxEnd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSend))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonConnectActionPerformed
        // TODO add your handling code here:
        connectButton();
    }//GEN-LAST:event_jButtonConnectActionPerformed

    private void jButtonSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSendActionPerformed
        String data = jTextFieldSend.getText();
        if (data != null && !data.isEmpty()) {
            sendMessage(data);
        }
    }//GEN-LAST:event_jButtonSendActionPerformed

    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        JCBDeviceEventDisable = true;
        findPortSerial();
        JCBDeviceEventDisable = false;
    }//GEN-LAST:event_jButtonRefreshActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonConnect;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonSend;
    private javax.swing.JComboBox jComboBoxBaudrate;
    private javax.swing.JComboBox jComboBoxDevice;
    private javax.swing.JComboBox jComboBoxEnd;
    private javax.swing.JComboBox jComboBoxOutputType;
    private javax.swing.JLabel jLabelInfo;
    private javax.swing.JLabel jLabelPort;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextAreaASCII;
    private javax.swing.JTextArea jTextAreaHEX;
    private javax.swing.JTextField jTextFieldSend;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        this.setName(COMP_NAME);
        jButtonConnect.setText(CONNECT);
        jTextFieldSend.setEnabled(false);
        jButtonSend.setEnabled(false);
        buffer = new StackBuffer(SIZE_BUFFER);
        findPortSerial();

    }

    private void connectButton() {
        if (jButtonConnect.getText().equals(CONNECT)) {
            connectDevice();
        } else if (jButtonConnect.getText().equals(DISCONNECT)) {
            disconnectDevice();
        }
    }

    public void connectDevice() {
        jTextAreaASCII.setText("");
        jTextAreaHEX.setText("");
        try {
            device = jComboBoxDevice.getSelectedItem().toString();
            initConnection(device);
            this.setName(COMP_NAME + " [Connected]");
            jButtonConnect.setText(DISCONNECT);
            isConnect = true;
            setLabelInfo();
            jTextFieldSend.setEnabled(true);
            jTextFieldSend.requestFocus();
            jButtonSend.setEnabled(true);
        } catch (SerialPortException | InterruptedException ex) {
            log.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Serial Monitor Message", JOptionPane.WARNING_MESSAGE);
        } catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Serial Monitor Message", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void disconnectDevice() {
        if (th.isAlive()) {
            try {
                view.terminate();
                serial.close();
                isConnect = false;
                setLabelInfo();
                jButtonConnect.setText(CONNECT);
                this.setName(COMP_NAME);
                jTextFieldSend.setEnabled(false);
                jButtonSend.setEnabled(false);
            } catch (SerialPortException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public void componentClosed() {
        closeWindows();
        isConnect = false;
        setLabelInfo();
    }

    private void closeWindows() {
        if (th != null && th.isAlive()) {
            try {
                if (serial != null) {
                    serial.close();
                }
                if (view != null) {
                    view.terminate();
                }
            } catch (SerialPortException ex) {
                log.warning(ex.getMessage());
            }
        }
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    private void initConnection(String port) throws SerialPortException, InterruptedException {
        baudrate = (Baudrate) jComboBoxBaudrate.getSelectedItem();
        serial = new SerialIO(port, baudrate.getBaudrate(), 8, 1, 0);
        serial.sendReset(false);
        th = new Thread(serial);
        th.start();
        Thread.sleep(10);
        view = new ViewSerialRead(serial, jTextAreaASCII, jTextAreaHEX);
        th = new Thread(view);
        th.start();
    }

    private void sendMessage(String data) {
        buffer.push(data);
        String sData = data;
        EndType selectEnd = (EndType) jComboBoxEnd.getSelectedItem();
        String outType = jComboBoxOutputType.getSelectedItem().toString();
        try {
            switch (outType) {
                case ASCII:
                    serial.writeBytes(sData.getBytes());
                    break;
                case HEX:
                    sData = sData.replace("0x", "");
                    if ((sData.length() % 2) == 0) {
                        byte[] a = DatatypeConverter.parseHexBinary(sData);
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "You have to insert groups of byte ex: 68656c6c6f or 0x68656c6c6f",
                                "Serial Monitor Message", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    break;
            }
            serial.writeBytes(selectEnd.value);
            jTextFieldSend.setText("");
        } catch (SerialPortException ex) {
            log.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null, "You have to connect port", "Serial Monitor Message", JOptionPane.INFORMATION_MESSAGE);
        } catch (NullPointerException ex) {
            log.info("You have to connect port, " + ex.toString());
            JOptionPane.showMessageDialog(null, "You have to connect port", "Serial Monitor Message", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Serial Monitor Message", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void findPortSerial() {

        String olditem = "";
        int index = 0;
        Object aux = jComboBoxDevice.getSelectedItem();
        if (aux != null) {
            olditem = aux.toString();
        }
        String[] a = SerialPortList.getPortNames();
        if (a != null && a.length > 0) {
            jComboBoxDevice.removeAllItems();
            for (int i = (a.length - 1); i >= 0; i--) {
                if (olditem.equals(a[i])) {
                    jComboBoxDevice.addItem(a[i]);
                    index = (a.length - 1) - i;
                } else {
                    jComboBoxDevice.addItem(a[i]);
                }
            }
        } else {
            jComboBoxDevice.removeAllItems();
        }
        jComboBoxDevice.setSelectedIndex(index);
    }

    private void setLabelInfo() {
        if (isConnect) {
            jLabelInfo.setForeground(Color.BLACK);
            jLabelInfo.setText("OPEN " + device + " " + baudrate.getBaudrate() + ",8N1");
        } else {
            jLabelInfo.setForeground(Color.GRAY);
            jLabelInfo.setText("CLOSE " + device + " " + baudrate.getBaudrate() + ",8N1");
        }
    }

}
