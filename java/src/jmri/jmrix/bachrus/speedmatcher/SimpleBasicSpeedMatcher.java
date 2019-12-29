package jmri.jmrix.bachrus.speedmatcher;

import jmri.AddressedProgrammer;
import jmri.AddressedProgrammerManager;
import jmri.DccLocoAddress;
import jmri.InstanceManager;
import jmri.jmrix.bachrus.Speed;

/**
 *
 * @author Todd Wegter
 */
public class SimpleBasicSpeedMatcher extends BasicSpeedMatcher{
    protected final int VSTART = 1;
    protected final int VHIGH = 255;
    protected final int REVERSE_TRIM_VALUE = 128;
    
    protected int vStart = VSTART;
    protected int lastVStart = VSTART;
    protected int vHigh = VHIGH;
    protected int lastVHigh = VHIGH;
    protected int reverseTrimValue = REVERSE_TRIM_VALUE;
    protected int lastReverseTrimValue = REVERSE_TRIM_VALUE;
    
    protected AddressedProgrammer opsModeProgrammer;
    
    protected State state = State.IDLE;
    
    //Do not use
    private SimpleBasicSpeedMatcher() {
        
    }
    
    public SimpleBasicSpeedMatcher(SpeedMatcherConfig config) {
        this.dccLocoAddress = config.dccLocoAddress;
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
    }
    
    @Override
    public boolean StartSpeedMatch(String error) {
        if (!super.Validate(error)) {
            return false;
        }
        
        //reset instance variables
        vStart = VSTART;
        lastVStart = VSTART;
        vHigh = VHIGH;
        lastVHigh = VHIGH;
        reverseTrimValue = REVERSE_TRIM_VALUE;
        lastReverseTrimValue = REVERSE_TRIM_VALUE;
        
        state = State.WAIT_FOR_THROTTLE;
        
        //get OPS MODE Programmer
        if (InstanceManager.getNullableDefault(AddressedProgrammerManager.class) != null) {
            if (InstanceManager.getDefault(AddressedProgrammerManager.class).isAddressedModePossible(dccLocoAddress)) {
                opsModeProgrammer = InstanceManager.getDefault(AddressedProgrammerManager.class).getAddressedProgrammer(dccLocoAddress);
            }
        }
        
        return true;
    }
    
    @Override
    public boolean IsIdle() {
        return state == State.IDLE;
    }
    
    protected enum State {
        IDLE {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return this;
           } 
        },
        WAIT_FOR_THROTTLE {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.INIT_ACCEL;
           } 
        },
        INIT_ACCEL {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.INIT_DECEL;
           } 
        },
        INIT_DECEL {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.INIT_VSTART;
           } 
        },
        INIT_VSTART {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.INIT_VMID;
           } 
        },
        INIT_VMID {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.INIT_VHIGH;
           } 
        },
        INIT_VHIGH {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.INIT_FORWARD_TRIM;
           } 
        },
        INIT_FORWARD_TRIM {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.INIT_REVERSE_TRIM;
           } 
        },
        INIT_REVERSE_TRIM {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               if (speedMatcher.warmUpLocomotive) {
                   return State.FORWARD_WARM_UP;
               } else {
                   return State.FORWARD_SPEED_MATCH_VHIGH;
               }
           } 
        },
        FORWARD_WARM_UP {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               if (speedMatcher.trimReverseSpeed) {
                   return State.REVERSE_WARM_UP;
               } else {
                   return State.FORWARD_SPEED_MATCH_VHIGH;
               }
           } 
        },
        REVERSE_WARM_UP {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.FORWARD_SPEED_MATCH_VHIGH;
           } 
        },
        FORWARD_SPEED_MATCH_VHIGH {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.FORWARD_SPEED_MATCH_VSTART;
           } 
        },
        FORWARD_SPEED_MATCH_VSTART {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.FORWARD_SPEED_MATCH_VMID;
           } 
        },
        FORWARD_SPEED_MATCH_VMID {
            @Override
            protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
                if (speedMatcher.trimReverseSpeed) {
                    return State.REVERSE_SPEED_MATCH_TRIM;
                } else {
                    return State.SET_ACCEL;
                }
            }
        },
        REVERSE_SPEED_MATCH_TRIM {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.SET_ACCEL;
           } 
        },
        SET_ACCEL {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.SET_DECEL;
           } 
        },
        SET_DECEL {
           @Override
           protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
               return State.CLEAN_UP;
           } 
        },
        CLEAN_UP {
          @Override
          protected State nextState(SimpleBasicSpeedMatcher speedMatcher) {
              return State.IDLE;
          }
        };
        
        protected abstract State nextState(SimpleBasicSpeedMatcher speedMatcher);
    }
    
}
