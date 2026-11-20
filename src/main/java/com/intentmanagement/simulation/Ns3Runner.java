package com.intentmanagement.simulation;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** Launches the included ns-3 scenario on Linux, or through WSL2 when the Java GUI runs on Windows. */
public class Ns3Runner {
    public record RunResult(int exitCode, Path log, Path csv, String message) {}

    public RunResult run(Path ns3Root, int ueCount, int seconds) throws IOException, InterruptedException {
        return run(ns3Root, ueCount, seconds, "256kbps", "4Mbps", "General", 150.0, 1.0, 1);
    }

    public RunResult run(Path ns3Root, int ueCount, int seconds, String voiceRate, String videoRate) throws IOException, InterruptedException {
        return run(ns3Root, ueCount, seconds, voiceRate, videoRate, "General", 150.0, 1.0, 1);
    }

    public RunResult run(Path ns3Root, int ueCount, int seconds, String voiceRate, String videoRate,
                         String serviceType, double targetLatencyMs, double targetLossPercent, int priority)
            throws IOException, InterruptedException {
        if (ns3Root == null || ns3Root.toString().isBlank())
            throw new IOException("Set NS3_ROOT to the ns-3 root directory. On Windows, use its Linux WSL path, e.g. /home/<user>/ns-3-dev.");
        int users = Math.max(1, Math.min(100, ueCount));
        int duration = Math.max(3, Math.min(3600, seconds));
        Path project = Path.of(".").toAbsolutePath().normalize();
        Path source = project.resolve("ns-3").resolve("scratch").resolve("intent-lifecycle.cc");
        if (!Files.isRegularFile(source)) throw new IOException("Scenario source not found: " + source);
        Path results = project.resolve("results"); Files.createDirectories(results);
        Path csv = results.resolve("ns3-intent-kpis.csv");
        Path log = results.resolve("ns3-run.log");
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        String configuredRoot = ns3Root.toString().replace('\\', '/');
        boolean wsl = windows && configuredRoot.startsWith("/");
        ProcessBuilder pb;
        String safeVoiceRate = sanitizeRate(voiceRate, "256kbps");
        String safeVideoRate = sanitizeRate(videoRate, "4Mbps");
        String safeService = serviceType == null ? "General" : serviceType.replaceAll("[^A-Za-z0-9_-]", "");
        String args = "scratch/intent-lifecycle --nUes=" + users + " --simTime=" + duration
                + " --voiceRate=" + safeVoiceRate + " --videoRate=" + safeVideoRate
                + " --intentService=" + safeService + " --targetLatencyMs=" + Math.max(0.1, targetLatencyMs)
                + " --targetLossPct=" + Math.max(0.0, Math.min(100.0, targetLossPercent))
                + " --intentPriority=" + Math.max(1, Math.min(5, priority)) + " --output=";
        if (wsl) {
            String projectLinux = toWslPath(project.toString());
            String csvLinux = toWslPath(csv.toString());
            String root = shellQuote(configuredRoot);
            String command = "set -e; cd " + root + "; test -x ./ns3 || test -f ./ns3; " +
                    "mkdir -p scratch; cp " + shellQuote(projectLinux + "/ns-3/scratch/intent-lifecycle.cc") + " scratch/intent-lifecycle.cc; " +
                    "./ns3 run " + shellQuote(args + csvLinux);
            pb = new ProcessBuilder("wsl.exe", "bash", "-lc", command);
        } else {
            Path rootPath = ns3Root.toAbsolutePath().normalize();
            if (!Files.isDirectory(rootPath)) throw new IOException("NS3_ROOT does not exist: " + rootPath + ". For Windows, use a WSL Linux path such as /home/<user>/ns-3-dev.");
            Path launcher = rootPath.resolve(windows ? "ns3.bat" : "ns3");
            if (!Files.exists(launcher)) throw new IOException("No ns-3 launcher found at " + launcher + ". Check the root path and ns-3 installation.");
            Path scratch = rootPath.resolve("scratch"); Files.createDirectories(scratch);
            Files.copy(source, scratch.resolve("intent-lifecycle.cc"), StandardCopyOption.REPLACE_EXISTING);
            String command = args + csv.toAbsolutePath().toString().replace('\\', '/');
            pb = windows ? new ProcessBuilder("cmd", "/c", "ns3.bat", "run", command)
                         : new ProcessBuilder("./ns3", "run", command);
            pb.directory(rootPath.toFile());
        }
        pb.redirectErrorStream(true); pb.redirectOutput(log.toFile());
        Process process;
        try { process = pb.start(); }
        catch (IOException ex) { throw new IOException("Could not start ns-3. Check NS3_ROOT and the installation. Details: " + ex.getMessage(), ex); }
        boolean done = process.waitFor(15, TimeUnit.MINUTES);
        if (!done) { process.destroyForcibly(); throw new IOException("ns-3 timed out after 15 minutes. Inspect " + log.toAbsolutePath()); }
        int code = process.exitValue();
        if (code == 0 && !Files.isRegularFile(csv)) code = 2;
        String message = code == 0 ? "ns-3 run completed. KPI CSV: " + csv.toAbsolutePath()
                : "ns-3 failed (exit code " + code + "). Inspect log: " + log.toAbsolutePath();
        return new RunResult(code, log, csv, message);
    }

    private static String toWslPath(String path) throws IOException {
        String normalized = path.replace('\\', '/');
        if (normalized.matches("^[A-Za-z]:/.*")) return "/mnt/" + Character.toLowerCase(normalized.charAt(0)) + normalized.substring(2);
        if (normalized.startsWith("/")) return normalized;
        throw new IOException("Cannot convert path to WSL format: " + path);
    }
    private static String sanitizeRate(String rate, String fallback) {
        if (rate == null || !rate.matches("[0-9]+(?:\\.[0-9]+)?(?:bps|Kbps|Mbps|Gbps|kbps|mbps|gbps)")) return fallback;
        return rate;
    }
    private static String shellQuote(String s) { return "'" + s.replace("'", "'\\''") + "'"; }

    public static void main(String[] args) throws Exception {
        String root = System.getenv("NS3_ROOT");
        if (root == null || root.isBlank()) throw new IllegalArgumentException("Set NS3_ROOT to the ns-3 root. On Windows, use the WSL Linux path, e.g. /home/<user>/ns-3-dev.");
        int ues = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 12;
        RunResult r = new Ns3Runner().run(Path.of(root), ues, seconds);
        System.out.println(r.message());
        if (r.exitCode() != 0) System.exit(r.exitCode());
    }
}
