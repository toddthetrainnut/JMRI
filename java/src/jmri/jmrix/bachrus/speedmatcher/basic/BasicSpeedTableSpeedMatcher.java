package jmri.jmrix.bachrus.speedmatcher.basic;

import jmri.DccThrottle;
import jmri.jmrix.bachrus.speedmatcher.SpeedMatcherConfig;

/**
 *
 * @author toddt
 */
public class BasicSpeedTableSpeedMatcher extends BasicSpeedMatcher {
    
    //<editor-fold defaultstate="collapsed" desc="Constants">
    private final int INITIAL_SPEED_TABLE_STEP = 1;
    private final int INITIAL_STEP28 = 255;
    private final int INITIAL_TRIM = 128;
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Instance Variables">
    private int speedTableStepValue = INITIAL_STEP28;
    private int lastSpeedTableStepValue = INITIAL_STEP28;
    private int reverseTrimValue = INITIAL_TRIM;
    private int lastReverseTrimValue = INITIAL_TRIM;
    
    private SpeedMatcherState speedMatcherState = SpeedMatcherState.IDLE;
    //</editor-fold>
    
    public BasicSpeedTableSpeedMatcher(SpeedMatcherConfig config) {
        super (config);
    }

    //<editor-fold defaultstate="collapsed" desc="SpeedMatcher Overrides">
    @Override
    public boolean StartSpeedMatch() {
        
        if (!super.Validate()) {
            return false;
        }
        
        //reset instance variables
        
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
    
    /**
     * Timer timeout handler for the speed match timer
     */
    private synchronized void speedMatchTimeout() {
        switch(speedMatcherState) { 
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
                
            //TODO: TRW - add case to set speed step mode to use Speed Table

            case INIT_SPEED_TABLE_STEPS:
                //TODO: TRW - implementation
                if (programmerState == programmerState.IDLE) {
                    String speedTableStepValue = currentSpeedTableStep == SpeedTableStep.STEP28 ? INITIAL_STEP28 : INITIAL_SPEED_TABLE_STEP;
                    writeSpeedTableStep(currentSpeedTableStep, speedTableStepValue);
                    setupNextSpeedMatchState(true, 0);
                }
                break;

            case INIT_FORWARD_TRIM:
                //set forward trim to 128 (CV 66)
                if (programmerState == ProgrammerState.IDLE) {
                    writeForwardTrim(INITIAL_TRIM);
                    setupNextSpeedMatchState(true, 0);
                }
                break;

            case INIT_REVERSE_TRIM:
                //set reverse trim to 128 (CV 95)
                if (programmerState == ProgrammerState.IDLE) {
                    writeReverseTrim(INITIAL_TRIM);
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

            case FORWARD_SPEED_MATCH:
                //TODO: TRW - implementation
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
    }
    
    protected enum SpeedMatcherState{
        IDLE {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return this;
            }
        },
        WAIT_FOR_THROTTLE {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.IDLE;
            }
        },
        INIT_ACCEL {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.INIT_DECEL;
            }
        },
        INIT_DECEL {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.INIT_SPEED_TABLE_STEPS;
            }
        },
        INIT_SPEED_TABLE_STEPS {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                if (currentSpeedTableStep == SpeedTableStep.STEP28) {
                    currentSpeedTableStep = SpeedTableStep.STEP1;
                    return SpeedMatcherState.INIT_FORWARD_TRIM;
                }
                
                
                return this;
            }
        },
        INIT_FORWARD_TRIM {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.INIT_REVERSE_TRIM;
            }
        },
        INIT_REVERSE_TRIM {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                if (speedMatcher.warmUpLocomotive) {
                    return SpeedMatcherState.FORWARD_WARM_UP;
                } else {
                    return SpeedMatcherState.FORWARD_SPEED_MATCH;
                }
            }
        }, 
        FORWARD_WARM_UP {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.FORWARD_SPEED_MATCH;
            }
        },
        FORWARD_SPEED_MATCH {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                if (currentSpeedTableStep == SpeedTableStep.STEP28) {
                    currentSpeedTableStep = SpeedTableStep.STETP1;
                    if (speedMatcher.trimReverseSpeed) {
                        return SpeedMatcherState.REVERSE_WARM_UP;
                    }
                    else {
                        return SpeedMatcherState.SET_ACCEL;
                    }
                }
                return this;
            }
        },
         REVERSE_WARM_UP {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.REVERSE_SPEED_MATCH_TRIM;
            }
        },
        REVERSE_SPEED_MATCH_TRIM {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.SET_ACCEL;
            }
        },
        SET_ACCEL {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.SET_DECEL;
            }
        },
        SET_DECEL {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.CLEAN_UP;
            }
        },
        CLEAN_UP {
            @Override
            protected SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher) {
                return SpeedMatcherState.IDLE;
            }
        };
            
        protected abstract SpeedMatcherState nextState(BasicSpeedTableSpeedMatcher speedMatcher);  
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Programmer">
    
    /**
     * Starts writing a Speed Table Step CV (CV 67-94) using the ops mode programmer
     * @param value speed table step value (0-255 inclusive)
     */
    private synchronized void writeSpeedTableStep(SpeedTableStep step, int value) {
        programmerState = ProgrammerState.WRITE_SPEED_TABLE_STEP;
        statusLabel.setText(String.format("Setting Speed Table Step %s to %s", step.getName(), value));
        startOpsModeWrite(step.getCV(), value);
    }
    
    //</editor-fold>
      
    //<editor-fold defaultstate="collapsed" desc="ThrottleListener Overrides">
    
    /**
     * Called when a throttle is found
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
