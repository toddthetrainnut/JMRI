package jmri.jmrix.bachrus.speedmatcher;

import javax.swing.JLabel;
import jmri.AddressedProgrammer;
import jmri.DccLocoAddress;
import jmri.DccThrottle;
import jmri.PowerManager;
import jmri.jmrix.bachrus.speedmatcher.ISpeedMatcher;
import org.slf4j.Logger;

/**
 *
 * @author toddt
 */
public abstract class BasicSpeedMatcher implements ISpeedMatcher{
    
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
    protected int acceleration;
    protected int deceleration;
    
    protected int stepDuration = 0;
    protected float currentSpeed = 0;
    
    protected DccLocoAddress dccLocoAddress;
    
    protected DccThrottle throttle = null;
    protected float throttleIncrement;
    protected AddressedProgrammer opsModeProgrammer = null;
    protected PowerManager powerManager = null;
    
    protected Logger logger;
    protected JLabel statusLabel;
        
    public boolean Validate() {
        if (dccLocoAddress.getNumber() <= 0) {
            statusLabel.setText("Please enter a valid DCC address");
            return false;
        }
        
        if (targetStartSpeedKPH < 1) {
            statusLabel.setText("Please enter a valid start speed");
            return false;
        }
        
        if (targetTopSpeedKPH <= targetStartSpeedKPH) {
            statusLabel.setText("Please enter a valid top speed");
            return false;
        }
        
        return true;
    } 
    
    @Override
    public void UpdateCurrentSpeed(float currentSpeed) {
        this.currentSpeed = currentSpeed;
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
