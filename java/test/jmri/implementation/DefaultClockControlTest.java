package jmri.implementation;

import jmri.util.JUnitUtil;

import org.junit.jupiter.api.*;
import org.junit.Assert;
/**
 *
 * @author Paul Bender Copyright (C) 2017
 */
public class DefaultClockControlTest {

    @Test
    public void testCTor() {
        DefaultClockControl t = new DefaultClockControl();
        Assert.assertNotNull("exists",t);
    }

    @Test
    public void testGetSet() {
        DefaultClockControl t = new DefaultClockControl();

        t.setRate(2.0); // doesn't do anything
        Assert.assertEquals(1.0, t.getRate(), 0.01);

        t.setTime(null); // doesn't do anything
        t.getTime();
        
        Assert.assertEquals(0, t.getStatus());

        Assert.assertEquals(null, t.getHardwareClockName());
        
        Assert.assertTrue(! t.canCorrectHardwareClock());
        Assert.assertTrue(! t.canSet12Or24HourClock());
        
    }

    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    public void tearDown() {
        JUnitUtil.tearDown();
    }

    //private final static Logger log = LoggerFactory.getLogger(DefaultClockControlTest.class);

}
