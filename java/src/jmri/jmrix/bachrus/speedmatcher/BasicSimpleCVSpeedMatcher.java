package jmri.jmrix.bachrus.speedmatcher;

import jmri.DccThrottle;
import jmri.ProgrammerException;

/**
 *
 * @author Todd Wegter
 */
public class BasicSimpleCVSpeedMatcher extends BasicSpeedMatcher {

    //<editor-fold defaultstate="collapsed" desc="Constants">
    private final int VSTART = 1;
    private final int VHIGH = 255;
    private final int VMID = 1;
    private final int TRIM = 128;
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Instance Variables">
    private int vStart = VSTART;
    private int lastVStart = VSTART;
    private int vHigh = VHIGH;
    private int lastVHigh = VHIGH;
    private int vMid = VMID;
    private int lastVMid = VMID;
    private int reverseTrimValue = TRIM;
    private int lastReverseTrimValue = TRIM;
    
    private float targetMidSpeedKPH;
    
    private SpeedMatcherState speedMatcherState = SpeedMatcherState.IDLE;
    private ProgrammerState programmerState = ProgrammerState.IDLE;
    //</editor-fold>
    
    public BasicSimpleCVSpeedMatcher(SpeedMatcherConfig config) {
        super(config);

        this.targetMidSpeedKPH = this.targetStartSpeedKPH + ((this.targetTopSpeedKPH - this.targetStartSpeedKPH) / 2);
    }

    //<editor-fold defaultstate="collapsed" desc="SpeedMatcher Overrides">
    @Override
    public boolean StartSpeedMatch() {
        
        if (!super.Validate()) {
            return false;
        }

        //reset instance variables
        vStart = VSTART;
        lastVStart = VSTART;
        vHigh = VHIGH;
        lastVHigh = VHIGH;
        vMid = 128;
        lastVMid = vMid;
        reverseTrimValue = TRIM;
        lastReverseTrimValue = TRIM;

        speedMatcherState = SpeedMatcherState.WAIT_FOR_THROTTLE;

        if (!super.InitializeAndStartSpeedMatcher(e -> speedMatchTimeout())) {
            CleanUp();
            return false;
        }

        return true;
    }

    @Override
    public void StopSpeedMatch() {
        logger.info("Speed matching manually stopped");
        CleanUp();
    }

    @Override
    public boolean IsIdle() {
        return speedMatcherState == SpeedMatcherState.IDLE;
    }

