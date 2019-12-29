package jmri.jmrix.bachrus.speedmatcher;

/**
 *
 * @author toddt
 */
public interface ISpeedMatcher {
    
    public boolean StartSpeedMatch(String error);
    
    public int GetSpeedMatcherStepElapsedSeconds();
    public boolean IsIdle();
}

