package jmri.jmrix.bachrus.speedmatcher;

import jmri.jmrix.bachrus.speedmatcher.BasicSpeedMatcher;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;
import jmri.AddressedProgrammer;
import jmri.AddressedProgrammerManager;
import jmri.DccLocoAddress;
import jmri.DccThrottle;
import jmri.InstanceManager;
import jmri.JmriException;
import jmri.LocoAddress;
import jmri.PowerManager;
import jmri.jmrix.bachrus.Speed;
import jmri.jmrix.bachrus.speedmatcher.SpeedMatcherConfig;
import org.slf4j.Logger;

/**
 *
 * @author Todd Wegter
 */
public class SimpleBasicSpeedMatcher extends BasicSpeedMatcher{
    
    private final int VSTART = 1;
    private final int VHIGH = 255;
    private final int VMID = 127;
    private final int TRIM = 128;
    
    private int vStart = VSTART;
    private int lastVStart = VSTART;
    private int vHigh = VHIGH;
    private int lastVHigh = VHIGH;
    private int reverseTrimValue = TRIM;
    private int lastReverseTrimValue = TRIM;
    
    private Timer speedMatchStateTimer;
    private SpeedMatcherState speedMatcherState = SpeedMatcherState.IDLE;
    private ProgrammerState programmerState = ProgrammerState.IDLE;
    
    public SimpleBasicSpeedMatcher(SpeedMatcherConfig config) {
        this.dccLocoAddress = config.dccLocoAddress;
        this.powerManager = config.powerManager;
        
        if (config.speedUnit == Speed.Unit.MPH) {
            this.targetStartSpeedKPH = Speed.mphToKph(config.targetStartSpeed);
            this.targetTopSpeedKPH = Speed.mphToKph(config.targetTopSpeed);
        }
        else {
            this.targetStartSpeedKPH = config.targetStartSpeed;
            this.targetTopSpeedKPH = config.targetTopSpeed;
        }
        this.trimReverseSpeed = config.trimReverseSpeed;
        this.warmUpLocomotive = config.warmUpLoco;
        
        this.logger = config.logger;
        this.statusLabel = config.statusLabel;
    }
    
    @Override
    public boolean StartSpeedMatch() {
        String error = "";
        
        if (!super.Validate()) {
            return false;
        }
        
        //reset instance variables
        vStart = VSTART;
        lastVStart = VSTART;
        vHigh = VHIGH;
        lastVHigh = VHIGH;
        reverseTrimValue = TRIM;
        lastReverseTrimValue = TRIM;
        
        speedMatcherState = SpeedMatcherState.WAIT_FOR_THROTTLE;
        
        //get OPS MODE Programmer
        if (InstanceManager.getNullableDefault(AddressedProgrammerManager.class) != null) {
            if (InstanceManager.getDefault(AddressedProgrammerManager.class).isAddressedModePossible(dccLocoAddress)) {
                opsModeProgrammer = InstanceManager.getDefault(AddressedProgrammerManager.class).getAddressedProgrammer(dccLocoAddress);
            }
        }
        
         //start speed match timer
        speedMatchStateTimer = new javax.swing.Timer(4000, e -> speedMatchTimeout());
        speedMatchStateTimer.setRepeats(false); //timer is used without repeats to improve time accuracy when changing the delay
        
        //request a throttle
        statusLabel.setText(Bundle.getMessage("StatReqThrottle"));
        speedMatchStateTimer.start();
        logger.info("Requesting Throttle");
        boolean throttleRequestOK = InstanceManager.throttleManagerInstance().requestThrottle(dccLocoAddress, this, true);
        if (!throttleRequestOK) {
            logger.error("Loco Address in use, throttle request failed.");
            statusLabel.setText(Bundle.getMessage("StatAddressInUse"));
            CleanUp();
        }       
       
        return throttleRequestOK;
    }
    
    @Override
    public void StopSpeedMatch() {
        CleanUp();
        
        logger.info("Speed matching manually stopped");
    }
    
    @Override
    public boolean IsIdle() {
        return speedMatcherState == SpeedMatcherState.IDLE;
    }
    