    @Override
    public void CleanUp() {
        speedMatcherState = SpeedMatcherState.IDLE;
        super.CleanUp();
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Speed Matcher State">
    
    /**
     * Timer timeout handler for the speed match timer
     */
    private synchronized void speedMatchTimeout() {
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
                    setupNextSpeedMatchState(true, 0);
                }
                break;
                
            case INIT_DECEL:
                //set deceleration mementum to 0 (CV 4)
                if (programmerState == ProgrammerState.IDLE) {
                    writeMomentumDecel(0);
                    setupNextSpeedMatchState(true, 0);
                }
                break;
                
            case INIT_VSTART:
                //set vStart to 1 (CV 2)
                if (programmerState == ProgrammerState.IDLE) {
                    writeVStart(VSTART);
                    setupNextSpeedMatchState(true, 0);
                }
                break;
                
            case INIT_VMID:
                //set vMid to 1 (CV 6)
                if (programmerState == ProgrammerState.IDLE) {
                    writeVMid(VMID);
                    setupNextSpeedMatchState(true, 0);
                }
                break;
                
            case INIT_VHIGH:
                //set vHigh to 255 (CV 5)
                if (programmerState == ProgrammerState.IDLE) {
                    writeVHigh(VHIGH);
                    setupNextSpeedMatchState(true, 0);
                }
                break;
                
            case INIT_FORWARD_TRIM:
                //set forward trim to 128 (CV 66)
                if (programmerState == ProgrammerState.IDLE) {
                    writeForwardTrim(TRIM);
                    setupNextSpeedMatchState(true, 0);
                }
                break;
                
            case INIT_REVERSE_TRIM:
                //set reverse trim to 128 (CV 95)
                if (programmerState == ProgrammerState.IDLE) {
                    writeReverseTrim(TRIM);
                    setupNextSpeedMatchState(true, 0);
                }
                break;
                
            case FORWARD_WARM_UP:
                //Run 4 minutes at high speed forward
                statusLabel.setText(Bundle.getMessage("StatForwardWarmUp", 240 - stepDuration));
                
                if (stepDuration == 0) {
                    setupSpeedMatchState(true, 28, 5000);
                } else if (stepDuration >= 240) {
                    setupNextSpeedMatchState(true, 28);
                } else {
                    stepDuration += 5;
                }
                
                break;
                
            case FORWARD_SPEED_MATCH_VHIGH:
                //Use PID Controller logic to adjust vHigh to achieve desired speed
                if (programmerState == ProgrammerState.IDLE) {
                    if (stepDuration == 0) {
                        statusLabel.setText(Bundle.getMessage("StatSettingSpeedStep28"));
                        setupSpeedMatchState(true, 28, 15000);
                        stepDuration = 1;
                    } else {
                        setSpeedMatchError(targetTopSpeedKPH);
                        
                        if ((speedMatchError < 0.5) && (speedMatchError > -0.5)) {
                            setupNextSpeedMatchState(true, 0);
                        } else {
                            vHigh = getNextSpeedMatchValue(lastVHigh);
                            
                            if (((lastVHigh == 1) || (lastVHigh == 255)) && (vHigh == lastVHigh)) {
                                statusLabel.setText(Bundle.getMessage("StatSetSpeedStep28Fail"));
                                logger.debug("Unable to achieve desired speed at Speed Step 28");
                                Abort();
                                break;
                            } else {
                                lastVHigh = vHigh;
                                writeVHigh(vHigh);
                            }
                            speedMatchStateTimer.setInitialDelay(8000);
                        }
                    }
                }
                break;
                
            case FORWARD_SPEED_MATCH_VMID:
                //Use PID Controller logic to adjust vMid to achieve desired speed
                if (programmerState == ProgrammerState.IDLE) {
                    if (stepDuration == 0) {
                        statusLabel.setText("Setting VMid");
                        setupSpeedMatchState(true, 14, 15000);
                        stepDuration = 1;
                        
                        vMid = vStart + ((vHigh - vStart) / 2);
                        lastVMid = vMid;
                        writeVMid(vMid);
                    } else {
                        setSpeedMatchError(targetMidSpeedKPH);
                        
                        if ((speedMatchError < 0.5) && (speedMatchError > -0.5)) {
                            setupNextSpeedMatchState(true, 0);
                        } else {
                            vMid = getNextSpeedMatchValue(lastVMid);
                            
                            if (((lastVMid == 1) || (lastVMid == 255)) && (vMid == lastVMid)) {
                                statusLabel.setText(Bundle.getMessage("StatSetSpeedStep28Fail"));
                                logger.debug("Unable to achieve desired speed vMid");
                                Abort();
                                break;
                            } else {
                                lastVMid = vMid;
                                writeVMid(vMid);
                            }
                            speedMatchStateTimer.setInitialDelay(8000);
                        }
                    }
                }
                break;
                
            case FORWARD_SPEED_MATCH_VSTART:
                //Use PID Controller to adjust vStart to achieve desired speed
                if (programmerState == ProgrammerState.IDLE) {
                    if (stepDuration == 0) {
                        statusLabel.setText(Bundle.getMessage("StatSettingSpeedStep1"));
                        setupSpeedMatchState(true, 1, 15000);
                        stepDuration = 1;
                    } else {
                        setSpeedMatchError(targetStartSpeedKPH);
                        
                        if ((speedMatchError < 0.5) && (speedMatchError > -0.5)) {
                            setupNextSpeedMatchState(true, 0);
                        } else {
                            vStart = getNextSpeedMatchValue(lastVStart);
                            
                            if (((lastVStart == 1) || (lastVStart == 255)) && (vStart == lastVStart)) {
                                statusLabel.setText(Bundle.getMessage("StatSetSpeedStep1Fail"));
                                logger.debug("Unable to achieve desired speed at Speed Step 1");
                                Abort();
                                break;
                            } else {
                                lastVStart = vStart;
                                writeVStart(vStart);
                            }
                            speedMatchStateTimer.setInitialDelay(8000);
                        }
                    }
                }
                break;
                
            case REVERSE_WARM_UP:
                //Run 2 minutes at high speed in reverse
                statusLabel.setText(Bundle.getMessage("StatReverseWarmUp", 240 - stepDuration));
                
                if (stepDuration == 0) {
                    setupSpeedMatchState(false, 28, 5000);
                } else if (stepDuration >= 120) {
                    setupNextSpeedMatchState(false, 28);
                } else {
                    stepDuration += 5;
                }
                
                break;
                
            case REVERSE_SPEED_MATCH_TRIM:
                //Use PID controller logic to adjust reverse trim until high speed reverse speed matches forward
                if (programmerState == ProgrammerState.IDLE) {
                    if (stepDuration == 0) {
                        statusLabel.setText(Bundle.getMessage("StatSettingReverseTrim"));
                        setupSpeedMatchState(false, 28, 15000);
                        stepDuration = 1;
                    } else {
                        setSpeedMatchError(targetTopSpeedKPH);
                        
                        if ((speedMatchError < 0.5) && (speedMatchError > -0.5)) {
                            setupNextSpeedMatchState(true, 0);
                        } else {
                            reverseTrimValue = getNextSpeedMatchValue(lastReverseTrimValue);
                            
                            if (((lastReverseTrimValue == 1) || (lastReverseTrimValue == 255)) && (reverseTrimValue == lastReverseTrimValue)) {
                                statusLabel.setText(Bundle.getMessage("StatSetReverseTripFail"));
                                logger.debug("Unable to trim reverse to match forward");
                                Abort();
                                break;
                            } else {
                                lastReverseTrimValue = reverseTrimValue;
                                writeReverseTrim(reverseTrimValue);
                            }
                            speedMatchStateTimer.setInitialDelay(8000);
                        }
                    }
                }
                break;
                
            case SET_ACCEL:
                //set acceleration momentum (CV 3)
                if (programmerState == ProgrammerState.IDLE) {
                    writeMomentumAccel(acceleration);
                    setupNextSpeedMatchState(true, 0);
                }
                break;
                
            case SET_DECEL:
                //set deceleration momentum (CV 4)
                if (programmerState == ProgrammerState.IDLE) {
                    writeMomentumDecel(deceleration);
                    setupNextSpeedMatchState(true, 0);
                }
                break;
                
            case CLEAN_UP:
                //wrap it up
                if (programmerState == ProgrammerState.IDLE) {
                    CleanUp();
                    statusLabel.setText(Bundle.getMessage("StatSpeedMatchComplete"));
                }
                break;
                
            default:
                CleanUp();
                logger.error("Unexpected speed match timeout");
                break;
        }
        
