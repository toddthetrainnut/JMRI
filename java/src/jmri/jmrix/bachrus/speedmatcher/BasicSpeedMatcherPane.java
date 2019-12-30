/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmri.jmrix.bachrus.speedmatcher;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 *
 * @author toddt
 */
public class BasicSpeedMatcherPane extends JPanel{
    
    //TODO: reformat for I18N - TRW
    protected JLabel basicSpeedMatchInfo = new JLabel("<html><p>" + 
            "You may need to adjust some of the provided settings since different decoder manufacturers interpret the NMRA standards differently." +
            "<br/><br/>Settings for some common manufactures:" +
            "<br/><ul>" +
            "<li>NCE - simple or complex speed table, disable Trim Reverse Speed</li>" +
            "<li>Digitrax - complex speed table only, enable or disable Trim Reverse Speed</li>" +
            "<li>ESU - simple speed table only, enable Trim Reverse Speed</li>" +
            "<li>SoundTraxx - simple or complex speed table, enable or disable Trim Reverse Speed</li>" +
            "</ul>" +
            "It is recommended to enable Warm Up Locomotive if your locomotive isn't already warmed up to help achieve a more accurate result." +
            "<br/><br/></p></html>");
    
    //TODO: I18N
    protected JLabel speedMatchForwardMomentumLabel = new JLabel("Forward Momentum: ");
    protected JTextField speedMatchForwardMomentumField = new JTextField(3);
    protected JLabel speedMatchReverseMomentumLabel = new JLabel("Reverse Momentum: ");
    protected JTextField speedMatchReverseMomentumField = new JTextField(3);
    protected JCheckBox speedMatchReverseCheckbox = new JCheckBox("Trim Reverse Speed");
    protected ButtonGroup speedMatchTableTypeGroup = new ButtonGroup();
    protected JRadioButton speedMatchSimpleTableButton = new JRadioButton("Simple (CV 2, CV 6, and CV 5)");
    protected JRadioButton speedMatchComplexTableButton = new JRadioButton("Complex Speed Table");
    
