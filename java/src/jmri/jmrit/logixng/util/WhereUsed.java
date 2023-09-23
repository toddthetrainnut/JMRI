package jmri.jmrit.logixng.util;

import java.util.ArrayList;
import java.util.List;

import jmri.InstanceManager;
import jmri.NamedBean;
import jmri.NamedBeanUsageReport;
import jmri.Reference;
import jmri.jmrit.logixng.*;

/**
 * List the places where an item is used.
 *
 * @author Daniel Bergqvist (C) 2023
 */
public class WhereUsed {


    private static final String PAD = "   ";


    private WhereUsed() {
    }


    public static String whereUsed(NamedBean bean) {

        Reference<String> ref = new Reference<>();

        // Iterate all the LogixNGs
        InstanceManager.getDefault(LogixNG_Manager.class).getNamedBeanSet().forEach((logixNG) -> {
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < logixNG.getNumConditionalNGs(); i++) {
                ConditionalNG conditionalNG = logixNG.getConditionalNG(i);
                String s = checkFemaleSocket(conditionalNG.getFemaleSocket(), bean, conditionalNG, PAD+PAD);
                if (s != null) {
                    sb.append(PAD);
                    sb.append(conditionalNG.getLongDescription());
                    sb.append(String.format("%n"));
                    sb.append(s);
                }
            }
            if (sb.length() > 0) {
                String str = logixNG.getLongDescription() + String.format("%n") + sb.toString() + String.format("%n");
                if (ref.get() != null) ref.set(ref.get() + str);
                else ref.set(str);
            }
        });

        // Iterate all the Modules
        InstanceManager.getDefault(ModuleManager.class).getNamedBeanSet().forEach((module) -> {
            String s = checkFemaleSocket(module.getRootSocket(), bean, null, PAD);
            if (s != null) {
                String str = module.getLongDescription() + String.format("%n") + s + String.format("%n");
                if (ref.get() != null) ref.set(ref.get() + str);
                else ref.set(str);
            }
        });

        String result = ref.get();

        // Iterate the Clipboard
        String s = checkFemaleSocket(InstanceManager.getDefault(LogixNG_Manager.class).getClipboard().getFemaleSocket(), bean, null, PAD);
        if (s != null) {
            String str = Bundle.getMessage("Clipboard") + String.format("%n") + s;
            if (result != null) result += str;
            else result = str;
        }

        return result != null ? result : "";
    }

    private static String checkFemaleSocket(FemaleSocket femaleSocket, NamedBean bean, ConditionalNG conditionalNG, String pad) {

        if (!femaleSocket.isConnected()) return null;

        Base b = femaleSocket.getConnectedSocket();

        while (b instanceof MaleSocket) {
            b = ((MaleSocket)b).getObject();
        }

        if (b == null) {
            throw new UnsupportedOperationException("object is null");
        }

        List<NamedBeanUsageReport> report = new ArrayList<>();
        b.getUsageDetail(0, bean, report, conditionalNG);

        String s = null;
        if (!report.isEmpty()) {
            s = pad + femaleSocket.getLongDescription() + String.format("%n");
            s += pad + PAD + b.getLongDescription() + String.format("   <<====%n");
            pad += PAD;
        }


        String padding = s != null ? pad+PAD : pad+PAD+PAD;

        for (int i=0; i < b.getChildCount(); i++) {
            String temp = checkFemaleSocket(b.getChild(i), bean, conditionalNG, padding);
            if (temp != null) {
                if (s == null) {
                    s = pad + femaleSocket.getLongDescription() + String.format("%n");
                    s += pad + PAD + b.getLongDescription() + String.format("%n");
                    pad += PAD;
                }
                s += temp;
            }
        }

        return s;
    }

}