    @Override
    public void CleanUp() {
        speedMatcherState = SpeedMatcherState.IDLE;
        
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
    
    /**
     * Sets up the speed match state by setting the throttle direction and
     * speed, clearing the speed match error, clearing the step elapsed seconds,
     * and setting the timer initial delay 
     *
     * @param isForward    - throttle direction - true for forward, false for
     *                       reverse
     * @param speedStep    - throttle speed step
     * @param initialDelay - initial delay for the timer in milliseconds
     */
    private void setupSpeedMatchState(boolean isForward, int speedStep, int initialDelay) {
        throttle.setIsForward(isForward);
        throttle.setSpeedSetting(speedStep * throttleIncrement);
        speedMatchError = 0;
        stepElapsedSeconds = 0;
        speedMatchStateTimer.setInitialDelay(initialDelay);
    }
    
    private void stopStateTimer() {
        if (speedMatchStateTimer != null) {
            speedMatchStateTimer.stop();
        }
    }
    
    /**
     * Timer timeout handler for the speed match timer
     */
    protected synchronized void speedMatchTimeout() {
        switch (speedMatcherState) {
            case WAIT_FOR_THROTTLE:
                CleanUp();
                logger.error("Timeout waiting for throttle");
                statusLabel.setText(Bundle.getMessage("StatusTimeout"));
                break;

            case INIT_ACCEL:
                //set acceleration momentum to 0 (CV 3)
                if (programmerState == ProgrammerState.IDLE) {
                    writeMomentumAccel(0);
                    speedMatcherState = speedMatcherState.nextState(this);
                    setupSpeedMatchState(true, 0, 1500);
                    speedMatchStateTimer.start();
                }
                break;

            case INIT_DECEL:
                //set deceleration mementum to 0 (CV 4)
                if (programmerState == ProgrammerState.IDLE) {
                    writeMomentumDecel(0);
                    speedMatcherState = speedMatcherState.nextState(this);
                    setupSpeedMatchState(true, 0, 1500);
                    speedMatchStateTimer.start();
                }
                break;

            case INIT_VSTART:
                //set vStart to 1 (CV 2)
                if (programmerState == ProgrammerState.IDLE) {
                    writeVStart(VSTART);
                    speedMatcherState = speedMatcherState.nextState(this);
                    setupSpeedMatchState(true, 0, 1500);
                    speedMatchStateTimer.start();
                }
                break;
                
            case INIT_VMID:
                //set vHigh to 255 (CV 6)
                if (programmerState == ProgrammerState.IDLE) {
                    writeVMid(VMID);
                    speedMatcherState = speedMatcherState.nextState(this);
                    setupSpeedMatchState(true, 0, 1500);
                    speedMatchStateTimer.start();
                }
                break;

            case INIT_VHIGH:
                //set vHigh to 255 (CV 5)
                if (programmerState == ProgrammerState.IDLE) {
                    writeVHigh(VHIGH);
                    speedMatcherState = speedMatcherState.nextState(this);
                    setupSpeedMatchState(true, 0, 1500);
                    speedMatchStateTimer.start();
                }
                break;

            case INIT_FORWARD_TRIM:
                //set forward trim to 128 (CV 66)
                if (programmerState == ProgrammerState.IDLE) {
                    writeForwardTrim(TRIM);
                    speedMatcherState = speedMatcherState.nextState(this);
                    setupSpeedMatchState(true, 0, 1500);
                    speedMatchStateTimer.start();
                }
                break;

            case INIT_REVERSE_TRIM:
                //set revers trim to 128 (CV 95)
                if (programmerState == ProgrammerState.IDLE) {
                    writeReverseTrim(TRIM);
                    speedMatcherState = speedMatcherState.nextState(this);
                    setupSpeedMatchState(true, 0, 1500);
                    speedMatchStateTimer.start();
                }
                break;
  
            case FORWARD_WARM_UP:
                //Run 4 minutes at high speed forward
                statusLabel.setText(Bundle.getMessage("StatForwardWarmUp", 240 - stepElapsedSeconds));

                if (stepElapsedSeconds <= 0) {
                    setupSpeedMatchState(true, 28, 5000);
                } else if (stepElapsedSeconds >= 240) {
                    speedMatcherState = speedMatcherState.nextState(this);
                    setupSpeedMatchState(true, 0, 2000);
                } else {
                    stepElapsedSeconds += 5;
                }
                
                speedMatchStateTimer.start();
                
                break;
//
//            case FORWARD_SPEED_MATCH_STEP_1:
//                //Use PID Controller to adjust vStart (and VMid) to achieve desired speed
//                if (programmerState == ProgrammerState.IDLE) {
//                    if (speedMatchDuration == 0) {
//                        statusLabel.setText(Bundle.getMessage("StatSettingSpeedStep1"));
//                        setupSpeedMatchTimer(true, 1, 15000);
//                        speedMatchDuration = 1;
//                    } else {
//                        setSpeedMatchError(speedStep1Target);
//
//                        if ((speedMatchError < 0.5) && (speedMatchError > -0.5)) {
//                            state = SpeedMatchState.FORWARD_SPEED_MATCH_STEP_28;
//                            setupSpeedMatchTimer(true, 0, 8000);
//                            speedMatchDuration = 0;
//                        } else {
//                            vStart = getNextSpeedMatchValue(lastVStart);
//
//                            if (((lastVStart == 1) || (lastVStart == 255)) && (vStart == lastVStart)) {
//                                statusLabel.setText(Bundle.getMessage("StatSetSpeedStep1Fail"));
//                                logger.debug("Unable to achieve desired speed at Speed Step 1");
//                                CleanUp();
//                            } else {
//                                lastVStart = vStart;
//                                writeVStart();
//                            }
//                            speedMatchTimer.setInitialDelay(8000);
//                        }
//                    }
//                }
//                break;
//
//            case FORWARD_SPEED_MATCH_STEP_28:
//                //Use PID Controller llogic to adjust vHigh (and vMid) to achieve desired speed
//                if (programmerState == ProgrammerState.IDLE) {
//                    if (speedMatchDuration == 0) {
//                        statusLabel.setText(Bundle.getMessage("StatSettingSpeedStep28"));
//                        setupSpeedMatchTimer(true, 28, 15000);
//                        speedMatchDuration = 1;
//                    } else {
//                        setSpeedMatchError(speedStep28Target);
//
//                        if ((speedMatchError < 0.5) && (speedMatchError > -0.5)) {
//                            if (basicSpeedMatchWarmUpCheckBox.isSelected()) {
//                                state = SpeedMatchState.REVERSE_WARM_UP;
//                            } else {
//                                state = SpeedMatchState.REVERSE_SPEED_MATCH_TRIM;
//                            }
//                            setupSpeedMatchTimer(false, 0, 5000);
//                            speedMatchDuration = 0;
//                        } else {
//                            vHigh = getNextSpeedMatchValue(lastVHigh);
//
//                            if (((lastVHigh == 1) || (lastVHigh == 255)) && (vHigh == lastVHigh)) {
//                                statusLabel.setText(Bundle.getMessage("StatSetSpeedStep28Fail"));
//                                logger.debug("Unable to achieve desired speed at Speed Step 28");
//                                CleanUp();
//                            } else {
//                                lastVHigh = vHigh;
//                                writeVHigh();
//                            }
//                            speedMatchTimer.setInitialDelay(8000);
//                        }
//                    }
//                }
//                break;
//
//            case REVERSE_WARM_UP:
//                //Run 2 minutes at high speed reverse
//                statusLabel.setText(Bundle.getMessage("StatReverseWarmUp", 120 - speedMatchDuration));
//
//                if (speedMatchDuration >= 120) {
//                    state = SpeedMatchState.REVERSE_SPEED_MATCH_TRIM;
//                } else {
//                    speedMatchDuration += 5;
//                }
//                setupSpeedMatchTimer(false, 28, 5000);
//                break;
//
//            case REVERSE_SPEED_MATCH_TRIM:
//                //Use PID controller logic to adjust reverse trim until high speed reverse speed matches forward
//                if (programmerState == ProgrammerState.IDLE) {
//                    if (speedMatchDuration == 0) {
//                        statusLabel.setText(Bundle.getMessage("StatSettingReverseTrim"));
//                        setupSpeedMatchTimer(false, 28, 15000);
//                        speedMatchDuration = 1;
//                    } else {
//                        setSpeedMatchError(speedStep28Target);
//
//                        if ((speedMatchError < 0.5) && (speedMatchError > -0.5)) {
//                            state = SpeedMatchState.RESTORE_MOMENTUM;
//                            speedMatchSetupState = SpeedMatchSetupState.MOMENTUM_ACCEL_WRITE;
//                            setupSpeedMatchTimer(false, 0, 1500);
//                            speedMatchDuration = 0;
//                        } else {
//                            reverseTrim = getNextSpeedMatchValue(lastReverseTrim);
//
//                            if (((lastReverseTrim == 1) || (lastReverseTrim == 255)) && (reverseTrim == lastReverseTrim)) {
//                                statusLabel.setText(Bundle.getMessage("StatSetReverseTripFail"));
//                                logger.debug("Unable to trim reverse to match forward");
//                                CleanUp();
//                            } else {
//                                lastReverseTrim = reverseTrim;
//                                writeReverseTrim(reverseTrim);
//                            }
//                            speedMatchTimer.setInitialDelay(8000);
//                        }
//                    }
//                }
//                break;
//
//            case RESTORE_MOMENTUM:
//                //restore momentum CVs
//                switch (speedMatchSetupState) {
//                    case MOMENTUM_ACCEL_WRITE:
//                        //restore acceleration momentum (CV 3)
//                        if (programmerState == ProgrammerState.IDLE) {
//                            writeMomentumAccel(oldMomentumAccel);
//                            speedMatchSetupState = SpeedMatchSetupState.MOMENTUM_DECEL_WRITE;
//                        }
//                        break;
//
//                    case MOMENTUM_DECEL_WRITE:
//                        //restore deceleration mumentum (CV 4)
//                        if (programmerState == ProgrammerState.IDLE) {
//                            writeMomentumDecel(oldMomentumDecel);
//                            speedMatchSetupState = SpeedMatchSetupState.IDLE;
//                        }
//                        break;
//
//                    case IDLE:
//                        //wrap everything up
//                        if (programmerState == ProgrammerState.IDLE) {
//                            CleanUp();
//                            statusLabel.setText(Bundle.getMessage("StatSpeedMatchComplete"));
//                        }
//                        break;
//
//                    default:
//                        logger.warn("Unhandled speed match cleanup state: {}", speedMatchSetupState);
//                }
//                break;

            default:
                CleanUp();
                logger.error("Unexpected speed match timeout");
                break;
        }

        if (state != SpeedMatcherState.IDLE) {
            speedMatchTimer.start();
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="ThrottleListener Overrides">
    /**
     * Called when a throttle is found
     *
     * @param t the requested DccThrottle
     */
    @Override
    public void notifyThrottleFound(DccThrottle t) {
        stopStateTimer();
        
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

        if (state == SpeedMatcherState.WAIT_FOR_THROTTLE) {
            logger.info("Starting speed matching");

            // using speed matching timer to trigger each phase of speed matching
            state = state.nextState(this);
            setupSpeedMatchState(true, 0, 1500);
            speedMatchStateTimer.start();
        } else {
            CleanUp();
        }
    }
    
    /**
     * Called when we must decide whether to steal the throttle for the requested address.
     * This is an automatically stealing implementation, so the throttle will be automatically stolen
     *
     * @param address the requested address
     * @param question the question being asked, steal / cancel, share / cancel, steal / share / cancel
     */
    @Override
    public void notifyDecisionRequired(LocoAddress address, DecisionType question) {
        InstanceManager.throttleManagerInstance().responseThrottleDecision(address, this, DecisionType.STEAL);
    }
    
    /**
     * @deprecated since 4.15.7; use
     * #notifyDecisionRequired(LocoAddress, DecisionType) instead
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
    
    protected enum SpeedMatcherState {
        IDLE {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return this;
           } 
        },
        WAIT_FOR_THROTTLE {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.IDLE;
           } 
        },
        INIT_ACCEL {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.INIT_DECEL;
           } 
        },
        INIT_DECEL {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.INIT_VSTART;
           } 
        },
        INIT_VSTART {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.INIT_VMID;
           } 
        },
        INIT_VMID {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.INIT_VHIGH;
           } 
        },
        INIT_VHIGH {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.INIT_FORWARD_TRIM;
           } 
        },
        INIT_FORWARD_TRIM {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.INIT_REVERSE_TRIM;
           } 
        },
        INIT_REVERSE_TRIM {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               if (speedMatcher.warmUpLocomotive) {
                   return SpeedMatcherState.FORWARD_WARM_UP;
               } else {
                   return SpeedMatcherState.FORWARD_SPEED_MATCH_VHIGH;
               }
           } 
        },
        FORWARD_WARM_UP {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               if (speedMatcher.trimReverseSpeed) {
                   return SpeedMatcherState.REVERSE_WARM_UP;
               } else {
                   return SpeedMatcherState.FORWARD_SPEED_MATCH_VHIGH;
               }
           } 
        },
        REVERSE_WARM_UP {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.FORWARD_SPEED_MATCH_VHIGH;
           } 
        },
        FORWARD_SPEED_MATCH_VHIGH {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.FORWARD_SPEED_MATCH_VSTART;
           } 
        },
        FORWARD_SPEED_MATCH_VSTART {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.FORWARD_SPEED_MATCH_VMID;
           } 
        },
        FORWARD_SPEED_MATCH_VMID {
            @Override
            protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
                if (speedMatcher.trimReverseSpeed) {
                    return SpeedMatcherState.REVERSE_SPEED_MATCH_TRIM;
                } else {
                    return SpeedMatcherState.SET_ACCEL;
                }
            }
        },
        REVERSE_SPEED_MATCH_TRIM {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.SET_ACCEL;
           } 
        },
        SET_ACCEL {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.SET_DECEL;
           } 
        },
        SET_DECEL {
           @Override
           protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return SpeedMatcherState.CLEAN_UP;
           } 
        },
        CLEAN_UP {
          @Override
          protected SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher) {
              return SpeedMatcherState.IDLE;
          }
        };
        
        protected abstract SpeedMatcherState nextState(SimpleBasicSpeedMatcher speedMatcher);
    }
    
    private enum ProgrammerState {
        IDLE {
            @Override
            protected ProgrammerState nextState() {
                return this;
            }
        };
        
        protected abstract ProgrammerState nextState();
    }
}
