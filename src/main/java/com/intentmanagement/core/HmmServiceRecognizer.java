package com.intentmanagement.core;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Bootstrapped first-order HMM classifier for service-intent recognition.
 * This is a transparent baseline, not the paper's original trained NER model: the
 * paper-specific labelled corpus/training settings are not distributed in this project.
 * States are service classes; observations are normalized tokens. Add labelled utterances
 * from the target domain to improve coverage and evaluate on a held-out set.
 */
public final class HmmServiceRecognizer {
    private static final String UNKNOWN = "<UNK>";
    private static final Pattern TOKEN = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private final List<String> states = new ArrayList<>();
    private final Map<String,Double> start = new HashMap<>();
    private final Map<String,Map<String,Double>> transitions = new HashMap<>();
    private final Map<String,Map<String,Double>> emissions = new HashMap<>();
    private final Map<String,Double> emissionTotals = new HashMap<>();
    private final Map<String,Double> transitionTotals = new HashMap<>();
    private final Map<String,Double> startTotals = new HashMap<>();
    private int vocabSize = 1;

    private record Example(String state, String text) {}

    public HmmServiceRecognizer() {
        List<Example> seeds = List.of(
            new Example("Emergency", "emergency ambulance public safety mission critical push to talk critical alert responder"),
            new Example("URLLC", "ultra reliable low latency industrial control gaming real time control cloud gaming"),
            new Example("Voice", "voice voip audio call conference call phone call speech communication"),
            new Example("Video", "video streaming video call video conference live broadcast online lecture"),
            new Example("mMTC", "iot sensor telemetry smart meter smart building machine type device monitoring"),
            new Example("eMBB", "web browsing file transfer download upload cloud backup high bandwidth bulk data"),
            new Example("General", "network service connectivity reliable request normal internet access")
        );
        Set<String> vocabulary = new HashSet<>();
        for (Example e : seeds) {
            states.add(e.state());
            List<String> tokens = tokenize(e.text());
            startTotals.merge(e.state(), 1.0, Double::sum);
            for (String token : tokens) {
                vocabulary.add(token);
                emissions.computeIfAbsent(e.state(), k -> new HashMap<>()).merge(token, 1.0, Double::sum);
                emissionTotals.merge(e.state(), 1.0, Double::sum);
            }
            List<String> expanded = new ArrayList<>(); expanded.add("<BOS>"); expanded.addAll(tokens); expanded.add("<EOS>");
            for (int i=1;i<expanded.size();i++) {
                String pair = expanded.get(i-1) + "\u0000" + expanded.get(i);
                transitions.computeIfAbsent(e.state(), k -> new HashMap<>()).merge(pair, 1.0, Double::sum);
                transitionTotals.merge(e.state(), 1.0, Double::sum);
            }
        }
        vocabSize = vocabulary.size() + 1;
    }

    public String recognize(String text) {
        List<String> obs = tokenize(text);
        if (obs.isEmpty()) return "General";
        String best = "General"; double bestScore = Double.NEGATIVE_INFINITY;
        // A left-to-right HMM Viterbi path over service states. Transition evidence rewards
        // repeated context tokens, while emission likelihood captures service vocabulary.
        int n=states.size(), m=obs.size();
        double[][] dp = new double[m][n]; int[][] back = new int[m][n];
        for (int s=0;s<n;s++) dp[0][s] = logProb(startTotals.getOrDefault(states.get(s),0.0)+1,
                startTotals.values().stream().mapToDouble(Double::doubleValue).sum()+n)
                + emissionLog(states.get(s),obs.get(0));
        for (int t=1;t<m;t++) for(int s=0;s<n;s++) {
            String state=states.get(s); double emit=emissionLog(state,obs.get(t));
            double max=Double.NEGATIVE_INFINITY; int arg=0;
            for(int prev=0;prev<n;prev++) {
                String prevState=states.get(prev);
                double trans=transitionLog(prevState,obs.get(t-1),obs.get(t));
                double score=dp[t-1][prev]+trans+emit;
                if(score>max){max=score;arg=prev;}
            }
            dp[t][s]=max;back[t][s]=arg;
        }
        int stateIndex=0; for(int s=1;s<n;s++) if(dp[m-1][s]>dp[m-1][stateIndex])stateIndex=s;
        best=states.get(stateIndex); bestScore=dp[m-1][stateIndex];
        // Backtrack to keep Viterbi execution explicit; final state is the sequence classification.
        for(int t=m-1;t>0;t--) stateIndex=back[t][stateIndex];
        return bestScore==Double.NEGATIVE_INFINITY?"General":best;
    }

    private double emissionLog(String state,String token){
        double count=emissions.getOrDefault(state,Map.of()).getOrDefault(token,0.0);
        return Math.log((count+1.0)/(emissionTotals.getOrDefault(state,0.0)+vocabSize));
    }
    private double transitionLog(String state,String previous,String current){
        String key=previous+"\u0000"+current;
        double count=transitions.getOrDefault(state,Map.of()).getOrDefault(key,0.0);
        return Math.log((count+1.0)/(transitionTotals.getOrDefault(state,0.0)+vocabSize));
    }
    private static double logProb(double n,double d){return Math.log(n/d);}
    private static List<String> tokenize(String s){
        String normalized=s.toLowerCase(Locale.ROOT).replace("ultra-reliable","ultra reliable").replace("push-to-talk","push to talk");
        List<String> out=new ArrayList<>(); for(String t:TOKEN.split(normalized)) if(!t.isBlank())out.add(t); return out;
    }
}
