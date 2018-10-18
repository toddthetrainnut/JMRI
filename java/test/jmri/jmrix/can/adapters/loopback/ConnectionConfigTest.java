package jmri.jmrix.can.adapters.loopback;

import jmri.util.JUnitUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for ConnectionConfig class.
 *
 * @author Paul Bender Copyright (C) 2016
 **/

public class ConnectionConfigTest extends jmri.jmrix.AbstractSerialConnectionConfigTestBase  {

   @Test
   public void ConstructorTest(){
      Assert.assertNotNull("ConnectionConfig constructor",new ConnectionConfig());
   }

   @Before
   public void setUp() {
        JUnitUtil.setUp();

        JUnitUtil.initDefaultUserMessagePreferences();
        cc = new ConnectionConfig();
   }

   @After
   public void tearDown(){
        JUnitUtil.tearDown();
   }

}
