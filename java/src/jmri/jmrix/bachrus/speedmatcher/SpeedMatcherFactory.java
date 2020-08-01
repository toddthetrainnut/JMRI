/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmri.jmrix.bachrus.speedmatcher;


/**
 *
 * @author Todd Wegter
 */
public class SpeedMatcherFactory {
    
    
    public static ISpeedMatcher getSpeedMatcher(SpeedMatcherConfig config) {
        
        ISpeedMatcher speedMatcher;
        
        switch (config.type) {
            case BASIC:
                switch (config.speedTable) {
                    default:
                        speedMatcher = new BasicStartMidHighSpeedMatcher(config);
                        break;
                }
                break;
//            case SPEEDSTEPSCALE:
//                switch (config.speedTable) {
//                    case SPEEDSTEPSCALE:
//                        break;
//                    case COMBO:
//                        break;
//                }
//                break;
            default:
                speedMatcher = new BasicStartMidHighSpeedMatcher(config);
                break;
        }
        
        return speedMatcher;
    }
}
