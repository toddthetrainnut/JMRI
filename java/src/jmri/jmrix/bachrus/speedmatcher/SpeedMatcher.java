package jmri.jmrix.bachrus.speedmatcher;

import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.Timer;
import jmri.AddressedProgrammer;
import jmri.AddressedProgrammerManager;
import jmri.DccLocoAddress;
import jmri.DccThrottle;
import jmri.InstanceManager;
import jmri.JmriException;
import jmri.LocoAddress;
import jmri.PowerManager;
import jmri.ProgListener;
import jmri.ThrottleListener;
import org.slf4j.Logger;

/**
 *
 * @author toddt
 */
public abstract class SpeedMatcher implements ThrottleListener, ProgListener {

    //<editor-fold defaultstate="collapsed" desc="Constants">
    //PID Coontroller Values
    protected final float kP = 0.75f;
    protected final float kI = 0.3f;
    protected final float kD = 0.4f;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Instance Variables">
    protected float speedMatchIntegral = 0;
    protected float speedMatchDerivative = 0;
    protected float lastSpeedMatchError = 0;
    protected float speedMatchError = 0;

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
    protected Timer speedMatchStateTimer;

    protected Logger logger;
    protected JLabel statusLabel;
    //</editor-fold>

    public SpeedMatcher(SpeedMatcherConfig config) {
        this.dccLocoAddress = config.dccLocoAddress;
        this.powerManager = config.powerManager;

        this.trimReverseSpeed = config.trimReverseSpeed;
        this.warmUpLocomotive = config.warmUpLoco;
        this.acceleration = config.acceleration;
        this.deceleration = config.deceleration;

        this.logger = config.logger;
        this.statusLabel = config.statusLabel;
    }
    
    //<editor-fold defaultstate="collapsed" desc="Public APIs">   
    public abstract boolean StartSpeedMatch();

    public abstract void StopSpeedMatch();

    public abstract boolean IsIdle();

    public void UpdateCurrentSpeed(float currentSpeedKPH) {
        this.currentSpeed = currentSpeedKPH;
    }
    
    public void CleanUp() {
        //stop the timer
        if (speedMatchStateTimer != null) {
            speedMatchStateTimer.stop();
        }

        //release throttle
        if (throttle != null) {
            throttle.setSpeedSetting(0.0F);
            InstanceManager.throttleManagerInstance().releaseThrottle(throttle, this);
            throttle = null;
        }

        //release ops mode programmer
        if (opsModeProgrammer != null) {
            InstanceManager.getDefault(AddressedProgrammerManager.class).releaseAddressedProgrammer(opsModeProgrammer);
            opsModeProgrammer = null;
        }

        statusLabel.setText(" ");
    }
    //</editor-fold>

    protected boolean InitializeAndStartSpeedMatcher(ActionListener timerActionListener) {
        //Setup speed match timer
        speedMatchStateTimer = new javax.swing.Timer(4000, timerActionListener);
        speedMatchStateTimer.setRepeats(false); //timer is used without repeats to improve time accuracy when changing the delay

        if (!GetOpsModeProgrammer()) {
            return false;
        }

        return GetThrottle();
    }

        /**
     * Sets up the speed match state by setting the throttle direction and
     * speed, clearing the speed match error, clearing the step elapsed seconds,
     * and setting the timer initial delay
     *
     * @param isForward    - throttle direction - true for forward, false for
     *                     reverse
     * @param speedStep    - throttle speed step
     * @param initialDelay - initial delay for the timer in milliseconds
     */
    protected void setupSpeedMatchState(boolean isForward, int speedStep, int initialDelay) {
        throttle.setIsForward(isForward);
        throttle.setSpeedSetting(speedStep * throttleIncrement);
        speedMatchError = 0;
        stepDuration = 0;
        speedMatchStateTimer.setInitialDelay(initialDelay);
    }

    protected void stopSpeedMatchStateTimer() {
        if (speedMatchStateTimer != null) {
            speedMatchStateTimer.stop();
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="Helper Functions">
    private boolean GetOpsModeProgrammer() {
        logger.info("Requesting Programmer");
        //get OPS MODE Programmer
        if (InstanceManager.getNullableDefault(AddressedProgrammerManager.class) != null) {
            if (InstanceManager.getDefault(AddressedProgrammerManager.class).isAddressedModePossible(dccLocoAddress)) {
                opsModeProgrammer = InstanceManager.getDefault(AddressedProgrammerManager.class).getAddressedProgrammer(dccLocoAddress);
            }
        }
        
        if (opsModeProgrammer != null) {
            return true;
        } else {
            logger.error("Programmer request failed.");
            statusLabel.setText("Programmer request failed");
            return false;
        }
    }
    
    private boolean GetThrottle() {
        statusLabel.setText("Requesting Throttle");
        logger.info("Requesting Throttle");
        speedMatchStateTimer.start();
        boolean throttleRequestOK = InstanceManager.throttleManagerInstance().requestThrottle(dccLocoAddress, this, true);
        if (!throttleRequestOK) {
            logger.error("Loco Address in use, throttle request failed.");
            statusLabel.setText("Loco Address in use, throttle request failed");
        }
        return throttleRequestOK;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="ThrottleListener Overrides">
    /**
     * Called when a throttle is found Must override, call super, and start
     * speed matcher in implementation
     *
     * @param t the requested DccThrottle
     */
    @Override
    public void notifyThrottleFound(DccThrottle t) {
        stopSpeedMatchStateTimer();

        throttle = t;
        logger.info("Throttle acquired");
        throttle.setSpeedStepMode(DccThrottle.SpeedStepMode28);
        if (throttle.getSpeedStepMode() != DccThrottle.SpeedStepMode28) {
            logger.error("Failed to set 28 step mode");
            statusLabel.setText(Bundle.getMessage("ThrottleError28"));
            InstanceManager.throttleManagerInstance().releaseThrottle(throttle, this);
            return;
        }

        // turn on power
        try {
            powerManager.setPower(PowerManager.ON);
        } catch (JmriException e) {
            logger.error("Exception during power on: " + e.toString());
            return;
        }

        throttleIncrement = throttle.getSpeedIncrement();
    }

    /**
     * Called when we must decide whether to steal the throttle for the
     * requested address. This is an automatically stealing implementation, so
     * the throttle will be automatically stolen
     *
     * @param address  the requested address
     * @param question the question being asked, steal / cancel, share / cancel,
     *                 steal / share / cancel
     */
    @Override
    public void notifyDecisionRequired(LocoAddress address, DecisionType question) {
        InstanceManager.throttleManagerInstance().responseThrottleDecision(address, this, DecisionType.STEAL);
    }

    /**
     * @deprecated since 4.15.7; use #notifyDecisionRequired(LocoAddress,
     * DecisionType) instead
     *
     * @param address the requested address
     */
    @Deprecated
    @Override
    public void notifyStealThrottleRequired(jmri.LocoAddress address) {
    }

    /**
     * Called when a throttle could not be obtained
     *
     * @param address the requested address
     * @param reason  the reason the throttle could not be obtained
     */
    @Override
    public void notifyFailedThrottleRequest(jmri.LocoAddress address, String reason) {
    }
    //</editor-fold>
}
