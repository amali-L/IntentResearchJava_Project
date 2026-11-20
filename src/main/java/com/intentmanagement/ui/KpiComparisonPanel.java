package com.intentmanagement.ui;

import com.intentmanagement.core.Intent;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/** Displays observed-versus-target KPI comparisons from current intent records. */
final class KpiComparisonPanel extends JPanel {
    private final java.util.List<Intent> intents;
    private final Color bg=new Color(12,12,16), panel=new Color(27,29,38), red=new Color(239,55,70), cyan=new Color(235,235,242), white=new Color(238,242,250), muted=new Color(165,175,190);
    KpiComparisonPanel(java.util.List<Intent> intents){this.intents=intents;setBackground(bg);setLayout(new GridLayout(3,1,8,8));setBorder(BorderFactory.createEmptyBorder(12,12,12,12));add(new MetricChart("LATENCY COMPARISON (ms)","Target latency","Observed latency",0));add(new MetricChart("THROUGHPUT COMPARISON (Mbps)","Required throughput","Observed throughput",1));add(new MetricChart("PACKET LOSS COMPARISON (%)","Allowed packet loss","Observed packet loss",2));}
    private final class MetricChart extends JPanel {
        private final String title,left,right; private final int kind;
        MetricChart(String title,String left,String right,int kind){this.title=title;this.left=left;this.right=right;this.kind=kind;setBackground(panel);setBorder(BorderFactory.createLineBorder(new Color(70,75,90)));setPreferredSize(new Dimension(800,150));}
        protected void paintComponent(Graphics raw){super.paintComponent(raw);Graphics2D g=(Graphics2D)raw.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int w=getWidth(),h=getHeight();g.setColor(red);g.setFont(new Font("Segoe UI",Font.BOLD,14));g.drawString(title,18,23);g.setFont(new Font("Segoe UI",Font.PLAIN,11));g.setColor(muted);g.drawString("RED = target / allowed  •  CYAN = observed  •  values from current intent records",18,42);if(intents.isEmpty()){g.setColor(white);g.setFont(new Font("Segoe UI",Font.PLAIN,14));g.drawString("No intent measurements yet. Submit an intent and run a monitoring cycle to populate this comparison.",18,78);g.dispose();return;}int rows=Math.min(3,intents.size());int y0=62;for(int i=0;i<rows;i++){Intent in=intents.get(i);double target=kind==0?in.targetLatencyMs:kind==1?in.requiredBandwidthMbps:in.maxPacketLossPercent;double observed=kind==0?in.measuredLatencyMs:kind==1?in.measuredThroughputMbps:in.measuredPacketLossPercent;double max=Math.max(1,Math.max(target,observed));int barMax=Math.max(60,w-360);int yy=y0+i*26;g.setColor(white);String name=in.serviceType.length()>17?in.serviceType.substring(0,17):in.serviceType;g.drawString(name,18,yy+10);g.setColor(red);g.fillRoundRect(150,yy, (int)(barMax*target/max),7,5,5);g.setColor(cyan);g.fillRoundRect(150,yy+10,(int)(barMax*observed/max),7,5,5);g.setColor(white);g.drawString(String.format(Locale.ROOT,"%.2f / %.2f",target,observed),Math.min(w-125,165+barMax),yy+12); }g.dispose();}
    }
}
