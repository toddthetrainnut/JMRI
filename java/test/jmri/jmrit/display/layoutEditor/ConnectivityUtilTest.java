package jmri.jmrit.display.layoutEditor;

import java.awt.GraphicsEnvironment;

import jmri.util.JUnitUtil;

import org.junit.jupiter.api.*;
import org.junit.Assert;
import org.junit.Assume;

/**
 * Test simple functioning of ConnectivityUtil
 *
 * @author Paul Bender Copyright (C) 2016
 */
public class ConnectivityUtilTest {

    @Test
    public void testCtor() {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        LayoutEditor e = new LayoutEditor();
        ConnectivityUtil t = new ConnectivityUtil(e);
        Assert.assertNotNull("exists", t);
        JUnitUtil.dispose(e);
    }

    // from here down is testing infrastructure
    @BeforeEach
    public void setUp() throws Exception {
        JUnitUtil.setUp();
        jmri.util.JUnitUtil.resetProfileManager();
    }

    @AfterEach
    public void tearDown() throws Exception {
        JUnitUtil.deregisterBlockManagerShutdownTask();
        JUnitUtil.deregisterEditorManagerShutdownTask();
        JUnitUtil.tearDown();
    }
}
