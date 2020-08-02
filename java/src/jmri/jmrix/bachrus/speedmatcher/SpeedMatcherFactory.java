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
    
    
    public static SpeedMatcher getSpeedMatcher(SpeedMatcherConfig config) {
        
        SpeedMatcher speedMatcher;
        
        switch (config.type) {
            case BASIC:
                switch (config.speedTable) {
                    //TODO: resepect different types
                    default:
                        speedMatcher = new BasicSimpleCVSpeedMatcher(config);
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
                speedMatcher = new BasicSimpleCVSpeedMatcher(config);
                break;
        }
        
        return speedMatcher;
    }
}