        if (speedMatcherState != SpeedMatcherState.IDLE) {
            speedMatchStateTimer.start();
        }
    }
    
     protected enum SpeedMatcherState {
        IDLE {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return this;
            }
        },
        WAIT_FOR_THROTTLE {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.IDLE;
            }
        },
        INIT_ACCEL {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.INIT_DECEL;
            }
        },
        INIT_DECEL {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.INIT_VSTART;
            }
        },
        INIT_VSTART {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.INIT_VMID;
            }
        },
        INIT_VMID {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.INIT_VHIGH;
            }
        },
        INIT_VHIGH {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.INIT_FORWARD_TRIM;
            }
        },
        INIT_FORWARD_TRIM {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.INIT_REVERSE_TRIM;
            }
        },
        INIT_REVERSE_TRIM {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                if (speedMatcher.warmUpLocomotive) {
                    return SpeedMatcherState.FORWARD_WARM_UP;
                } else {
                    return SpeedMatcherState.FORWARD_SPEED_MATCH_VHIGH;
                }
            }
        },
        FORWARD_WARM_UP {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.FORWARD_SPEED_MATCH_VHIGH;
            }
        },
        FORWARD_SPEED_MATCH_VHIGH {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.FORWARD_SPEED_MATCH_VMID;
            }
        },
        FORWARD_SPEED_MATCH_VMID {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.FORWARD_SPEED_MATCH_VSTART;
            }
        },
        FORWARD_SPEED_MATCH_VSTART {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                if (speedMatcher.trimReverseSpeed) {
                    return SpeedMatcherState.REVERSE_WARM_UP;
                } else {
                    return SpeedMatcherState.SET_ACCEL;
                }
            }
        },
        REVERSE_WARM_UP {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.REVERSE_SPEED_MATCH_TRIM;
            }
        },
        REVERSE_SPEED_MATCH_TRIM {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.SET_ACCEL;
            }
        },
        SET_ACCEL {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.SET_DECEL;
            }
        },
        SET_DECEL {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.CLEAN_UP;
            }
        },
        CLEAN_UP {
            @Override
            protected SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher) {
                return SpeedMatcherState.IDLE;
            }
        };

        protected abstract SpeedMatcherState nextState(BasicSimpleCVSpeedMatcher speedMatcher);
    }
    
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Programmer">
    /**
     * Starts writing acceleration momentum (CV 3) using the ops mode programmer
     * @param value acceleration value (0-255 inclusive)
     */ 
    private synchronized void writeMomentumAccel(int value) {
        programmerState = ProgrammerState.WRITE3;
        statusLabel.setText(Bundle.getMessage("ProgSetAccel", value));
        startOpsModeWrite("3", value);
    }
    
    /**
     * Starts writing deceleration momentum (CV 4) using the ops mode programmer
     * @param value deceleration value (0-255 inclusive)
     */
    protected synchronized void writeMomentumDecel(int value) {
        programmerState = ProgrammerState.WRITE4;
        statusLabel.setText(Bundle.getMessage("ProgSetDecel", value));
        startOpsModeWrite("4", value);
    }
    
    /**
     * Starts writing vStart (CV 2) using the ops mode programmer
     * @param value vStart value (0-255 inclusive)
     */
    protected synchronized void writeVStart(int value) {
        programmerState = ProgrammerState.WRITE2;
        statusLabel.setText(Bundle.getMessage("ProgSetVStart", value));
        startOpsModeWrite("2", value);
    }
    
    /**
     * Starts writing vMid (CV 6) using the ops mode programmer
     * @param value vMid value (0-255 inclusive)
     */
    protected synchronized void writeVMid(int value) {
        programmerState = ProgrammerState.WRITE6;
        statusLabel.setText(Bundle.getMessage("ProgSetVMid", value));
        startOpsModeWrite("6", value);
    }
    
    /**
     * Starts writing vHigh (CV 5) using the ops mode programmer
     * @param value vHigh value (0-255 inclusive)
     */
    protected synchronized void writeVHigh(int value) {
        programmerState = ProgrammerState.WRITE5;
        statusLabel.setText(Bundle.getMessage("ProgSetVHigh", value));
        startOpsModeWrite("5", value);
    }
    
    /**
     * Starts writing forward trim (CV 66) using the ops mode programmer
     * @param value forward trim value (0-255 inclusive)
     */
    protected synchronized void writeForwardTrim(int value) {
        programmerState = ProgrammerState.WRITE66;
        statusLabel.setText(Bundle.getMessage("ProgSetForwardTrim", value));
        startOpsModeWrite("66", value);
    }
    
    /**
     * Starts writing reverse trim (CV 95) using the ops mode programmer
     * @param value reverse trim value (0-255 inclusive)
     */
    protected synchronized void writeReverseTrim(int value) {
        programmerState = ProgrammerState.WRITE95;
        statusLabel.setText(Bundle.getMessage("ProgSetReverseTrim", value));
        startOpsModeWrite("95", value);
    }
    
    /**
     * Starts writing a CV using the ops mode programmer (Programming on the Main)
     *
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
     *
     * @param value  Value from a read operation, or value written on a write
     * @param status Denotes the completion code. Note that this is a bitwise
     *               combination of the various states codes defined in this
     *               interface. (see ProgListener.java for possible values)
     */
    @Override
    public void programmingOpReply(int value, int status) {
        if (status == 0) {
            switch (programmerState) {
                case IDLE:
                    logger.debug("unexpected reply in IDLE state");
                    break;
                    
                case WRITE2:
                case WRITE3:
                case WRITE4:
                case WRITE5:
                case WRITE6:
                case WRITE66:
                case WRITE95:
                    programmerState = ProgrammerState.IDLE;
                    break;
                    
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
        IDLE,
        WRITE2,
        WRITE3,
        WRITE4,
        WRITE5,
        WRITE6,
        WRITE66,
        WRITE95,
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="ThrottleListener Overrides">
    /**
     * Called when a throttle is found
     *
     * @param t the requested DccThrottle
     */
    @Override
    public void notifyThrottleFound(DccThrottle t) {
        super.notifyThrottleFound(t);

        if (speedMatcherState == SpeedMatcherState.WAIT_FOR_THROTTLE) {
            logger.info("Starting speed matching");

            // using speed matching timer to trigger each phase of speed matching            
            setupNextSpeedMatchState(true, 0);
            speedMatchStateTimer.start();
        } else {
            CleanUp();
        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Helper Functions">
    private void Abort() {
        speedMatcherState = SpeedMatcherState.SET_ACCEL;
        setupNextSpeedMatchState(true, 0);
    }
    
    private void setupNextSpeedMatchState(boolean isForward, int speedStep) {
        speedMatcherState = speedMatcherState.nextState(this);
        setupSpeedMatchState(isForward, speedStep, 1500);
    }
    //</editor-fold>
    
}
