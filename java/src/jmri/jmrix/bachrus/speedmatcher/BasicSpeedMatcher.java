package jmri.jmrix.bachrus.speedmatcher;

import jmri.DccLocoAddress;

/**
 *
 * @author toddt
 */
public class BasicSpeedMatcher implements ISpeedMatcher{
    
    //PID Coontroller Values
    protected final float kP = 0.75f;
    protected final float kI = 0.3f;
    protected final float kD = 0.4f;
    
    protected float speedMatchIntegral = 0;
    protected float speedMatchDerivative = 0;
    protected float lastSpeedMatchError = 0;
    protected float speedMatchError = 0;
    
    protected float targetStartSpeedKPH;
    protected float targetTopSpeedKPH;
    protected boolean trimReverseSpeed;
    protected boolean warmUpLocomotive;
    
    protected int stepElapsedSeconds = 0;
    protected float currentSpeed = 0;
    
    protected DccLocoAddress dccLocoAddress;
    
    protected BasicSpeedMatcher() {
        
    }
    
    public boolean Initialize(DccLocoAddress dccLocoAddress, float targetStartSpeedKPH, float targetTopSpeedKPH, boolean trimReverseSpeed, boolean warmUpLoco, String error) {
        if (dccLocoAddress.getNumber() <= 0) {
            error = "Please enter a valid DCC address";
            return false;
        }
        
        if (targetStartSpeedKPH < 1) {
            error = "Please enter a valid start speed";
            return false;
        }
        
        if (targetTopSpeedKPH <= targetStartSpeedKPH) {
            error = "Please enter a valid top speed";
            return false;
        }
        
        this.dccLocoAddress = dccLocoAddress;
        this.targetStartSpeedKPH = targetStartSpeedKPH;
        this.targetTopSpeedKPH = targetTopSpeedKPH;
        this.trimReverseSpeed = trimReverseSpeed;
        this.warmUpLocomotive = warmUpLoco;
        
        return true;
    }
    
    public abstract boolean StartSpeedMatch(DccLocoAddress dccLocoAddress, float targetStartSpeedKPH, float targetTopSpeedKPH, boolean warmUpLoco, boolean trimReverse, String error);
    
    @Override
    public int GetSpeedMatcherStepElapsedSeconds() {
        return stepElapsedSeconds;
    }
    
  
    
        /**
     * Sets the PID controller's speed match error for speed matching
     *
     * @param speedTarget - target speed in KPH
     */
    protected void setSpeedMatchError(float speedTarget) {
        speedMatchError = speedTarget - currentSpeed;
    }

    /**
     * Gets the next value to try for speed matching using a PID controller
     *
     * @param lastValue - the last speed match CV value tried
     * @return the next value to try for speed matching (1-255 inclusive)
     */
    protected int getNextSpeedMatchValue(int lastValue) {
        speedMatchIntegral += speedMatchError;
        speedMatchDerivative = speedMatchError - lastSpeedMatchError;

        int value = (lastValue + Math.round((kP * speedMatchError) + (kI * speedMatchIntegral) + (kD * speedMatchDerivative)));

        if (value > 255) {
            value = 255;
        } else if (value < 1) {
            value = 1;
        }

        return value;
    }
}