    protected JLabel speedStep1TargetLabel = new JLabel(Bundle.getMessage("lblStartSpeed"));
    protected JTextField targetStartSpeedField = new JTextField("3", 3);
    protected JLabel speedStep1TargetUnit = new JLabel(Bundle.getMessage("lblMPH"));
    protected JLabel speedStep28TargetLabel = new JLabel(Bundle.getMessage("lblTopSpeed"));
    protected JTextField targetHighSpeedField = new JTextField("55", 3);
    protected JLabel speedStep28TargetUnit = new JLabel(Bundle.getMessage("lblMPH"));
    protected JCheckBox speedMatchWarmUpCheckBox = new JCheckBox(Bundle.getMessage("chkbxWarmUp"));
    protected JButton speedMatchButton = new JButton(Bundle.getMessage("btnStartSpeedMatch"));
   
    
    public BasicSpeedMatcherPane(Runnable stopFunction) {
        super();
        
        speedMatchForwardMomentumField.setHorizontalAlignment(JTextField.RIGHT);
        speedMatchReverseMomentumField.setHorizontalAlignment(JTextField.RIGHT);
        speedMatchReverseCheckbox.setSelected(true);
        speedMatchTableTypeGroup.add(speedMatchSimpleTableButton);
        //TOOD: I18N
        speedMatchSimpleTableButton.setToolTipText("Set the simple speed table. Faster than setting the complex speed table.");
        speedMatchTableTypeGroup.add(speedMatchComplexTableButton);
        speedMatchComplexTableButton.setToolTipText("Set the complex speed table. Some decoders will only respect the trim CVs if the complex speed table is used.");
        speedMatchSimpleTableButton.setSelected(true);
        
        targetStartSpeedField.setHorizontalAlignment(JTextField.RIGHT);
        speedStep1TargetUnit.setPreferredSize(new Dimension(35, 16));
        targetHighSpeedField.setHorizontalAlignment(JTextField.RIGHT);
        speedStep28TargetUnit.setPreferredSize(new Dimension(35, 16));
        speedMatchWarmUpCheckBox.setSelected(true);
        
        this.setLayout(new BorderLayout());
        JPanel basicSpeedMatchSettingsPane = new JPanel();
        basicSpeedMatchSettingsPane.setLayout(new BoxLayout(basicSpeedMatchSettingsPane, BoxLayout.PAGE_AXIS));
        
        //Important Information
        JPanel speedMatchImportantInfoPane = new JPanel();
        speedMatchImportantInfoPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Important Information"));
        speedMatchImportantInfoPane.setLayout(new BoxLayout(speedMatchImportantInfoPane, BoxLayout.LINE_AXIS));
        speedMatchImportantInfoPane.add(basicSpeedMatchInfo);
//        speedMatchImportantInfoPane.add(basicSpeedMatchInfo2);
//        speedMatchImportantInfoPane.add(basicSpeedMatchInfoNCE);
//        speedMatchImportantInfoPane.add(basicSpeedMatchInfoDigitrax);
//        speedMatchImportantInfoPane.add(basicSpeedMatchInfoESU);
//        speedMatchImportantInfoPane.add(basicSpeedMatchInfoSoundtraxx);
        basicSpeedMatchSettingsPane.add(speedMatchImportantInfoPane);
        
        //Speed Table Type
        JPanel speedMatchTableTypePane = new JPanel();
        speedMatchTableTypePane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Select the Desired Speed Table"));
        speedMatchTableTypePane.setLayout(new FlowLayout());
        speedMatchTableTypePane.add(speedMatchSimpleTableButton);
        speedMatchTableTypePane.add(speedMatchComplexTableButton);
        basicSpeedMatchSettingsPane.add(speedMatchTableTypePane);
        
        //Other Settings
        JPanel speedMatchOtherSettingsPane = new JPanel();
        speedMatchOtherSettingsPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Other Speed Matching Settings"));
        speedMatchOtherSettingsPane.setLayout(new FlowLayout());
        speedMatchOtherSettingsPane.add(speedMatchWarmUpCheckBox);
        speedMatchOtherSettingsPane.add(speedMatchReverseCheckbox);
        basicSpeedMatchSettingsPane.add(speedMatchOtherSettingsPane);
        
        //Momentum
        JPanel speedMatchMomentumPane = new JPanel();
        speedMatchMomentumPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Set Momentum (If Desired)"));
        speedMatchMomentumPane.setLayout(new FlowLayout());
        speedMatchMomentumPane.add(speedMatchForwardMomentumLabel);
        speedMatchMomentumPane.add(speedMatchForwardMomentumField);
        speedMatchMomentumPane.add(speedMatchReverseMomentumLabel);
        speedMatchMomentumPane.add(speedMatchReverseMomentumField);
        basicSpeedMatchSettingsPane.add(speedMatchMomentumPane);
        
        //Speed Settings
        JPanel speedMatchSpeedPane = new JPanel();
        speedMatchSpeedPane.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        speedMatchSpeedPane.add(speedStep1TargetLabel, gbc);
        speedMatchSpeedPane.add(targetStartSpeedField, gbc);
        speedMatchSpeedPane.add(speedStep1TargetUnit, gbc);
        speedMatchSpeedPane.add(speedStep28TargetLabel, gbc);
        speedMatchSpeedPane.add(targetHighSpeedField, gbc);
        speedMatchSpeedPane.add(speedStep28TargetUnit, gbc);
        speedMatchSpeedPane.add(speedMatchButton, gbc);
        
        this.add(basicSpeedMatchSettingsPane, BorderLayout.NORTH);
        this.add(speedMatchSpeedPane, BorderLayout.CENTER);
        
              // Listen to speed match button
        basicSpeedMatcherPane.addSpeedButtonActionListener(e -> {
            float targetStartSpeedKPH;
            float targetHighSpeedKPH;
            boolean speedMatchReverse;
            boolean warmUpLoco;
            String error = "";
            
            if ((speedMatcher != null && speedMatcher.IsIdle()) && (profileState == ProfileState.IDLE)) {
                getCustomScale();
                targetStartSpeedKPH = Integer.parseInt(targetStartSpeedField.getText());
                targetHighSpeedKPH = Integer.parseInt(targetHighSpeedField.getText());

                if (mphButton.isSelected()) {
                    targetStartSpeedKPH = Speed.mphToKph(targetStartSpeedKPH);
                    targetHighSpeedKPH = Speed.mphToKph(targetHighSpeedKPH);
                }
                
                //TODO: get complex/simple
                speedMatchReverse = speedMatchReverseCheckbox.isSelected();
                warmUpLoco = speedMatchWarmUpCheckBox.isSelected();
                
                speedMatcher = SpeedMatcherFactory.getSpeedMatcher(
                        new SpeedMatcherConfig(
                                SpeedMatcherConfig.SpeedMatcherType.BASIC, 
                                SpeedMatcherConfig.SpeedTable.BASIC, 
                                locomotiveAddress, 
                                targetStartSpeedKPH, 
                                targetHighSpeedKPH, 
                                Speed.Unit.KPH, 
                                warmUpLoco, 
                                speedMatchReverse
                        )
                );

                if (speedMatcher.StartSpeedMatch(error)) {
                    speedMatchButton.setText(Bundle.getMessage("btnStopSpeedMatch"));
                } else {
                    statusLabel.setText(error);
                }
            } else {
//                stopProfileAndSpeedMatch();
                stopFunction.run();
            }
        });
    }
}
