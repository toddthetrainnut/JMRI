package jmri.jmrix.bachrus.speedmatcher.basic;

import jmri.jmrix.bachrus.Speed;
import jmri.jmrix.bachrus.speedmatcher.SpeedMatcher;
import jmri.jmrix.bachrus.speedmatcher.SpeedMatcherConfig;

/**
 *
 * @author toddt
 */
public abstract class BasicSpeedMatcher extends SpeedMatcher{
        
    
    //<editor-fold defaultstate="collapsed" desc="Instance Variables">
    protected float targetStartSpeedKPH;
    protected float targetTopSpeedKPH;
    //</editor-fold>
    
    public BasicSpeedMatcher(SpeedMatcherConfig config) {
        super(config);
        
        if (config.speedUnit == Speed.Unit.MPH) {
            this.targetStartSpeedKPH = Speed.mphToKph(config.targetStartSpeed);
            this.targetTopSpeedKPH = Speed.mphToKph(config.targetTopSpeed);
        } else {
            this.targetStartSpeedKPH = config.targetStartSpeed;
            this.targetTopSpeedKPH = config.targetTopSpeed;
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="Protected APIs">
    @Override
    protected boolean Validate() {
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
    //</editor-fold>
}
