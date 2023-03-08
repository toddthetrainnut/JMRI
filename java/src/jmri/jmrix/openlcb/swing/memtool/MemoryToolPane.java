package jmri.jmrix.openlcb.swing.memtool;

import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import jmri.*;
import jmri.jmrix.can.CanSystemConnectionMemo;
import jmri.swing.JmriJTablePersistenceManager;
import jmri.util.swing.MultiLineCellRenderer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.openlcb.*;
import org.openlcb.implementations.*;
import org.openlcb.swing.*;


/**
 * Pane for doing various memory operations
 * <p>
 *
 * @author Bob Jacobsen Copyright (C) 2023
 * @since 5.3.4
 */
public class MemoryToolPane extends jmri.util.swing.JmriPanel
        implements jmri.jmrix.can.swing.CanPanelInterface {

    protected CanSystemConnectionMemo memo;
    Connection connection;
    NodeID nid;

    MimicNodeStore store;
    MemoryConfigurationService service;
    NodeSelector nodeSelector;

    public String getTitle(String menuTitle) {
        return Bundle.getMessage("TitleMemoryTool");
    }

    static final int CHUNKSIZE = 64;

    JTextField spaceField;
    JLabel statusField;
    boolean cancelled = false;
    boolean running = false;

    @Override
    public void initComponents(CanSystemConnectionMemo memo) {
        this.memo = memo;
        this.connection = memo.get(Connection.class);
        this.nid = memo.get(NodeID.class);

        store = memo.get(MimicNodeStore.class);
        EventTable stdEventTable = memo.get(OlcbInterface.class).getEventTable();
        if (stdEventTable == null) log.warn("no OLCB EventTable found");
        service = memo.get(MemoryConfigurationService.class);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Add to GUI here
        nodeSelector = new org.openlcb.swing.NodeSelector(store, Integer.MAX_VALUE);
        add(nodeSelector);

        var ms = new JPanel();
        ms.setLayout(new FlowLayout());
        add(ms);
        ms.add(new JLabel("Memory Space:"));
        spaceField = new JTextField("255");
        ms.add(spaceField);

        var bb = new JPanel();
        bb.setLayout(new FlowLayout());
        add(bb);
        var gb = new JButton("Get...");
        bb.add(gb);
        gb.addActionListener(this::pushedGetButton);
        var pb = new JButton("Put...");
        bb.add(pb);
        pb.addActionListener(this::pushedPutButton);
        var cb = new JButton("Cancel");
        bb.add(cb);
        cb.addActionListener(this::pushedCancel);

        bb = new JPanel();
        bb.setLayout(new FlowLayout());
        add(bb);
        statusField = new JLabel("                          ",SwingConstants.CENTER);
        bb.add(statusField);

    }

    public MemoryToolPane() {
    }

    @Override
    public void dispose() {
        // and complete this
        super.dispose();
    }

    @Override
    public String getHelpTarget() {
        return "package.jmri.jmrix.openlcb.swing.memtool.MemoryToolPane";
    }

    @Override
    public String getTitle() {
        if (memo != null) {
            return (memo.getUserName() + " Memory Tool");
        }
        return getTitle(Bundle.getMessage("TitleEventTable"));
    }

    void pushedCancel(ActionEvent e) {
        if (running) {
            cancelled = true;
        }
    }

    int space = 0xFF;

    NodeID farID = new NodeID("0.0.0.0.0.0");

    MemoryConfigurationService.McsReadHandler cbr =
        new MemoryConfigurationService.McsReadHandler() {
            @Override
            public void handleFailure(int errorCode) {
                if (errorCode == 0x1082) {
                    statusField.setText("Done reading");
                } if (errorCode == 0x1081) {
                    log.error("Read failed. Address space not known");
                    statusField.setText("Read failed. Address space not known");
                } else {
                    log.error("Read failed. Error code is {}", String.format("%04X", errorCode));
                    statusField.setText("Read failed. Error code is "+String.format("%04X", errorCode));
                }
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException ex) {
                    log.warn("Error closing file", ex);
                    statusField.setText("Error closing output file");
                }
                running = false;
            }

            @Override
            public void handleReadData(NodeID dest, int readSpace, long readAddress, byte[] readData) {
                log.trace("read succeed with {} bytes at {}", readData.length, readAddress);
                statusField.setText("Read "+readAddress+" bytes");
                try {
                    outputStream.write(readData);
                } catch (IOException ex) {
                    log.error("Error writing data to file", ex);
                    statusField.setText("Error writing data to file");
                    return; // stop now
                }
                if (readData.length != CHUNKSIZE) {
                    // short read is another way to indicate end
                    statusField.setText("Done reading");
                    log.warn("Stopping read due to short reply");
                    try {
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException ex) {
                        log.warn("Error closing file", ex);
                        statusField.setText("Error closing output file");
                    }
                    return;
                }
                // fire another
                if (!cancelled) {
                    service.requestRead(farID, space, readAddress+readData.length, CHUNKSIZE, cbr);
                } else {
                    running = false;
                    cancelled = false;
                    log.info("Get operation cancelled");
                    statusField.setText("Cancelled");
                }
            }
        };

    OutputStream outputStream;

    /**
     * Starts reading from node and writing to file
     * @param e not used
     */
    void pushedGetButton(ActionEvent e) {
        running = true;
        farID = nodeSelector.getSelectedItem();
        try {
            space = Integer.parseInt(spaceField.getText().trim());
        } catch (NumberFormatException ex) {
            log.error("error parsing the space field value \"{}\"", spaceField.getText());
            statusField.setText("Error parsing the space value");
            running = false;
            return;
        }
        log.debug("Start put");
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
        }
        fileChooser.setDialogTitle("Read into binary file");
        fileChooser.rescanCurrentDirectory();
        fileChooser.setSelectedFile(new File("memory.bin"));

        int retVal = fileChooser.showSaveDialog(this);
        if (retVal != JFileChooser.APPROVE_OPTION) {
            running = false;
            return;
        }

        // open file
        File file = fileChooser.getSelectedFile();
        log.debug("access {}", file);
        try {
            outputStream = new FileOutputStream(file);
        } catch (IOException ex) {
            log.error("Error opening file", ex);
            statusField.setText("Error opening file");
            running = false;
            return;
        }

        // do first memory read
        int address = 0;
        service.requestRead(farID, space, address, CHUNKSIZE, cbr);
    }

    MemoryConfigurationService.McsWriteHandler cb =
        new MemoryConfigurationService.McsWriteHandler() {
            @Override
            public void handleFailure(int errorCode) {
                if (errorCode == 0x1081) {
                    log.error("Write failed. Address space not known");
                    statusField.setText("Write failed. Address space not known.");
                } else if (errorCode == 0x1083) {
                    log.error("Write failed. Address space not writable");
                    statusField.setText("Write failed. Address space not writeable.");
                } else {
                    log.error("Write failed. error code is {}", String.format("%016X", errorCode));
                    statusField.setText("Write failed. error code is "+String.format("%016X", errorCode));
                }
                running = false;
                // return because we're done.
            }

            @Override
            public void handleSuccess() {
                log.info("Write succeeded {} bytes", address+bytesRead);

                if (cancelled) {
                    log.info("Cancelled");
                    statusField.setText("Cancelled");
                    running = false;
                    cancelled = false;
                }
                // next operation
                address = address+bytesRead;

                byte[] dataRead = new byte[0];
                try {
                    dataRead = getBytes();
                    if (dataRead == null) {
                        // end of read present
                        running = false;
                        log.info("Completed");
                        statusField.setText("Completed.");
                        inputStream.close();
                        return;
                    }
                    bytesRead = dataRead.length;
                    log.info("write {} bytes", bytesRead);
                } catch (IOException ex) {
                    log.error("Error reading file",ex);
                    return;
                }
                service.requestWrite(farID, space, address, dataRead, cb);
            }
        };

    void pushedPutButton(ActionEvent e) {
        log.debug("Start get");
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
        }
        fileChooser.setDialogTitle("Upload binary file");
        fileChooser.rescanCurrentDirectory();
        fileChooser.setSelectedFile(new File("memory.bin"));

        int retVal = fileChooser.showOpenDialog(this);
        if (retVal != JFileChooser.APPROVE_OPTION) { return; }

        // open file and read first 64 bytes
        File file = fileChooser.getSelectedFile();
        log.debug("access {}", file);

        byte[] dataRead = new byte[0];
        try {
            inputStream = new FileInputStream(file);
            dataRead = getBytes();
            if (dataRead == null) {
                // end of read present
                log.info("Completed");
                inputStream.close();
                return;
            }
            bytesRead = dataRead.length;
            log.info("read {} bytes", bytesRead);
        } catch (IOException ex) {
            log.error("Error reading file",ex);
            return;
        }

        // do first memory write
        address = 0;
        running = true;
        service.requestWrite(farID, space, address, dataRead, cb);
    }

    byte[] bytes = new byte[CHUNKSIZE];
    int bytesRead;
    InputStream inputStream;
    int address;

    byte[] getBytes() throws IOException {
        int bytesRead = inputStream.read(bytes);
        if (bytesRead == -1) return null;  // file done
        if (bytesRead == CHUNKSIZE) return bytes;
        // less data received, have to adjust size of return array
        return Arrays.copyOf(bytes, bytesRead);
    }

    // static to remember choice from one use to another.
    static JFileChooser fileChooser = null;

    /**
     * Nested class to create one of these using old-style defaults
     */
    public static class Default extends jmri.jmrix.can.swing.CanNamedPaneAction {

        public Default() {
            super("Openlcb Memory Tool",
                    new jmri.util.swing.sdi.JmriJFrameInterface(),
                    MemoryToolPane.class.getName(),
                    jmri.InstanceManager.getDefault(jmri.jmrix.can.CanSystemConnectionMemo.class));
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MemoryToolPane.class);
}
