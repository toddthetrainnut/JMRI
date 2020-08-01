package jmri.jmrix.bachrus.speedmatcher;

import jmri.ThrottleListener;
import jmri.jmrix.bachrus.SpeedoListener;

/**
 *
 * @author toddt
 */
public interface ISpeedMatcher extends ThrottleListener, SpeedoListener{
    
    public boolean StartSpeedMatch();
    public void StopSpeedMatch();
    
    public void UpdateCurrentSpeed(float currentSpeed);
    
    public void CleanUp();
    
    public boolean IsIdle();
}

