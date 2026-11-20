/*
 * Intent lifecycle packet-level scenario scaffold for ns-3 (C++).
 * This is an independent reproducible scenario, NOT the authors' original ns-3 source.
 * Requires an ns-3 build with LTE/EPC, applications, internet, flow-monitor and traffic-control modules.
 * Example: ./ns3 run "scratch/intent-lifecycle --nUes=4 --simTime=12 --voiceRate=256kbps --videoRate=4Mbps"
 */
#include "ns3/core-module.h"
#include "ns3/network-module.h"
#include "ns3/internet-module.h"
#include "ns3/point-to-point-module.h"
#include "ns3/applications-module.h"
#include "ns3/lte-module.h"
#include "ns3/flow-monitor-module.h"
#include "ns3/traffic-control-module.h"
#include "ns3/mobility-module.h"
#include <fstream>
#include <iomanip>
#include <cctype>

using namespace ns3;
NS_LOG_COMPONENT_DEFINE("IntentLifecycleScenario");

int main(int argc, char *argv[]) {
  uint32_t nUes = 4;
  double simTime = 12.0;
  std::string voiceRate = "256kbps";
  std::string videoRate = "4Mbps";
  std::string output = "intent-lifecycle-kpis.csv";
  std::string intentService = "General";
  double targetLatencyMs = 150.0;
  double targetLossPct = 1.0;
  uint32_t intentPriority = 1;
  CommandLine cmd(__FILE__);
  cmd.AddValue("nUes", "Number of UE nodes", nUes);
  cmd.AddValue("simTime", "Simulation duration in seconds", simTime);
  cmd.AddValue("voiceRate", "UDP offered rate for voice-like flow", voiceRate);
  cmd.AddValue("videoRate", "UDP offered rate for video-like flow", videoRate);
  cmd.AddValue("output", "CSV output path", output);
  cmd.AddValue("intentService", "Service class from the submitted Java intent", intentService);
  cmd.AddValue("targetLatencyMs", "Requested latency target (metadata for KPI comparison)", targetLatencyMs);
  cmd.AddValue("targetLossPct", "Requested packet-loss target (metadata for KPI comparison)", targetLossPct);
  cmd.AddValue("intentPriority", "Intent priority from 1 to 5", intentPriority);
  cmd.Parse(argc, argv);
  if (nUes < 1 || nUes > 100 || simTime < 3.0) {
    NS_FATAL_ERROR("nUes must be in [1,100] and simTime >= 3 seconds");
  }

  Ptr<LteHelper> lte = CreateObject<LteHelper>();
  Ptr<PointToPointEpcHelper> epc = CreateObject<PointToPointEpcHelper>();
  lte->SetEpcHelper(epc);
  lte->SetSchedulerType("ns3::PfFfMacScheduler");
  lte->SetEnbDeviceAttribute("DlBandwidth", UintegerValue(50));
  lte->SetEnbDeviceAttribute("UlBandwidth", UintegerValue(50));

  NodeContainer ueNodes; ueNodes.Create(nUes);
  NodeContainer enbNodes; enbNodes.Create(1);
  Ptr<Node> pgw = epc->GetPgwNode();
  NodeContainer remoteNodes; remoteNodes.Create(1);
  Ptr<Node> remoteHost = remoteNodes.Get(0);
  InternetStackHelper internet; internet.Install(remoteNodes);
  PointToPointHelper p2ph;
  p2ph.SetDeviceAttribute("DataRate", StringValue("1Gbps"));
  p2ph.SetChannelAttribute("Delay", StringValue("5ms"));
  NetDeviceContainer internetDevices = p2ph.Install(pgw, remoteHost);
  Ipv4AddressHelper ipv4h; ipv4h.SetBase("1.0.0.0", "255.0.0.0");
  Ipv4InterfaceContainer internetIfaces = ipv4h.Assign(internetDevices);
  Ipv4StaticRoutingHelper routing;
  Ptr<Ipv4StaticRouting> remoteRouting = routing.GetStaticRouting(remoteHost->GetObject<Ipv4>());
  remoteRouting->AddNetworkRouteTo(Ipv4Address("7.0.0.0"), Ipv4Mask("255.0.0.0"), 1);

  MobilityHelper mobility;
  mobility.SetMobilityModel("ns3::ConstantPositionMobilityModel");
  mobility.Install(enbNodes); mobility.Install(ueNodes);
  NetDeviceContainer enbDevs = lte->InstallEnbDevice(enbNodes);
  NetDeviceContainer ueDevs = lte->InstallUeDevice(ueNodes);
  internet.Install(ueNodes);
  Ipv4InterfaceContainer ueIfaces = epc->AssignUeIpv4Address(ueDevs);
  for (uint32_t i=0;i<nUes;i++) {
    Ptr<Ipv4StaticRouting> ueRouting = routing.GetStaticRouting(ueNodes.Get(i)->GetObject<Ipv4>());
    ueRouting->SetDefaultRoute(epc->GetUeDefaultGatewayAddress(), 1);
  }
  lte->Attach(ueDevs, enbDevs.Get(0));

  // Do not install a second root queue disc: EPC/InternetStack may already
  // have installed one on these devices. Traffic profile is selected per CSV intent.
  std::string serviceLower = intentService;
  for (char &ch : serviceLower) ch = static_cast<char>(std::tolower(static_cast<unsigned char>(ch)));
  const bool videoProfile = serviceLower.find("video") != std::string::npos ||
      serviceLower.find("embb") != std::string::npos || serviceLower.find("file") != std::string::npos ||
      serviceLower.find("transfer") != std::string::npos;
  const bool bothProfiles = serviceLower == "general" || serviceLower.empty();
  uint16_t voicePort = 9000, videoPort = 9100;
  ApplicationContainer sinks;
  for (uint32_t i=0;i<nUes;i++) {
    if (!videoProfile || bothProfiles) {
      PacketSinkHelper voiceSink("ns3::UdpSocketFactory", InetSocketAddress(Ipv4Address::GetAny(), voicePort+i));
      sinks.Add(voiceSink.Install(ueNodes.Get(i)));
    }
    if (videoProfile || bothProfiles) {
      PacketSinkHelper videoSink("ns3::UdpSocketFactory", InetSocketAddress(Ipv4Address::GetAny(), videoPort+i));
      sinks.Add(videoSink.Install(ueNodes.Get(i)));
    }
  }
  sinks.Start(Seconds(0.5)); sinks.Stop(Seconds(simTime));
  for (uint32_t i=0;i<nUes;i++) {
    if (!videoProfile || bothProfiles) {
      OnOffHelper voice("ns3::UdpSocketFactory", InetSocketAddress(ueIfaces.GetAddress(i), voicePort+i));
      voice.SetAttribute("DataRate", DataRateValue(DataRate(voiceRate)));
      voice.SetAttribute("PacketSize", UintegerValue(160));
      voice.SetAttribute("OnTime", StringValue("ns3::ConstantRandomVariable[Constant=1]"));
      voice.SetAttribute("OffTime", StringValue("ns3::ConstantRandomVariable[Constant=0]"));
      ApplicationContainer va=voice.Install(remoteHost); va.Start(Seconds(1.0)); va.Stop(Seconds(simTime-0.2));
    }
    if (videoProfile || bothProfiles) {
      OnOffHelper video("ns3::UdpSocketFactory", InetSocketAddress(ueIfaces.GetAddress(i), videoPort+i));
      video.SetAttribute("DataRate", DataRateValue(DataRate(videoRate)));
      video.SetAttribute("PacketSize", UintegerValue(1200));
      video.SetAttribute("OnTime", StringValue("ns3::ConstantRandomVariable[Constant=1]"));
      video.SetAttribute("OffTime", StringValue("ns3::ConstantRandomVariable[Constant=0]"));
      ApplicationContainer vi=video.Install(remoteHost); vi.Start(Seconds(1.2)); vi.Stop(Seconds(simTime-0.2));
    }
  }

  FlowMonitorHelper flowHelper;
  Ptr<FlowMonitor> monitor=flowHelper.InstallAll();
  Simulator::Stop(Seconds(simTime)); Simulator::Run();
  monitor->CheckForLostPackets();
  std::ofstream csv(output);
  csv << "flow_id,intent_service,intent_priority,target_latency_ms,target_packet_loss_pct,source,destination,tx_packets,rx_packets,lost_packets,mean_delay_ms,throughput_mbps,packet_loss_pct\n";
  Ptr<Ipv4FlowClassifier> classifier=DynamicCast<Ipv4FlowClassifier>(flowHelper.GetClassifier());
  std::map<FlowId, FlowMonitor::FlowStats> stats=monitor->GetFlowStats();
  for (const auto &entry: stats) {
    Ipv4FlowClassifier::FiveTuple t=classifier->FindFlow(entry.first);
    const auto &s=entry.second;
    double delayMs=s.rxPackets ? 1000.0*s.delaySum.GetSeconds()/s.rxPackets : 0.0;
    double throughput=s.timeLastRxPacket.GetSeconds()>s.timeFirstTxPacket.GetSeconds() ? (s.rxBytes*8.0)/(s.timeLastRxPacket.GetSeconds()-s.timeFirstTxPacket.GetSeconds())/1e6 : 0.0;
    double loss=s.txPackets ? 100.0*static_cast<double>(s.lostPackets)/s.txPackets : 0.0;
    csv << entry.first << ',' << intentService << ',' << intentPriority << ',' << std::fixed << std::setprecision(6)
        << targetLatencyMs << ',' << targetLossPct << ',' << t.sourceAddress << ',' << t.destinationAddress << ','
        << s.txPackets << ',' << s.rxPackets << ',' << s.lostPackets << ',' << delayMs << ',' << throughput << ',' << loss << '\n';
  }
  csv.close(); monitor->SerializeToXmlFile("intent-lifecycle-flowmon.xml", true, true);
  Simulator::Destroy();
  std::cout << "ns-3 run complete. KPI CSV: " << output << "\n";
  return 0;
}
