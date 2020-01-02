package jmri.jmrix.bachrus.speedmatcher;

import jmri.ThrottleListener;

/**
 *
 * @author toddt
 */
public interface ISpeedMatcher extends ThrottleListener{
    
    public boolean StartSpeedMatch();
    public void StopSpeedMatch();
    
    public void UpdateCurrentSpeed(float currentSpeed);
    
    public void CleanUp();
    
    public boolean IsIdle();
}

