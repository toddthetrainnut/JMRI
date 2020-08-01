/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmri.jmrix.bachrus.speedmatcher;

import javax.swing.JLabel;
import jmri.DccLocoAddress;
import jmri.PowerManager;
import jmri.jmrix.bachrus.Speed;
import org.slf4j.Logger;

/**
 *
 * @author toddt
 */
public class SpeedMatcherConfig {
    
    public enum SpeedMatcherType {
        BASIC, SPEEDSTEPSCALE
    }
    
    public enum SpeedTable {
        SIMPLE, ADVANCED, COMBO
    }
    
    protected SpeedMatcherType type;
    protected SpeedTable speedTable; 
    
    protected DccLocoAddress dccLocoAddress;
    protected Logger logger;
    protected PowerManager powerManager;
    
    protected JLabel statusLabel;
    protected float targetStartSpeed;
    protected float targetTopSpeed;
    protected Speed.Unit speedUnit;
    protected boolean warmUpLoco;
    protected boolean trimReverseSpeed; 
    protected int acceleration;
    protected int deceleration;
    
    //Do not use
    private SpeedMatcherConfig() {
        
    }
    
    public SpeedMatcherConfig(
            SpeedMatcherType type, 
            SpeedTable speedTable, 
            DccLocoAddress address, 
            float targetStartSpeed, 
            float targetTopSpeed, 
            Speed.Unit speedUnit, 
            boolean warmUpLoco, 
            boolean trimReverseSpeed, 
            int acceleration,
            int deceleration,
            PowerManager powerManager, 
            Logger logger, 
            JLabel statusLabel
    ) {
        this.type = type;
        this.speedTable = speedTable;
        this.targetStartSpeed = targetStartSpeed;
        this.targetTopSpeed = targetTopSpeed;
        this.speedUnit = speedUnit;
        
        this.warmUpLoco = warmUpLoco;
        this.trimReverseSpeed = trimReverseSpeed;
        this.acceleration = acceleration;
        this.deceleration = deceleration;
        
        this.dccLocoAddress = address;
        this.powerManager = powerManager;
        
        this.logger = logger;
        this.statusLabel = statusLabel;
    }
    
}
