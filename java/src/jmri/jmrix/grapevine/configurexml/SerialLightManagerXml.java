package jmri.jmrix.grapevine.configurexml;

import org.jdom2.Element;

/**
 * Provide load and store functionality for configuring SerialLightManagers.
 * <p>
 * Uses the store method from the abstract base class, but provides a load
 * method here.
 * <p>
 * Based on SerialTurnoutManagerXml.java
 *
 * @author Dave Duchamp Copyright (c) 2004, 2007
 */
public class SerialLightManagerXml extends jmri.managers.configurexml.AbstractLightManagerConfigXML {

    public SerialLightManagerXml() {
        super();
    }

    @Override
    public void setStoreElementClass(Element lights) {
        lights.setAttribute("class", "jmri.jmrix.grapevine.configurexml.SerialLightManagerXml");
    }

    @Override
    public boolean load(Element shared, Element perNode) {
        // load individual lights
        return loadLights(shared);
    }

//    private final static Logger log = LoggerFactory.getLogger(SerialLightManagerXml.class);

}
