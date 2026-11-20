package com.intentmanagement.ui;

import com.intentmanagement.core.IntentManager;
import com.intentmanagement.dataset.UserIntentCsvProcessor;
import com.intentmanagement.simulation.Ns3Runner;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** User workflow: upload id,intent CSV -> parse/complete -> export processed CSV -> select an intent for ns-3. */
public final class IntentCsvPipelinePanel extends JPanel {
    // The CSV workflow page uses its own light, high-contrast theme; other dashboard pages are unchanged.
    private static final Color BG=new Color(245,248,252), PANEL=Color.WHITE, PANEL2=new Color(225,234,245), TEXT=new Color(25,38,58), MUTED=new Color(75,91,112), ACCENT=new Color(25,92,160), GREEN=new Color(28,112,91);
    private final IntentManager manager; private final java.util.function.Consumer<String> simulate;
    private Path selectedFile, processedFile;
    private java.util.List<UserIntentCsvProcessor.Row> processedRows = java.util.List.of();
    private final JLabel fileLabel=new JLabel("No input CSV selected");
    private final JLabel resultLabel=new JLabel("Required input columns: id,intent");
    private final DefaultTableModel model=new DefaultTableModel(new Object[]{"ID","Intent","Service","Latency ms","Bandwidth Mbps","Loss %","Priority","Status"},0){public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model); private final BoxPlot boxPlot=new BoxPlot();
    public IntentCsvPipelinePanel(IntentManager manager, java.util.function.Consumer<String> simulate){this.manager=manager;this.simulate=simulate;setBackground(BG);setLayout(new BorderLayout(10,10));setBorder(new EmptyBorder(14,16,14,16));setFont(new Font("Segoe UI",Font.PLAIN,15));build();}
    private void build(){
        JPanel top=new JPanel(new BorderLayout(8,8));top.setOpaque(false);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));buttons.setOpaque(false);
        buttons.add(button("1. Upload intent CSV",ACCENT,e->chooseCsv()));
        buttons.add(button("2. Process + export CSV",GREEN,e->processCsv()));
        buttons.add(button("3. Simulate selected intent",GREEN,e->simulateSelected()));
        buttons.add(button("4. Deploy validated intents in ns-3",ACCENT,e->simulateAll()));
        buttons.add(button("Open processed CSV",PANEL2,e->openProcessed()));
        top.add(buttons,BorderLayout.NORTH);
        JPanel labels=new JPanel(new GridLayout(2,1,2,2));labels.setOpaque(false);fileLabel.setForeground(TEXT);fileLabel.setFont(new Font("Segoe UI",Font.BOLD,14));resultLabel.setForeground(MUTED);resultLabel.setFont(new Font("Segoe UI",Font.PLAIN,13));labels.add(fileLabel);labels.add(resultLabel);top.add(labels,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        table.setRowHeight(32);table.setFont(new Font("Segoe UI",Font.PLAIN,14));table.setBackground(PANEL);table.setForeground(TEXT);table.setGridColor(new Color(205,216,230));table.setSelectionForeground(Color.WHITE);table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,14));table.getTableHeader().setBackground(PANEL2);table.getTableHeader().setForeground(TEXT);table.setSelectionBackground(new Color(25,92,160));
        JPanel center=new JPanel(new BorderLayout(8,8));center.setOpaque(false);JPanel tablePanel=section("PROCESSED INTENTS");tablePanel.setLayout(new BorderLayout());tablePanel.add(new JScrollPane(table),BorderLayout.CENTER);center.add(tablePanel,BorderLayout.CENTER);
        JPanel plotPanel=section("KPI DISTRIBUTION — BOX PLOTS");plotPanel.setLayout(new BorderLayout());plotPanel.add(boxPlot,BorderLayout.CENTER);plotPanel.setPreferredSize(new Dimension(100,250));center.add(plotPanel,BorderLayout.SOUTH);add(center,BorderLayout.CENTER);
    }
    private void chooseCsv(){JFileChooser fc=new JFileChooser();fc.setDialogTitle("Choose user intent CSV (id,intent)");if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){selectedFile=fc.getSelectedFile().toPath();fileLabel.setText("Input: "+selectedFile.toAbsolutePath());resultLabel.setText("Ready to process. CSV must contain id and intent columns.");}}
    private void processCsv(){if(selectedFile==null){chooseCsv();if(selectedFile==null)return;}try{Path out=Path.of("results","processed_intents.csv");var result=new UserIntentCsvProcessor().process(selectedFile,out,manager);processedFile=result.output();processedRows=result.rows();model.setRowCount(0);java.util.List<Double> lat=new ArrayList<>(),bw=new ArrayList<>(),loss=new ArrayList<>();for(var r:result.rows()){model.addRow(new Object[]{r.id(),r.rawIntent(),r.service(),num(r.latencyMs()),num(r.bandwidthMbps()),num(r.packetLossPct()),r.priority(),r.status()});if(Double.isFinite(r.latencyMs()))lat.add(r.latencyMs());if(Double.isFinite(r.bandwidthMbps()))bw.add(r.bandwidthMbps());if(Double.isFinite(r.packetLossPct()))loss.add(r.packetLossPct());}boxPlot.setData(lat,bw,loss);resultLabel.setText("Processed "+result.rows().size()+" rows → "+processedFile+" | ambiguous rows marked NEEDS_REVIEW");JOptionPane.showMessageDialog(this,"CSV processing completed.\nRows: "+result.rows().size()+"\nProcessed output: "+processedFile,"Intent CSV pipeline",JOptionPane.INFORMATION_MESSAGE);}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"CSV processing failed",JOptionPane.ERROR_MESSAGE);}}
    private void simulateAll(){
        if(processedRows.isEmpty()){
            JOptionPane.showMessageDialog(this,"Upload and process the CSV first.","No processed intents",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String root=System.getenv("NS3_ROOT");
        if(root==null||root.isBlank()){
            if(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) root="/home/Amali/ns-3.49";
            else { JOptionPane.showMessageDialog(this,"Set NS3_ROOT to your ns-3 directory (for example /home/Amali/ns-3.49).","ns-3 not configured",JOptionPane.WARNING_MESSAGE); return; }
        }
        final String ns3Root=root;
        Path batchDir=Path.of("results","ns3-batch");
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),"Batch ns-3 simulation",Dialog.ModalityType.MODELESS);
        JPanel content=new JPanel(new BorderLayout(10,10)); content.setBorder(new EmptyBorder(16,18,16,18)); content.setBackground(BG);
        JLabel progressText=new JLabel("Validating deployment candidates from "+processedRows.size()+" intents…"); progressText.setForeground(TEXT); progressText.setFont(new Font("Segoe UI",Font.BOLD,15));
        JProgressBar bar=new JProgressBar(0,100); bar.setStringPainted(true); bar.setFont(new Font("Segoe UI",Font.PLAIN,13));
        JButton cancel=new JButton("Cancel batch"); content.add(progressText,BorderLayout.NORTH); content.add(bar,BorderLayout.CENTER); content.add(cancel,BorderLayout.SOUTH); dialog.setContentPane(content); dialog.pack(); dialog.setLocationRelativeTo(this);
        SwingWorker<Path,String> worker=new SwingWorker<>(){
            protected Path doInBackground() throws Exception {
                Files.createDirectories(batchDir);
                Path aggregate=Path.of("results","ns3-batch-kpis.csv");
                int success=0,failed=0,skipped=0;
                try(BufferedWriter writer=Files.newBufferedWriter(aggregate,java.nio.charset.StandardCharsets.UTF_8)){
                    writer.write("intent_id,intent_text,processing_status,simulation_status,flow_id,intent_service,intent_priority,target_latency_ms,target_packet_loss_pct,source,destination,tx_packets,rx_packets,lost_packets,mean_delay_ms,throughput_mbps,packet_loss_pct");
                    writer.newLine();
                    for(int i=0;i<processedRows.size();i++){
                        if(isCancelled()) break;
                        UserIntentCsvProcessor.Row r=processedRows.get(i);
                        String safeId=r.id().replaceAll("[^A-Za-z0-9_-]","_");
                        // The paper lifecycle verifies feasibility before deployment: do not deploy rejected or ambiguous intents.
                        if (!"ACTIVE".equalsIgnoreCase(r.status())) {
                            String simStatus = "REJECTED".equalsIgnoreCase(r.status()) ? "NOT_DEPLOYED_REJECTED" : "NOT_DEPLOYED_REVIEW";
                            writer.write(csv(r.id())+","+csv(r.rawIntent())+","+csv(r.status())+","+simStatus+",,,,,,,,,,,,,");
                            writer.newLine(); skipped++;
                            setProgress((int)Math.round((i+1)*100.0/processedRows.size()));
                            publish((i+1)+"|Lifecycle validation: "+(i+1)+" / "+processedRows.size()+" | simulated "+success+" | failed "+failed+" | not deployed "+skipped);
                            continue;
                        }
                        String service=(r.service()==null||r.service().isBlank()||r.service().equalsIgnoreCase("Unknown"))?"General":r.service();
                        double bw=Double.isFinite(r.bandwidthMbps())&&r.bandwidthMbps()>0?Math.min(100.0,r.bandwidthMbps()):1.0;
                        double latency=Double.isFinite(r.latencyMs())&&r.latencyMs()>0?r.latencyMs():150.0;
                        double loss=Double.isFinite(r.packetLossPct())&&r.packetLossPct()>=0?Math.min(100.0,r.packetLossPct()):1.0;
                        int priority=r.priority()>=1&&r.priority()<=5?r.priority():1;
                        String rate=String.format(Locale.ROOT,"%.3fMbps",Math.max(0.05,bw));
                        String voice="256kbps",video="4Mbps";
                        if(service.toLowerCase(Locale.ROOT).contains("video")||service.toLowerCase(Locale.ROOT).contains("embb")) video=rate; else voice=rate;
                        try {
                            Ns3Runner.RunResult run=new Ns3Runner().run(Path.of(ns3Root),4,3,voice,video,service,latency,loss,priority);
                            if(run.exitCode()==0 && Files.isRegularFile(run.csv())){
                                Path saved=batchDir.resolve(String.format(Locale.ROOT,"%04d_%s.csv",i+1,safeId));
                                Files.copy(run.csv(),saved,StandardCopyOption.REPLACE_EXISTING);
                                Files.copy(run.log(),batchDir.resolve(String.format(Locale.ROOT,"%04d_%s.log",i+1,safeId)),StandardCopyOption.REPLACE_EXISTING);
                                List<String> lines=Files.readAllLines(saved,java.nio.charset.StandardCharsets.UTF_8);
                                for(int j=1;j<lines.size();j++){writer.write(csv(r.id())+","+csv(r.rawIntent())+","+csv(r.status())+",SUCCESS,"+lines.get(j));writer.newLine();}
                                success++;
                            } else {
                                writer.write(csv(r.id())+","+csv(r.rawIntent())+","+csv(r.status())+",FAILED,,,,,,,,,,,,");writer.newLine(); failed++;
                            }
                        } catch(Exception ex){
                            writer.write(csv(r.id())+","+csv(r.rawIntent())+","+csv(r.status())+",FAILED,,,,,,,,,,,,");writer.newLine();failed++;
                        }
                        setProgress((int)Math.round((i+1)*100.0/processedRows.size())); publish((i+1)+"|Completed "+(i+1)+" / "+processedRows.size()+" | simulated "+success+" | failed "+failed+" | not deployed "+skipped);
                    }
                }
                return aggregate.toAbsolutePath();
            }
            protected void process(java.util.List<String> messages){if(!messages.isEmpty()){String last=messages.get(messages.size()-1);int split=last.indexOf('|');if(split>0){bar.setValue(Integer.parseInt(last.substring(0,split)));progressText.setText(last.substring(split+1));}}}
            protected void done(){dialog.dispose();try{Path report=get();loadKpiComparison(report);JOptionPane.showMessageDialog(IntentCsvPipelinePanel.this,"Batch run finished. Only ACTIVE/validated intents were deployed.\nConsolidated lifecycle/KPI report: "+report+"\nPer-intent CSV/log files: "+batchDir.toAbsolutePath()+"\nBox plots now compare target and measured KPIs where simulation results are available.","Batch simulation",JOptionPane.INFORMATION_MESSAGE);}catch(Exception ex){JOptionPane.showMessageDialog(IntentCsvPipelinePanel.this,"Batch simulation did not complete: "+ex.getMessage(),"Batch ns-3 error",JOptionPane.ERROR_MESSAGE);}}
        };
        cancel.addActionListener(e->worker.cancel(true)); worker.execute(); dialog.setVisible(true);
    }
    private void loadKpiComparison(Path report){
        try{
            Map<String,UserIntentCsvProcessor.Row> byId=new HashMap<>(); for(var r:processedRows)byId.put(r.id(),r);
            java.util.List<Double> targetLat=new ArrayList<>(),actualLat=new ArrayList<>(),targetBw=new ArrayList<>(),actualBw=new ArrayList<>(),targetLoss=new ArrayList<>(),actualLoss=new ArrayList<>();
            java.util.List<String> lines=Files.readAllLines(report,java.nio.charset.StandardCharsets.UTF_8);
            for(int i=1;i<lines.size();i++){
                java.util.List<String> c=UserIntentCsvProcessor.parseCsvLine(lines.get(i));
                if(c.size()<17 || !"SUCCESS".equalsIgnoreCase(c.get(3))) continue;
                UserIntentCsvProcessor.Row r=byId.get(c.get(0)); if(r==null)continue;
                addFinite(targetLat,r.latencyMs()); addFinite(actualLat,Double.parseDouble(c.get(14)));
                addFinite(targetBw,r.bandwidthMbps()); addFinite(actualBw,Double.parseDouble(c.get(15)));
                addFinite(targetLoss,r.packetLossPct()); addFinite(actualLoss,Double.parseDouble(c.get(16)));
            }
            boxPlot.setComparison(targetLat,actualLat,targetBw,actualBw,targetLoss,actualLoss);
        }catch(Exception ex){resultLabel.setText("Batch KPI report created; comparison plot could not be refreshed: "+ex.getMessage());}
    }
    private static void addFinite(java.util.List<Double> values,double value){if(Double.isFinite(value))values.add(value);}
    private static String csv(String s){return "\""+(s==null?"":s.replace("\"","\"\"").replace("\r"," ").replace("\n"," "))+"\"";}
    private void simulateSelected(){int view=table.getSelectedRow();if(view<0){JOptionPane.showMessageDialog(this,"Select a processed intent row first.","Select intent",JOptionPane.INFORMATION_MESSAGE);return;}int row=table.convertRowIndexToModel(view);String intent=String.valueOf(model.getValueAt(row,1));String status=String.valueOf(model.getValueAt(row,7));if(status.equals("REJECTED")||status.equals("NEEDS_REVIEW")){int yes=JOptionPane.showConfirmDialog(this,"This row is "+status+". Review it before simulating? Continue anyway?","Intent review",JOptionPane.YES_NO_OPTION);if(yes!=JOptionPane.YES_OPTION)return;}simulate.accept(intent);}
    private void openProcessed(){if(processedFile==null||!Files.isRegularFile(processedFile)){JOptionPane.showMessageDialog(this,"Process an input CSV first. Output is written to results/processed_intents.csv.","No output yet",JOptionPane.INFORMATION_MESSAGE);return;}try{Desktop.getDesktop().open(processedFile.toFile());}catch(Exception ex){JOptionPane.showMessageDialog(this,"Processed CSV: "+processedFile.toAbsolutePath()+"\n"+ex.getMessage());}}
    private static String num(double d){return Double.isFinite(d)?String.format(Locale.ROOT,"%.3f",d):"—";}
    private JButton button(String t,Color c,java.awt.event.ActionListener l){JButton b=new JButton(t);b.setUI(new javax.swing.plaf.basic.BasicButtonUI());b.setFont(new Font("Segoe UI",Font.BOLD,13));b.setBackground(c);double lum=0.299*c.getRed()+0.587*c.getGreen()+0.114*c.getBlue();b.setForeground(lum>125?new Color(18,30,45):Color.WHITE);b.setOpaque(true);b.setContentAreaFilled(true);b.setEnabled(true);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(lum>125?c.darker():c.brighter()),new EmptyBorder(8,10,8,10)));b.addActionListener(l);return b;}
    private JPanel section(String title){JPanel p=new JPanel();p.setBackground(PANEL);p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(190,205,223)),title,0,0,new Font("Segoe UI",Font.BOLD,14),ACCENT));return p;}
    private static final class BoxPlot extends JPanel {
        private java.util.List<Double>[] series=new java.util.List[]{new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>()};
        private final String[] names={"Latency (ms)","Bandwidth / throughput (Mbps)","Packet loss (%)"};
        BoxPlot(){setBackground(PANEL);setPreferredSize(new Dimension(900,250));}
        void setData(java.util.List<Double> latency,java.util.List<Double> bandwidth,java.util.List<Double> loss){
            series=new java.util.List[]{new ArrayList<>(latency),new ArrayList<>(),new ArrayList<>(bandwidth),new ArrayList<>(),new ArrayList<>(loss),new ArrayList<>()};repaint();
        }
        void setComparison(java.util.List<Double> targetLatency,java.util.List<Double> actualLatency,java.util.List<Double> targetBandwidth,java.util.List<Double> actualThroughput,java.util.List<Double> targetLoss,java.util.List<Double> actualLoss){
            series=new java.util.List[]{new ArrayList<>(targetLatency),new ArrayList<>(actualLatency),new ArrayList<>(targetBandwidth),new ArrayList<>(actualThroughput),new ArrayList<>(targetLoss),new ArrayList<>(actualLoss)};repaint();
        }
        protected void paintComponent(Graphics g){
            super.paintComponent(g);Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),top=45,bottom=h-45;d.setFont(new Font("Segoe UI",Font.PLAIN,13));
            d.setColor(new Color(25,92,160));d.fillRect(12,12,12,12);d.setColor(TEXT);d.drawString("Target / requested",29,23);
            d.setColor(new Color(28,112,91));d.fillRect(165,12,12,12);d.setColor(TEXT);d.drawString("Measured by ns-3 (after batch run)",182,23);
            for(int group=0;group<3;group++){
                int center=(group+1)*w/4;d.setColor(MUTED);d.drawString(names[group],center-76,39);
                java.util.List<Double> target=series[group*2].stream().filter(Double::isFinite).sorted().toList();
                java.util.List<Double> actual=series[group*2+1].stream().filter(Double::isFinite).sorted().toList();
                java.util.List<Double> all=new ArrayList<>(target);all.addAll(actual);all.sort(Double::compareTo);
                if(all.isEmpty()){d.drawString("No numeric data",center-42,(top+bottom)/2);continue;}
                double min=all.get(0),max=all.get(all.size()-1),range=Math.max(1e-9,max-min);
                java.util.function.ToIntFunction<Double> y=v->bottom-8-(int)((v-min)/range*(bottom-top-16));
                d.setColor(new Color(220,228,238));d.drawLine(center-66,bottom,center+66,bottom);
                drawBox(d,target,center-22,y,new Color(25,92,160),MUTED,bottom);
                if(!actual.isEmpty())drawBox(d,actual,center+22,y,new Color(28,112,91),MUTED,bottom);
                d.setColor(MUTED);d.drawString("n="+target.size()+" / "+actual.size(),center-35,h-12);
            }
            d.dispose();
        }
        private static void drawBox(Graphics2D d,java.util.List<Double> values,int x,java.util.function.ToIntFunction<Double> y,Color fill,Color line,int bottom){
            if(values.isEmpty())return;double min=values.get(0),max=values.get(values.size()-1),q1=q(values,.25),med=q(values,.5),q3=q(values,.75);int yMin=y.applyAsInt(min),yQ1=y.applyAsInt(q1),yMed=y.applyAsInt(med),yQ3=y.applyAsInt(q3),yMax=y.applyAsInt(max);
            d.setColor(line);d.drawLine(x,yMin,x,yQ1);d.drawLine(x,yQ3,x,yMax);d.drawLine(x-8,yMin,x+8,yMin);d.drawLine(x-8,yMax,x+8,yMax);
            d.setColor(fill);d.fillRect(x-12,Math.min(yQ3,yQ1),24,Math.max(3,Math.abs(yQ1-yQ3)));d.setColor(fill.darker());d.drawRect(x-12,Math.min(yQ3,yQ1),24,Math.max(3,Math.abs(yQ1-yQ3)));d.drawLine(x-12,yMed,x+12,yMed);
            double iqr=q3-q1;for(double v:values)if(v<q1-1.5*iqr||v>q3+1.5*iqr){d.setColor(fill);d.fillOval(x-3,y.applyAsInt(v)-3,6,6);}
        }
        private static double q(java.util.List<Double> a,double p){if(a.size()==1)return a.get(0);double pos=p*(a.size()-1);int lo=(int)Math.floor(pos),hi=(int)Math.ceil(pos);return a.get(lo)+(a.get(hi)-a.get(lo))*(pos-lo);}
    }
}
