# ns-3 setup and GUI integration (Windows + WSL2)

## What this integration does

The Java GUI launches the project's `ns-3/scratch/intent-lifecycle.cc`, waits for the run, and writes a log to `results/ns3-run.log` plus flow-level KPI CSV to `results/ns3-intent-kpis.csv`. On Windows, the adapter supports launching a Linux ns-3 tree through `wsl.exe` when `NS3_ROOT` is set to a Linux path such as `/home/<linux-user>/ns-3-dev`. The Java project itself can remain in `C:\...`; the adapter maps that project path to `/mnt/c/...` inside WSL.

**Important:** ns-3 is a separate simulator and is not included in the ZIP. This environment cannot install or compile your local ns-3 build, so the scenario must be compiled and run on your machine. The included C++ scenario is independently authored and needs validation against your chosen ns-3 release and the paper's exact setup before its results can be called a reproduction.

## Recommended Windows installation: WSL2 + Ubuntu

1. Open PowerShell as Administrator and run `wsl --install -d Ubuntu`, then restart if Windows asks.
2. Open Ubuntu from the Start menu and create your Linux username/password.
3. Install build dependencies in Ubuntu:

   ```bash
   sudo apt update
   sudo apt install -y g++ cmake ninja-build git python3 python3-dev pkg-config sqlite3 libsqlite3-dev libeigen3-dev
   ```

4. Download an ns-3 release that includes the LTE/EPC, applications, FlowMonitor, and traffic-control modules. Use the official ns-3 installation documentation to select a release and follow its build instructions: https://www.nsnam.org/docs/installation/html/
5. Build ns-3 and verify the launcher from its root in Ubuntu:

   ```bash
   ./ns3 configure --enable-examples --enable-tests
   ./ns3 build
   ./ns3 show version
   ```

6. Before connecting Java, copy the scenario into your ns-3 tree and compile/run it directly. From the extracted Java project directory in Windows, its path is available to WSL as `/mnt/c/.../IntentResearchJava/ns-3/scratch/intent-lifecycle.cc`. Copy it to `<ns-3-root>/scratch/intent-lifecycle.cc`, then run:

   ```bash
   ./ns3 run "scratch/intent-lifecycle --nUes=4 --simTime=12 --voiceRate=256kbps --videoRate=4Mbps --output=intent-lifecycle-kpis.csv"
   ```

   If the C++ API differs for your selected ns-3 release, fix the compiler error shown by `./ns3 build` before running the Java adapter. Keep the ns-3 version and build log with your experiment.

## Connect the Java GUI to ns-3 on Windows

Set `NS3_ROOT` to the **Linux path inside WSL**, not the Windows path. This project defaults to `/home/Amali/ns-3.49` on Windows if the variable is not set; if your ns-3 folder differs, set it explicitly. Replace `your-linux-user` with your actual username:

```powershell
[Environment]::SetEnvironmentVariable('NS3_ROOT', '/home/your-linux-user/ns-3-dev', 'User')
$env:NS3_ROOT = '/home/your-linux-user/ns-3-dev'
```

Close and reopen VS Code/PowerShell if you set the persistent User variable. Launch the Java GUI from the extracted project folder. Enter your intent in **Intent lifecycle**, then click **Submit intent + run ns-3** (or use **Overview → Submit current intent + run ns-3**). The current submitted intent supplies the service name, target latency, target packet loss, priority, and bandwidth-derived traffic rate. You can also use **Dataset & ns-3 → Run ns-3 packet simulation** to rerun the most recently submitted intent. Check `results/ns3-run.log` first if it fails; a successful run should create `results/ns3-intent-kpis.csv`.

## Linux

Set `NS3_ROOT=/absolute/path/to/ns-3` and launch the Java application from the project root. The runner copies the scenario to the ns-3 `scratch/` directory, runs it, and records the log/CSV under the project `results/` directory.

## What the current scenario measures

FlowMonitor-derived transmit/receive/lost packets, mean flow delay, throughput, and packet-loss percentage. It is a starter LTE/EPC packet-level scenario with UDP voice-like and video-like flows and a FQ-CoDel root queue discipline on the wired backhaul. It does **not** yet implement the paper's complete service arrival process, calibrated 5G NSA topology, exact queue placements/configuration, or an optimizer that feeds measured ns-3 KPIs back into SPSA/MGDA. Those are explicit integration/calibration tasks, not claims of completed functionality.

## Troubleshooting

- `ns-3 is not configured`: set `NS3_ROOT` and restart the GUI.
- `wsl.exe not found`: enable/install WSL2 on Windows, or run the app on Linux.
- `No such file or directory`: confirm `NS3_ROOT` points to the directory containing `./ns3`.
- Compile/API error: run the scenario directly from ns-3 and resolve compiler errors before Java integration.
- No CSV after a zero exit code: inspect `results/ns3-run.log`; the runner now reports this as a failed run.
- For research, keep raw logs, ns-3 version, run parameters, random seeds, and multiple repetitions; do not mix Java synthetic KPI values with ns-3 measured values.
