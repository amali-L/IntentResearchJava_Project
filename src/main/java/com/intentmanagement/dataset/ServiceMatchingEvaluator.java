package com.intentmanagement.dataset;

import com.intentmanagement.knowledge.JenaKnowledgeBase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Evaluates ontology-driven service classification against a human-readable labelled CSV. */
public final class ServiceMatchingEvaluator {
    public record Metrics(int total, int correct, double accuracy, double macroPrecision, double macroRecall, double macroF1, Map<String,Map<String,Integer>> confusion) {}
    public Metrics evaluate(Path csv, JenaKnowledgeBase kb) throws IOException {
        Map<String,Map<String,Integer>> confusion=new TreeMap<>(); int total=0,correct=0;
        try(BufferedReader br=Files.newBufferedReader(csv, StandardCharsets.UTF_8)){
            String header=br.readLine();if(header==null)throw new IOException("Dataset is empty: "+csv);
            Map<String,Integer> ix=index(header); if(!ix.containsKey("intent")||!ix.containsKey("gold_service"))throw new IOException("Expected columns intent and gold_service in "+csv); boolean heldOut=ix.containsKey("split");
            String line;while((line=br.readLine())!=null){if(line.isBlank())continue;List<String> c=parseCsv(line);if(heldOut&&!get(c,ix.get("split")).equalsIgnoreCase("test"))continue;String intent=get(c,ix.get("intent")),gold=get(c,ix.get("gold_service"));if(intent.isBlank()||gold.isBlank())continue;
                String predicted=kb.match(intent).map(JenaKnowledgeBase.Match::service).orElse("UNKNOWN");confusion.computeIfAbsent(gold,k->new TreeMap<>()).merge(predicted,1,Integer::sum);total++;if(gold.equalsIgnoreCase(predicted))correct++;
            }
        }
        double p=0,r=0,f=0;int classes=confusion.size();for(String label:confusion.keySet()){int tp=confusion.get(label).getOrDefault(label,0);int fp=0,fn=confusion.get(label).values().stream().mapToInt(Integer::intValue).sum()-tp;for(String gold:confusion.keySet()){if(!gold.equals(label))fp+=confusion.get(gold).getOrDefault(label,0);}double precision=tp+fp==0?0:(double)tp/(tp+fp),recall=tp+fn==0?0:(double)tp/(tp+fn);p+=precision;r+=recall;f+=(precision+recall==0?0:2*precision*recall/(precision+recall));}
        return new Metrics(total,correct,total==0?0:(double)correct/total,classes==0?0:p/classes,classes==0?0:r/classes,classes==0?0:f/classes,confusion);
    }
    public String format(Metrics m){StringBuilder b=new StringBuilder("LABELLED SERVICE-MATCHING EVALUATION\n");b.append("Records: ").append(m.total()).append("\nCorrect: ").append(m.correct()).append("\nAccuracy: ").append(fmt(m.accuracy())).append("\nMacro precision: ").append(fmt(m.macroPrecision())).append("\nMacro recall: ").append(fmt(m.macroRecall())).append("\nMacro F1: ").append(fmt(m.macroF1())).append("\n\nConfusion matrix (gold -> predicted counts)\n");m.confusion().forEach((g,row)->b.append(g).append(" -> ").append(row).append('\n'));b.append("\nThis report uses only rows marked split=test when that column exists. The labelled data is synthetic and template-generated; results validate code behavior, not real-world/generalization performance.");return b.toString();}
    private static String fmt(double d){return String.format(Locale.ROOT,"%.4f (%.2f%%)",d,d*100);}
    private static Map<String,Integer> index(String line){List<String> h=parseCsv(line);Map<String,Integer> m=new HashMap<>();for(int i=0;i<h.size();i++)m.put(h.get(i).trim().toLowerCase(Locale.ROOT),i);return m;}
    private static String get(List<String> c,int i){return i>=0&&i<c.size()?c.get(i):"";}
    static List<String> parseCsv(String s){List<String> out=new ArrayList<>();StringBuilder b=new StringBuilder();boolean quoted=false;for(int i=0;i<s.length();i++){char ch=s.charAt(i);if(ch=='"'){if(quoted&&i+1<s.length()&&s.charAt(i+1)=='"'){b.append('"');i++;}else quoted=!quoted;}else if(ch==','&&!quoted){out.add(b.toString());b.setLength(0);}else b.append(ch);}out.add(b.toString());return out;}
}
