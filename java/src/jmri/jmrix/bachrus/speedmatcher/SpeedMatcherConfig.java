/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmri.jmrix.bachrus.speedmatcher;

import jmri.DccLocoAddress;
import jmri.jmrix.bachrus.Speed;

/**
 *
 * @author toddt
 */
public class SpeedMatcherConfig {
    
    public enum SpeedMatcherType {
        BASIC, ADVANCED
    }
    
    public enum SpeedTable {
        BASIC, ADVANCED, COMBO
    }
    
    protected SpeedMatcherType type;
    protected SpeedTable speedTable; 
    
    protected DccLocoAddress dccLocoAddress;
    protected float targetStartSpeed;
    protected float targetTopSpeed;
    protected Speed.Unit speedUnit;
    protected boolean warmUpLoco;
    protected boolean trimReverseSpeed; 
    
    //Do not use
    private SpeedMatcherConfig() {
        
    }
    
    public SpeedMatcherConfig(SpeedMatcherType type, SpeedTable speedTable, DccLocoAddress address, float targetStartSpeed, float targetTopSpeed, Speed.Unit speedUnit, boolean warmUpLoco, boolean trimReverseSpeed) {
        this.type = type;
        this.speedTable = speedTable;
        this.dccLocoAddress = address;
        this.targetStartSpeed = targetStartSpeed;
        this.targetTopSpeed = targetTopSpeed;
        this.speedUnit = speedUnit;
        this.warmUpLoco = warmUpLoco;
        this.trimReverseSpeed = trimReverseSpeed;
    }
    
}
