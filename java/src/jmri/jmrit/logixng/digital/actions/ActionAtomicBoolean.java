package jmri.jmrit.logixng.digital.actions;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import jmri.InstanceManager;
import jmri.jmrit.logixng.Base;
import jmri.jmrit.logixng.Category;
import jmri.jmrit.logixng.DigitalActionManager;
import jmri.jmrit.logixng.FemaleSocket;

/**
 * This action sets the value of an AtomicBoolean. It is mostly used for tests.
 * 
 * @author Daniel Bergqvist Copyright 2018
 */
public class ActionAtomicBoolean extends AbstractDigitalAction {

    private ActionAtomicBoolean _template;
    private AtomicBoolean _atomicBoolean;
    private boolean _newValue;
    
    public ActionAtomicBoolean(AtomicBoolean atomicBoolean, boolean newValue)
            throws BadUserNameException {
        super(InstanceManager.getDefault(DigitalActionManager.class).getNewSystemName());
        _atomicBoolean = atomicBoolean;
        _newValue = newValue;
    }

    public ActionAtomicBoolean(String sys)
            throws BadUserNameException {
        super(sys);
//        jmri.InstanceManager.turnoutManagerInstance().addVetoableChangeListener(this);
    }

    public ActionAtomicBoolean(String sys, AtomicBoolean atomicBoolean, boolean newValue)
            throws BadUserNameException {
        super(sys);
        _atomicBoolean = atomicBoolean;
        _newValue = newValue;
    }

    public ActionAtomicBoolean(String sys, String user)
            throws BadUserNameException, BadSystemNameException {
        super(sys, user);
    }
    
    public ActionAtomicBoolean(String sys, String user, AtomicBoolean atomicBoolean, boolean newValue)
            throws BadUserNameException, BadSystemNameException {
        super(sys, user);
        _atomicBoolean = atomicBoolean;
        _newValue = newValue;
    }
    
    private ActionAtomicBoolean(ActionAtomicBoolean template, String sys) {
        super(sys);
        _template = template;
        if (_template == null) throw new NullPointerException();    // Temporary solution to make variable used.
    }
    
    /** {@inheritDoc} */
    @Override
    public Base getNewObjectBasedOnTemplate(String sys) {
        return new ActionAtomicBoolean(this, sys);
    }
    
    public void setAtomicBoolean(AtomicBoolean atomicBoolean) {
        _atomicBoolean = atomicBoolean;
    }
    
    public AtomicBoolean getAtomicBoolean() {
        return _atomicBoolean;
    }
    
    public void setNewValue(boolean newValue) {
        _newValue = newValue;
    }
    
    public boolean getNewValue() {
        return _newValue;
    }
    
    /** {@inheritDoc} */
    @Override
    public Category getCategory() {
        return Category.OTHER;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExternal() {
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public void execute() {
        _atomicBoolean.set(_newValue);
    }

    @Override
    public FemaleSocket getChild(int index) throws IllegalArgumentException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String getShortDescription(Locale locale) {
        return Bundle.getMessage(locale, "ActionAtomicBoolean_Short");
    }

    @Override
    public String getLongDescription(Locale locale) {
        return Bundle.getMessage(locale, "ActionAtomicBoolean_Long");
    }
    
    /** {@inheritDoc} */
    @Override
    public void setup() {
    }
    
    /** {@inheritDoc} */
    @Override
    public void registerListenersForThisClass() {
    }
    
    /** {@inheritDoc} */
    @Override
    public void unregisterListenersForThisClass() {
    }
    
    /** {@inheritDoc} */
    @Override
    public void disposeMe() {
    }

}
