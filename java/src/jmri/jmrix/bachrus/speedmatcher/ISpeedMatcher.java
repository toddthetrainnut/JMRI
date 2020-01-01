package jmri.jmrix.bachrus.speedmatcher;

import jmri.ThrottleListener;

/**
 *
 * @author toddt
 */
public interface ISpeedMatcher extends ThrottleListener{
    
    public boolean StartSpeedMatch();
    public void StopSpeedMatch();
    
    public void CleanUp();
    
    public int GetSpeedMatcherStepElapsedSeconds();
    public boolean IsIdle();
}

