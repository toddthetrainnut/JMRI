package jmri.jmrix.bachrus.speedmatcher.basic;

import jmri.DccThrottle;
import jmri.ProgrammerException;
import jmri.jmrix.bachrus.speedmatcher.SpeedMatcherConfig;

/**
 *
 * @author toddt
 */
public class BasicSpeedTableSpeedMatcher extends BasicSpeedMatcher {
    
    //<editor-fold defaultstate="collapsed" desc="Constants">
    private final int STEP1 = 1;
    private final int STEP28 = 255;
    private final int TRIM = 128;
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Instance Variables">
    private SpeedMatcherState speedMatcherState = SpeedMatcherState.IDLE;
    private ProgrammerState programmerState = ProgrammerState.IDLE;
    //</editor-fold>
    
    public BasicSpeedTableSpeedMatcher(SpeedMatcherConfig config) {
        super (config);
    }

    //<editor-fold defaultstate="collapsed" desc="SpeedMatcher Overrides">
    @Override
    public boolean StartSpeedMatch() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void StopSpeedMatch() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean IsIdle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void CleanUp() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Speed Matcher State">
    private synchronized void speedMatchTimeout() {
        switch(speedMatcherState) {
            //TOOD: speedMatchTimeout
        }
    }
    
    protected enum SpeedMatcherState{
        IDLE {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return this;
            }
        };
            
        protected abstract SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher);  
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Programmer">
    /**
     * Starts writing a CV using the ops mode programmer (Programming on the Main)
     * @param cv    the CV
     * @param value the value to write to the CV (0-255 inclusive)
     */
    private void startOpsModeWrite(String cv, int value) {
        try {
            opsModeProgrammer.writeCV(cv, value, this);
        } catch (ProgrammerException e) {
            logger.error("Exception writing CV " + cv + " " + e);
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="ProgListener Overrides">
    /**
     * Called when the programmer (ops mode or service mode) has completed its
     * operation
     * @param value  Value from a read operation, or value written on a write
     * @param status Denotes the completion code. Note that this is a bitwise
     *               combination of the various states codes defined in this
     *               interface. (see ProgListener.java for possible values)
     */
    @Override
    public void programmingOpReply(int value, int status) {
        if (status == 0) { //OK
            switch (programmerState) {
                case IDLE:
                    logger.debug("unexpected reply in IDLE state");
                    break;
                    
                //TODO: programmingOpReply
                    
                default:
                    programmerState = ProgrammerState.IDLE;
                    logger.warn("Unhandled programmer state: {}", programmerState);
                    break;
            }
        } else {
            // Error during programming
            logger.error("Status not OK during " + programmerState.toString() + ": " + status);
            //profileAddressField.setText("Error");
            statusLabel.setText("Error using programmer");
            programmerState = ProgrammerState.IDLE;
            CleanUp();
        }
    }
    //</editor-fold>
    
    private enum ProgrammerState {
        IDLE
    }
    //</editor-fold>
      
    //<editor-fold defaultstate="collapsed" desc="ThrottleListener Overrides">
    @Override
    public void notifyThrottleFound(DccThrottle t) {
        throw new UnsupportedOperationException("Not supported yet.");
         //To change body of generated methods, choose Tools | Templates.
         
//        super.notifyThrottleFound(t);
//        
//        if (speedMatcherState == SpeedMatcherState.WAIT_FOR_THROTTLE) {
//            logger.info("Starting speed matching");
//
//            // using speed matching timer to trigger each phase of speed matching            
//            setupNextSpeedMatchState(true, 0);
//            speedMatchStateTimer.start();
//        } else {
//            CleanUp();
//        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Helper Functions">
    private void Abort() {
        //TODO:
        //speedMatcherState = SpeedMatcherState.SET_ACCEL;
        //setupNextSpeedMatchState(true, 0);
    }
    
    private void setupNextSpeedMatchState(boolean isForward, int speedStep) {
        //speedMatcherState = speedMatcherState.nextState(this);
        //setupSpeedMatchState(isForward, speedStep, 1500);
    }
    //</editor-fold>
}
