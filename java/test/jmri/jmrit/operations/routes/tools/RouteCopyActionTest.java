package jmri.jmrit.operations.routes.tools;

import jmri.jmrit.operations.OperationsTestCase;
import jmri.jmrit.operations.routes.tools.RouteCopyAction;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Paul Bender Copyright (C) 2017	
 */
public class RouteCopyActionTest extends OperationsTestCase {

    @Test
    public void testCTor() {
        RouteCopyAction t = new RouteCopyAction("Test");
        Assert.assertNotNull("exists",t);
    }

    // private final static Logger log = LoggerFactory.getLogger(RouteCopyActionTest.class);

}
