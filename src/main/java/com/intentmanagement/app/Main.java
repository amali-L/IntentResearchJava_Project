package com.intentmanagement.app;

import com.intentmanagement.core.*;import com.intentmanagement.knowledge.*;import com.intentmanagement.reporting.ReportService;import com.intentmanagement.ui.Dashboard;import com.intentmanagement.dataset.DatasetPipeline;import com.intentmanagement.dataset.ServiceMatchingEvaluator;import java.nio.file.*;import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if(Arrays.asList(args).contains("--self-test")){SelfTest.run();return;}
        if(Arrays.asList(args).contains("--cli-demo")){cliDemo();return;}
        if(Arrays.asList(args).contains("--evaluate-matching")){try(JenaKnowledgeBase kb=new JenaKnowledgeBase(Path.of("data","ontology","intent-schema.ttl"))){var eval=new ServiceMatchingEvaluator();var m=eval.evaluate(Path.of("data","datasets","labelled_intent_matching.csv"),kb);String report=eval.format(m);System.out.println(report);Files.createDirectories(Path.of("results"));Files.writeString(Path.of("results","service-matching-evaluation.txt"),report); }return;}
        if(Arrays.asList(args).contains("--validate-ontology")){try(JenaKnowledgeBase kb=new JenaKnowledgeBase(Path.of("data","ontology","intent-schema.ttl"))){System.out.println(kb.validateWithShacl(Path.of("data","ontology","shapes","service-shapes.ttl")));}return;}
        if(Arrays.asList(args).contains("--rdf-query")){String q=args.length>1?args[1]:"Emergency";try(JenaKnowledgeBase kb=new JenaKnowledgeBase(Path.of("data","ontology","intent-schema.ttl"))){System.out.println(kb.queryFacts(q));}return;}
        for(int i=0;i<args.length;i++) if(args[i].equals("--generate-dataset")) {long n=i+1<args.length?Long.parseLong(args[i+1]):100000;var r=new DatasetPipeline().generateAndPreprocess(n,Path.of("data","datasets"));System.out.println("Dataset pipeline complete: raw="+r.rawRows()+", preprocessed="+r.cleanRows()+", triples="+r.triples()+", folder="+r.directory().toAbsolutePath());return;}
        if(Arrays.asList(args).contains("--kg-query")){int i=Arrays.asList(args).indexOf("--kg-query");String q=i+1<args.length?args[i+1]:"Emergency";System.out.println(new IntentManager(100).graph.queryText(q));return;}
        javax.swing.SwingUtilities.invokeLater(()->{
            configureDialogTheme();
            new com.intentmanagement.ui.LoginDialog().show(()->new Dashboard(new IntentManager(100)).setVisible(true));
        });
    }

    /** Apply the application red/navy theme to all Swing option dialogs. */
    private static void configureDialogTheme() {
        java.awt.Color navy = new java.awt.Color(13, 20, 35);
        java.awt.Color panel = new java.awt.Color(22, 32, 50);
        java.awt.Color white = new java.awt.Color(245, 248, 255);
        java.awt.Color muted = new java.awt.Color(190, 204, 224);
        java.awt.Color red = new java.awt.Color(225, 29, 52);
        javax.swing.UIManager.put("OptionPane.background", navy);
        javax.swing.UIManager.put("Panel.background", navy);
        javax.swing.UIManager.put("OptionPane.messageForeground", white);
        javax.swing.UIManager.put("OptionPane.foreground", white);
        javax.swing.UIManager.put("OptionPane.border", javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(55, 75, 105), 1),
                javax.swing.BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        javax.swing.UIManager.put("Button.background", panel);
        javax.swing.UIManager.put("Button.foreground", white);
        javax.swing.UIManager.put("Button.select", red);
        javax.swing.UIManager.put("Button.focus", new java.awt.Color(255, 80, 100));
        javax.swing.UIManager.put("Button.border", javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(80, 105, 140)),
                javax.swing.BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        javax.swing.UIManager.put("Label.foreground", white);
        javax.swing.UIManager.put("OptionPane.buttonAreaBackground", navy);
        javax.swing.UIManager.put("OptionPane.titleText", white);
        javax.swing.UIManager.put("TextField.background", panel);
        javax.swing.UIManager.put("TextField.foreground", white);
        javax.swing.UIManager.put("TextField.caretForeground", white);
        javax.swing.UIManager.put("TextField.selectionBackground", red);
        javax.swing.UIManager.put("TextField.selectionForeground", white);
        javax.swing.UIManager.put("ComboBox.background", panel);
        javax.swing.UIManager.put("ComboBox.foreground", white);
        javax.swing.UIManager.put("ToolTip.background", panel);
        javax.swing.UIManager.put("ToolTip.foreground", white);
        javax.swing.UIManager.put("OptionPane.warningDialog.titlePane.background", red);
        javax.swing.UIManager.put("OptionPane.errorDialog.titlePane.background", red);
        javax.swing.UIManager.put("OptionPane.informationDialog.titlePane.background", panel);
    }

    private static void cliDemo() throws Exception {IntentManager m=new IntentManager(100);m.submit("Reliable voice service, latency 50ms, bandwidth 40Mbps");m.submit("Video streaming, latency 100ms, bandwidth 35Mbps");m.submit("New voice service, latency 50ms, bandwidth 40Mbps");m.simulateMonitoring();System.out.println(m.trainKg2e());System.out.println(m.optimizeDemo());System.out.println(new ReportService().exportIntents(m.intents,Path.of("results","intents.csv")).toAbsolutePath());m.intents.forEach(i->System.out.println(i.id+" "+i.serviceType+" "+i.status+" "+i.explanation));}
}
