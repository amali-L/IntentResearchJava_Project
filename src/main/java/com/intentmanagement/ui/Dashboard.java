package com.intentmanagement.ui;

import com.intentmanagement.core.*;
import com.intentmanagement.knowledge.Triple;
import com.intentmanagement.reporting.ReportService;
import com.intentmanagement.dataset.DatasetPatternProcessor;
import com.intentmanagement.simulation.Ns3Runner;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.*;
import java.util.List;

/** Polished, responsive-enough Swing desktop dashboard; all controls are local and functional. */
public class Dashboard extends JFrame {
    private static final Color BG=new Color(10,10,13), PANEL=new Color(24,24,31), PANEL2=new Color(33,35,44), TEXT=new Color(245,247,252), MUTED=new Color(180,190,205), ACCENT=new Color(255,70,85), GREEN=new Color(220,38,55), RED=new Color(255,70,85);
    private final IntentManager manager;
    private final JTextArea input=new JTextArea("Mission-critical push-to-talk, latency 75ms, bandwidth 25Mbps",3,40);
    private final JTextArea explanation=area(); private final JTextField kgQuery=new JTextField("Emergency",24); private final JTextArea kgOutput=area();
    private final JLabel activeMetric=metric("0","ACTIVE INTENTS"), bandwidthMetric=metric("0 / 100 Mbps","ALLOCATED CAPACITY"), graphMetric=metric("0","KG TRIPLES"), complianceMetric=metric("—","LAST CYCLE");
    private final DefaultTableModel model=new DefaultTableModel(new Object[]{"ID","Service","Lifecycle","Target ms","Req. Mbps","Priority","Observed ms","Throughput Mbps","Loss %"},0){public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model); private final JLabel status=new JLabel("● SYSTEM READY  |  local synthetic simulation");
    public Dashboard(IntentManager manager){super("IntentOps | Knowledge-Driven Network Orchestration");this.manager=manager;style();setDefaultCloseOperation(EXIT_ON_CLOSE);setMinimumSize(new Dimension(1120,740));setSize(1480,920);setLocationRelativeTo(null);setLayout(new BorderLayout());add(header(),BorderLayout.NORTH);add(buildTabs(),BorderLayout.CENTER);add(footer(),BorderLayout.SOUTH);refresh();}
    private void style(){
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}
        Font base=new Font("Segoe UI",Font.PLAIN,15);
        for(String key:List.of("Label.font","Button.font","TextField.font","TextArea.font","ComboBox.font","CheckBox.font","RadioButton.font","TabbedPane.font","Table.font","TableHeader.font","OptionPane.messageFont","OptionPane.buttonFont"))UIManager.put(key,base);
        UIManager.put("TabbedPane.background",BG);UIManager.put("TabbedPane.foreground",TEXT);UIManager.put("TabbedPane.selected",PANEL2);
        UIManager.put("TabbedPane.contentAreaColor",BG);UIManager.put("TabbedPane.focus",ACCENT);
        UIManager.put("Button.foreground",TEXT);UIManager.put("Button.background",PANEL2);
        UIManager.put("TabbedPane.contentAreaColor",BG);UIManager.put("TabbedPane.focus",ACCENT);UIManager.put("Table.selectionBackground",new Color(14,116,144));UIManager.put("Table.selectionForeground",Color.WHITE);
        UIManager.put("OptionPane.background",PANEL);UIManager.put("Panel.background",PANEL);UIManager.put("Button.font",new Font("Segoe UI",Font.BOLD,15));
        getContentPane().setBackground(BG);
    }
    private JPanel header(){JPanel p=new JPanel(new BorderLayout());p.setBackground(BG);p.setBorder(new EmptyBorder(22,26,16,26));JPanel left=new JPanel();left.setOpaque(false);left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));JLabel brand=new JLabel("INTENTOPS  /  RESEARCH CONSOLE");brand.setForeground(ACCENT);brand.setFont(new Font("Segoe UI",Font.BOLD,14));JLabel title=new JLabel("Knowledge-driven network orchestration");title.setForeground(TEXT);title.setFont(new Font("Segoe UI",Font.BOLD,29));JLabel sub=new JLabel("Intent → KG completion → validation → deployment → assurance → re-optimization");sub.setForeground(MUTED);sub.setFont(new Font("Segoe UI",Font.PLAIN,16));left.add(brand);left.add(Box.createVerticalStrut(7));left.add(title);left.add(Box.createVerticalStrut(5));left.add(sub);p.add(left,BorderLayout.WEST);JLabel tag=new JLabel("JAVA DESKTOP  •  ns-3 ADAPTER INCLUDED");tag.setForeground(GREEN);tag.setFont(new Font("Segoe UI",Font.BOLD,13));p.add(tag,BorderLayout.EAST);return p;}
    private JTabbedPane buildTabs(){
        JTabbedPane tabs=new JTabbedPane();
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI(){
            protected void installDefaults(){super.installDefaults();tabAreaInsets=new Insets(7,8,2,8);tabInsets=new Insets(10,14,10,14);contentBorderInsets=new Insets(2,2,2,2);}
            protected void paintTabBackground(Graphics g,int placement,int index,int x,int y,int w,int h,boolean selected){g.setColor(selected?new Color(220,38,55):new Color(24,24,31));g.fillRect(x,y,w,h);}
            protected void paintContentBorder(Graphics g,int placement,int selectedIndex){g.setColor(new Color(65,72,88));int x=0,y=calculateTabAreaHeight(placement,runCount, maxTabHeight);g.drawRect(x,y,tabPane.getWidth()-1,tabPane.getHeight()-y-1);}
        });
        tabs.setFont(new Font("Segoe UI",Font.BOLD,15));tabs.setForeground(TEXT);tabs.setBackground(BG);tabs.setOpaque(true);
        tabs.addTab("  Overview  ",overview());tabs.addTab("  Knowledge graph  ",kgTab());tabs.addTab("  Equations & experiments  ",equationsTab());tabs.addTab("  Dataset & ns-3  ",datasetTab());tabs.addTab("  User Intent CSV  ",new IntentCsvPipelinePanel(manager, text -> { input.setText(text); submitAndRunNs3(); }));tabs.addTab("  KPI comparison  ",new KpiComparisonPanel(manager.intents));
        for(int i=0;i<tabs.getTabCount();i++)tabs.setForegroundAt(i,Color.WHITE);
        // CSV-first workflow: hide the single-intent lifecycle page and open the uploaded-CSV pipeline by default.
        tabs.setSelectedIndex(4);
        return tabs;
    }
    private JPanel overview(){JPanel p=panel();p.setLayout(new BorderLayout(14,14));JPanel cards=new JPanel(new GridLayout(1,4,12,12));cards.setOpaque(false);cards.add(metricCard(activeMetric));cards.add(metricCard(bandwidthMetric));cards.add(metricCard(graphMetric));cards.add(metricCard(complianceMetric));p.add(cards,BorderLayout.NORTH);JPanel body=new JPanel(new BorderLayout(12,12));body.setOpaque(false);JPanel quick=section("QUICK ACTIONS");quick.setLayout(new GridLayout(0,1,8,8));quick.add(action("Submit current intent + run ns-3",GREEN,e->submitAndRunNs3()));quick.add(action("Run monitoring cycle",GREEN,e->{manager.simulateMonitoring();refresh();complianceMetric.setText("updated");explanation.setText("Monitoring pass complete. KPIs are synthetic Java simulator values unless ns-3 is run separately.\n\n"+manager.intents.stream().map(i->i.id+" | "+i.serviceType+" | "+i.status+" | latency="+String.format("%.2f",i.measuredLatencyMs)+" ms").reduce("",(a,b)->a+b+"\n"));}));quick.add(action("Run SPSA + MGDA demonstration",ACCENT,e->explanation.setText(manager.optimizeDemo())));quick.add(action("Inspect knowledge graph",ACCENT,e->explanation.setText(manager.graph.describe())));quick.add(action("Export intent report",ACCENT,e->export()));quick.add(action("Analyze existing dataset → KG",ACCENT,e->analyzeDatasetPatterns()));body.add(quick,BorderLayout.WEST);JPanel report=section("SYSTEM / RESEARCH NOTES");report.setLayout(new BorderLayout());JTextArea note=area();note.setText("PAPER-ALIGNED PIPELINE\n\n01  Intent expression + keyword/parameter extraction\n02  Service-template completion from KG triples\n03  KG embedding / triple ranking (prototype approximation)\n04  Candidate validation and conflict checks\n05  Service mapping and resource feasibility\n06  Deployment into a network model\n07  KPI monitoring against SLA targets\n08  SPSA gradient estimation + MGDA multi-objective direction\n09  Re-optimization after arrivals or KPI violations\n\nResearch integrity: Java synthetic measurements are not ns-3 output. Use the Dataset & ns-3 tab to generate data and run the included ns-3 scenario after installing ns-3.");report.add(new JScrollPane(note),BorderLayout.CENTER);body.add(report,BorderLayout.CENTER);p.add(body,BorderLayout.CENTER);return p;}
    private JPanel intentTab(){JPanel p=panel();p.setLayout(new BorderLayout(12,12));JPanel compose=section("NEW USER-PROVIDED INTENT");compose.setLayout(new BorderLayout(8,8));input.setLineWrap(true);input.setWrapStyleWord(true);input.setBackground(PANEL2);input.setForeground(TEXT);input.setCaretColor(ACCENT);input.setFont(new Font("Segoe UI",Font.PLAIN,17));input.setBorder(new EmptyBorder(12,12,12,12));compose.add(new JScrollPane(input),BorderLayout.CENTER);JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));controls.setOpaque(false);controls.add(action("Validate + complete from KG",ACCENT,e->submit()));controls.add(action("Submit intent + run ns-3",GREEN,e->submitAndRunNs3()));controls.add(action("Clear",MUTED,e->input.setText("")));controls.add(action("Run monitoring",GREEN,e->{manager.simulateMonitoring();refresh();}));compose.add(controls,BorderLayout.SOUTH);p.add(compose,BorderLayout.NORTH);table.setRowHeight(34);table.setFont(new Font("Segoe UI",Font.PLAIN,14));table.setForeground(TEXT);table.setBackground(PANEL);table.getTableHeader().setBackground(PANEL2);table.getTableHeader().setForeground(TEXT);table.setGridColor(new Color(51,65,85));JPanel grid=section("DEPLOYED INTENTS / LIVE KPI VIEW");grid.setLayout(new BorderLayout());grid.add(new JScrollPane(table),BorderLayout.CENTER);p.add(grid,BorderLayout.CENTER);JPanel explain=section("LIFECYCLE TRACE / EXPLANATION");explanation.setText("Submit an intent to view parsed entities, KG-filled parameters, feasibility checks, and current status.");explain.setLayout(new BorderLayout());explain.add(new JScrollPane(explanation),BorderLayout.CENTER);explain.setPreferredSize(new Dimension(100,140));p.add(explain,BorderLayout.SOUTH);return p;}
    private JPanel kgTab(){JPanel p=panel();p.setLayout(new BorderLayout(12,12));JPanel query=section("QUERY THE KNOWLEDGE GRAPH");query.setLayout(new BorderLayout(8,8));JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT));bar.setOpaque(false);kgQuery.setColumns(34);kgQuery.setBackground(PANEL2);kgQuery.setForeground(TEXT);bar.add(kgQuery);bar.add(action("Search triples",ACCENT,e->queryKg()));bar.add(action("Search RDF / SPARQL",ACCENT,e->kgOutput.setText(manager.queryRdf(kgQuery.getText()))));bar.add(action("Validate ontology (SHACL)",GREEN,e->kgOutput.setText(manager.validateOntology())));bar.add(action("Evaluate labelled intents",GREEN,e->{try{var kb=new com.intentmanagement.knowledge.JenaKnowledgeBase(Path.of("data","ontology","intent-schema.ttl"));var evaluator=new com.intentmanagement.dataset.ServiceMatchingEvaluator();var metrics=evaluator.evaluate(Path.of("data","datasets","labelled_intent_matching.csv"),kb);String report=evaluator.format(metrics);Files.createDirectories(Path.of("results"));Files.writeString(Path.of("results","service-matching-evaluation.txt"),report);kgOutput.setText(report);kb.close();}catch(Exception ex){kgOutput.setText("Evaluation failed: "+ex.getMessage());}}));bar.add(action("Show all triples",MUTED,e->kgOutput.setText(manager.graph.describe())));query.add(bar,BorderLayout.NORTH);kgOutput.setText("Apache Jena status: "+manager.rdfGraphStatus()+"\nTry RDF/SPARQL search for Emergency, Voice, latency, or bandwidth.\nOntology SHACL validation and labelled intent evaluation are available above.");query.add(new JScrollPane(kgOutput),BorderLayout.CENTER);p.add(query,BorderLayout.CENTER);JPanel help=section("HOW DYNAMIC INTENT COMPLETION USES THE KG");JTextArea t=area();t.setText("Apache Jena loads data/ontology/intent-schema.ttl as an RDF model and uses SPARQL to retrieve service aliases and QoS profiles. SHACL shapes in data/ontology/shapes/service-shapes.ttl validate latency, bandwidth, loss, priority and alias constraints. The indexed TSV triple store remains available for KG2E demonstrations and compatibility. Explicit values in the user intent override ontology defaults. The RDF ontology includes service classes, aliases, QoS constraints, resource relationships, compatibility and conflict links.");help.setLayout(new BorderLayout());help.add(new JScrollPane(t),BorderLayout.CENTER);help.setPreferredSize(new Dimension(100,190));p.add(help,BorderLayout.SOUTH);return p;}
    private JPanel equationsTab(){JPanel p=panel();p.setLayout(new BorderLayout(12,12));JTextArea t=area();JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.setOpaque(false);actions.add(action("Show last intent equation trace",ACCENT,e->t.setText(manager.runEquationPipeline())));actions.add(action("Run KG2E ranking training",GREEN,e->t.setText(manager.trainKg2e())));p.add(actions,BorderLayout.NORTH);t.setText("EQUATION → IMPLEMENTATION TRACEABILITY\n\n1. KG triple representation: G=(E,R,F), (h,r,t) ∈ F\n   Triple.java; KnowledgeGraph.java\n\n2. Gaussian embedding score (prototype): score(h,r,t) ≈ −Σd[(μh−μr−μt)²/Cd + ln(Cd)]\n   KG2EModel.java. The paper uses Gaussian entity/relation distributions and its specified scoring/training procedure; this compact diagonal implementation is not an exact reproduction.\n\n3. KPI loss / MSE: MSE = (1/m) Σi(yi−ŷi)²\n   QoSLossCalculator.java\n\n4. SPSA estimate (paper Eq. 9): ĝi = [f(θ+cΔ)−f(θ−cΔ)]/(2cΔi), Δi∈{−1,+1}\n   SPSAOptimizer.java\n\n5. MGDA weights (paper Eq. 11): minw ||Σi wi gi||², subject to wi≥0 and Σi wi=1\n   MGDAOptimizer.java\n\n6. Proposed novelty (not a paper contribution): cross-intent conflict-aware admission + priority-aware re-optimization + human-readable decision trace. It must be evaluated against baselines/ablations before claiming improvement.\n\nThe exact paper equations and pseudocode are documented in docs/MATHEMATICS.md with equation numbers and implementation gaps.");p.add(new JScrollPane(t),BorderLayout.CENTER);return p;}
    private JPanel datasetTab(){JPanel p=panel();p.setLayout(new BorderLayout(12,12));JPanel actions=section("EXISTING DATASET / SIMULATION");actions.setLayout(new GridLayout(0,1,8,8));actions.add(action("Analyze existing dataset → KG patterns",ACCENT,e->analyzeDatasetPatterns()));actions.add(action("Run ns-3 packet simulation",GREEN,e->runNs3()));actions.add(action("Open dataset folder",MUTED,e->{try{Desktop.getDesktop().open(Path.of("data","datasets").toAbsolutePath().toFile());}catch(Exception ex){kgOutput.setText("Dataset folder: "+Path.of("data","datasets").toAbsolutePath()+"\n"+ex.getMessage());}}));p.add(actions,BorderLayout.WEST);JTextArea info=area();info.setText("EXISTING DATASET → KNOWLEDGE GRAPH\n\nThe project uses data/datasets/labelled_intent_matching.csv as its existing intent corpus. Analyze patterns reads these records, summarizes service-specific tokens, and adds datasetObservedIntentCount / observedIntentPattern facts to the runtime knowledge graph. It does not generate or append 1,000-row datasets. Derived facts and a human-readable summary are written under results/.\n\nns-3 PACKET-LEVEL SIMULATION\n\nThe scenario source is ns-3/scratch/intent-lifecycle.cc. On Windows with WSL2, NS3_ROOT must be a Linux path such as /home/Amali/ns-3.49. The adapter detects this path and invokes wsl.exe, copies the scenario into ns-3 scratch, and writes the KPI CSV into this project's results folder. This requires WSL, ns-3, and a working build environment. A successful Java GUI launch alone does not prove the simulation ran.");JPanel notes=section("SIMULATION & DATASET NOTES");notes.setLayout(new BorderLayout());notes.add(new JScrollPane(info),BorderLayout.CENTER);p.add(notes,BorderLayout.CENTER);return p;}
    private JPanel footer(){JPanel p=new JPanel(new BorderLayout());p.setBackground(new Color(8,15,30));p.setBorder(new EmptyBorder(8,18,8,18));status.setForeground(MUTED);status.setFont(new Font("Segoe UI",Font.PLAIN,13));p.add(status,BorderLayout.WEST);JLabel right=new JLabel("Prototype build • Java 21 • reproducible seed");right.setForeground(MUTED);right.setFont(new Font("Segoe UI",Font.PLAIN,13));p.add(right,BorderLayout.EAST);return p;}
    private JPanel panel(){JPanel p=new JPanel();p.setBackground(BG);p.setBorder(new EmptyBorder(16,22,18,22));return p;}
    private JPanel section(String title){JPanel p=new JPanel();p.setBackground(PANEL);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(51,65,85)),BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(8,8,8,8),title,0,0,new Font("Segoe UI",Font.BOLD,13),ACCENT)));return p;}
    private JPanel metricCard(JLabel value){JPanel p=new JPanel(new BorderLayout(4,8));p.setBackground(PANEL);p.setBorder(new EmptyBorder(15,16,15,16));value.setFont(new Font("Segoe UI",Font.BOLD,25));value.setForeground(TEXT);p.add(value,BorderLayout.CENTER);return p;}
    private JLabel metric(String value,String name){JLabel l=new JLabel("<html><div style='font-size:10px;color:#94a3b8'>"+name+"</div><div style='padding-top:7px'>"+value+"</div></html>");return l;}
    private JButton action(String text,Color color,java.awt.event.ActionListener listener){
        JButton b=new JButton(text);
        // Use BasicButtonUI so Windows' system Look & Feel cannot replace the
        // requested foreground/background and create white-on-white labels.
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setRolloverEnabled(true);
        b.setBackground(color);
        double luminance=(0.299*color.getRed()+0.587*color.getGreen()+0.114*color.getBlue());
        b.setForeground(luminance>155?new Color(18,18,18):Color.WHITE);
        b.setFont(new Font("Segoe UI",Font.BOLD,15));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(luminance>155?new Color(180,180,180):color.darker(),1),
            new EmptyBorder(10,14,10,14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(listener);
        return b;
    }
    private JTextArea area(){JTextArea a=new JTextArea();a.setEditable(false);a.setLineWrap(true);a.setWrapStyleWord(true);a.setBackground(new Color(17,27,46));a.setForeground(TEXT);a.setCaretColor(TEXT);a.setFont(new Font("Consolas",Font.PLAIN,14));a.setBorder(new EmptyBorder(12,12,12,12));return a;}
    private void submit(){try{
        Intent i=manager.submit(input.getText());
        String rdfEvidence=manager.matchIntentWithRdf(i.rawText).map(m->m.service()+" | score="+String.format(java.util.Locale.ROOT,"%.3f",m.score())+" | evidence="+m.evidence()).orElse("No RDF alias match; lexical fallback used.");
        explanation.setText("LIFECYCLE TRACE  |  "+i.id+"\nStatus: "+i.status+"\nService: "+i.serviceType+"\nTargets: latency="+i.targetLatencyMs+" ms, bandwidth="+i.requiredBandwidthMbps+" Mbps, loss ≤ "+i.maxPacketLossPercent+"%, priority="+i.priority+"\n\nApache Jena match:\n"+rdfEvidence+"\n\nTriple-store evidence:\n"+manager.graph.find(i.serviceType,null).stream().filter(t->List.of("requiresLatencyMs","requiresBandwidthMbps","maxPacketLossPercent","priority","hasKeyword").contains(t.relation())).map(Object::toString).reduce("",(a,b)->a+b+"\n")+"\nDecision: "+i.explanation+"\n\nMeasurements: Java synthetic simulator; use ns-3 for packet-level metrics.\n\n"+manager.runEquationPipeline());refresh();
    }catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Intent validation error",JOptionPane.ERROR_MESSAGE);}}
    private void queryKg(){kgOutput.setText(manager.graph.queryText(kgQuery.getText()));}
    private void refresh(){model.setRowCount(0);for(Intent i:manager.intents)model.addRow(new Object[]{i.id,i.serviceType,i.status,String.format("%.2f",i.targetLatencyMs),String.format("%.2f",i.requiredBandwidthMbps),i.priority,String.format("%.2f",i.measuredLatencyMs),String.format("%.2f",i.measuredThroughputMbps),String.format("%.2f",i.measuredPacketLossPercent)});long active=manager.intents.stream().filter(i->i.status.equals("ACTIVE")).count();activeMetric.setText("<html><div style='font-size:10px;color:#94a3b8'>ACTIVE INTENTS</div><div style='padding-top:7px'>"+active+"</div></html>");bandwidthMetric.setText("<html><div style='font-size:10px;color:#94a3b8'>ALLOCATED CAPACITY</div><div style='padding-top:7px'>"+String.format("%.1f / %.1f Mbps",manager.usedBandwidth(),manager.capacity())+"</div></html>");graphMetric.setText("<html><div style='font-size:10px;color:#94a3b8'>KG TRIPLES</div><div style='padding-top:7px'>"+manager.graph.all().size()+"</div></html>");status.setText("● READY  |  "+manager.intents.size()+" total intents  |  "+active+" active  |  local synthetic KPI mode");}
    private void export(){try{Path path=new ReportService().exportIntents(manager.intents,Path.of("results","intents.csv"));explanation.setText("Report exported: "+path.toAbsolutePath());}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Export failed",JOptionPane.ERROR_MESSAGE);}}
    private void submitAndRunNs3(){
        try {
            Intent submitted = manager.submit(input.getText());
            refresh();
            explanation.setText("Intent submitted for packet-level simulation\n"
                    + "ID: " + submitted.id + "\nService: " + submitted.serviceType
                    + "\nLatency target: " + submitted.targetLatencyMs + " ms"
                    + "\nBandwidth: " + submitted.requiredBandwidthMbps + " Mbps"
                    + "\nPacket-loss target: " + submitted.maxPacketLossPercent + "%"
                    + "\nPriority: " + submitted.priority + "\n\nLaunching ns-3…");
            runNs3();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Intent validation error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runNs3(){
        String root=System.getenv("NS3_ROOT");
        // Convenience default for the user's known Windows + WSL installation.
        if(root==null||root.isBlank()) {
            if(System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win")) {
                root = "/home/Amali/ns-3.49";
            } else {
                JOptionPane.showMessageDialog(this,"Set NS3_ROOT to the folder containing ./ns3, for example /home/Amali/ns-3.49.","ns-3 not configured",JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        final String ns3Root = root;
        JDialog wait=new JDialog(this,"Running ns-3",false);
        JLabel label=new JLabel("Preparing ns-3 LTE/EPC simulation and collecting packet-level KPIs…",SwingConstants.CENTER);label.setFont(new Font("Segoe UI",Font.BOLD,16));
        label.setBorder(new EmptyBorder(24,24,24,24));wait.add(label);wait.pack();wait.setLocationRelativeTo(this);wait.setVisible(true);
        new SwingWorker<Ns3Runner.RunResult,Void>(){
            protected Ns3Runner.RunResult doInBackground()throws Exception{
                Intent latest = manager.intents.isEmpty() ? null : manager.intents.get(manager.intents.size()-1);
                if (latest == null) throw new java.io.IOException("Submit an intent in the Intent lifecycle tab before running ns-3.");
                int users = Math.max(1, Math.min(20, (int)Math.ceil(latest.requiredBandwidthMbps / 10.0)));
                String voiceRate = "256kbps", videoRate = "4Mbps";
                if (latest != null) {
                    String service = latest.serviceType.toLowerCase(java.util.Locale.ROOT);
                    String requested = String.format(java.util.Locale.ROOT, "%.3fMbps", Math.max(0.05, Math.min(100.0, latest.requiredBandwidthMbps)));
                    if (service.contains("video") || service.contains("embb")) videoRate = requested;
                    else voiceRate = requested;
                }
                return new Ns3Runner().run(Path.of(ns3Root), users, 12, voiceRate, videoRate,
                        latest.serviceType, latest.targetLatencyMs, latest.maxPacketLossPercent, latest.priority);
            }
            protected void done(){
                wait.dispose();
                try{
                    Ns3Runner.RunResult r=get();
                    JOptionPane.showMessageDialog(Dashboard.this,r.message()+"\nLog: "+r.log().toAbsolutePath(),"ns-3 result",r.exitCode()==0?JOptionPane.INFORMATION_MESSAGE:JOptionPane.WARNING_MESSAGE);
                    if(r.exitCode()==0){status.setText("ns-3 simulation completed  |  "+r.csv().toAbsolutePath()); String preview=""; try { preview="\n\nKPI CSV PREVIEW\n"+Files.readString(r.csv()); } catch(Exception ignored) { preview="\n(CSV preview unavailable; open the CSV file directly.)"; } explanation.setText("REAL ns-3 PACKET-LEVEL RUN\n\nIntent: "+(manager.intents.isEmpty()?"none":manager.intents.get(manager.intents.size()-1).rawText)+"\n\n"+r.message()+"\n\nRun log: "+r.log().toAbsolutePath()+preview+"\n\nTarget latency/loss/priority are passed as scenario metadata; this scaffold does not enforce them in the LTE scheduler.");}
                }catch(Exception ex){JOptionPane.showMessageDialog(Dashboard.this,ex.getMessage(),"ns-3 failed",JOptionPane.ERROR_MESSAGE);}
            }
        }.execute();
    }
    private void analyzeDatasetPatterns(){try{var result=new DatasetPatternProcessor().process(Path.of("data","datasets","labelled_intent_matching.csv"),Path.of("results"),manager.graph);String report=Files.readString(result.report());kgOutput.setText(report);explanation.setText(report+"\n\nPattern triples: "+result.triples().toAbsolutePath());refresh();status.setText("● DATASET PATTERNS LOADED  |  "+result.rows()+" existing rows → "+result.patternFacts()+" KG facts");JOptionPane.showMessageDialog(this,"Existing dataset analyzed successfully.\nRows read: "+result.rows()+"\nNo dataset rows generated.\nDerived KG facts: "+result.patternFacts()+"\nReport: "+result.report().toAbsolutePath(),"Dataset → KG complete",JOptionPane.INFORMATION_MESSAGE);}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Dataset analysis failed",JOptionPane.ERROR_MESSAGE);}}
}
