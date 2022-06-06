package jmri.jmrix.can.cbus.swing.simulator;

import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import jmri.jmrix.can.CanSystemConnectionMemo;
import jmri.jmrix.can.TrafficControllerScaffold;
import jmri.util.JmriJFrame;
import jmri.util.JUnitUtil;
import jmri.util.ThreadingUtil;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import org.netbeans.jemmy.operators.*;

/**
 * Test simple functioning of CBUS SimulatorPane.
 *
 * @author Paul Bender Copyright (C) 2016
 * @author Steve Young Copyright (C) 2019
 */
public class SimulatorPaneTest extends jmri.util.swing.JmriPanelTest {

    private CanSystemConnectionMemo memo; 
    private TrafficControllerScaffold tcis;
 
    @Override
    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
        title = Bundle.getMessage("MenuItemNetworkSim");
        helpTarget = "package.jmri.jmrix.can.cbus.swing.simulator.SimulatorPane";
        memo = new CanSystemConnectionMemo();
        tcis = new TrafficControllerScaffold();
        memo.setTrafficController(tcis);
        
        panel = new SimulatorPane();
    }
    
    @Override
    @AfterEach
    public void tearDown() {
        tcis.terminateThreads();
        memo.dispose();
        tcis = null;
        memo = null;
        super.tearDown();
    }

    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    @Test
    public void testInitComp() {
        
        ((SimulatorPane)panel).initComponents(memo);
        
        Assertions.assertNotNull(panel, "exists");
        Assertions.assertEquals("CAN " + Bundle.getMessage("MenuItemNetworkSim"), panel.getTitle(), "name with memo");
        
        // check pane has loaded something
        JmriJFrame f = new JmriJFrame();
        f.add(panel);
        f.setTitle(panel.getTitle());
        
        List<JMenu> list = panel.getMenus();
        JMenuBar bar = f.getJMenuBar();
        if (bar == null) {
            bar = new JMenuBar();
        }
        for (JMenu menu : list) {
            bar.add(menu);
        }
        f.setJMenuBar(bar);
        
        f.pack();
        
        ThreadingUtil.runOnGUI(() -> {
            f.setVisible(true);
        });
        // Find new window by name
        JFrameOperator jfo = new JFrameOperator( panel.getTitle() );
        
        Assertions.assertTrue(getResetCsButtonEnabled(jfo));

        // Ask to close window
        jfo.requestClose();
        jfo.waitClosed();
 
    }
    
    private boolean getResetCsButtonEnabled( JFrameOperator jfo ){
        return ( new JButtonOperator(jfo, Bundle.getMessage("Reset")).isEnabled() );
    }

}
