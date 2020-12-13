package apps.amirNetworkUserLimiter.src.main.java.org.onosproject.amirNetworkUserLimiter;

import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.util.*;

public class LimiterTask {


    private long LimitationThreshold = 10000000; /* 10 Mb/s MAX for each user   */
    String[] UserIpAddressRange = {"[10.0.0.1]", "[10.0.0.2]"};


    class Task extends TimerTask {

        public Device getDevice() {
            return device;
        }

        public DeviceService getDeviceService() {
            return deviceService;
        }

        // Previous Host and its flows
        private HostLocation previousLocation = null;


        public HostLocation getPreviousLocation() {
            return previousLocation;
        }

        public void setPreviousLocation(HostLocation previousLocation) {
            this.previousLocation = previousLocation;
        }

        public long getDelay() {
            return delay;
        }

        private Map<HostId, Integer> hostIdCurrentPenaltyTime = new HashMap<HostId, Integer>();
        private Map<HostId, Long> hostIdPrevBitrateMap = new HashMap<HostId, Long>();

        TrafficTreatment THREATMENTDROP = DefaultTrafficTreatment.builder()
                .build();

        @Override
        public void run() {
            List<String> UserList = Arrays.asList(UserIpAddressRange);
            while (!isExit()) {

                log.info("========================================");
                // Get hosts
                for (Host host : getHostService().getHosts()) {
                    // Check if the host is a user or not. we do not want to apply the rule for the servers
                    if (UserList.contains(host.ipAddresses().toString())) {
                        PortStatistics portstat = getDeviceService().getStatisticsForPort(host.location().deviceId(), host.location().port());
                        if (portstat != null) {
                            if (hostIdPrevBitrateMap.get(host.id()) != null) {
                                if (portstat.bytesReceived() >= hostIdPrevBitrateMap.get(host.id())) {
                                    Long currentBitrate = 8 * ((portstat.bytesReceived() - hostIdPrevBitrateMap.get(host.id())) / 5); /* Converting Byte to bit*/
                                    hostIdPrevBitrateMap.put(host.id(), portstat.bytesReceived());
                                    hostIdCurrentPenaltyTime.putIfAbsent(host.id(), 0);

                                    //For Illustration
                                    DecimalFormat formatter = new DecimalFormat("#,###.00");
                                    log.info("Current bit rate for Host " + host.ipAddresses().toString() + " is: " + formatter.format(currentBitrate) + " b/s");

                                    if (currentBitrate > LimitationThreshold) {
                                        // Find which destination this host is sending the data and block that destination
                                        boolean isDropRuleHasBeenAlreadyAdded = false;
                                        for (FlowEntry flowEntry : getFlowRuleService().getFlowEntries(host.location().deviceId())) {
                                            if(flowEntry.treatment().equals(THREATMENTDROP)){
                                                for (Criterion cr : flowEntry.selector().criteria()) {
                                                    if (cr.type() == Criterion.Type.ETH_SRC) {
                                                        if (((EthCriterion) cr).mac().equals(host.mac())) {
                                                            isDropRuleHasBeenAlreadyAdded = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (!isDropRuleHasBeenAlreadyAdded) {
                                            hostIdCurrentPenaltyTime.put(host.id(), hostIdCurrentPenaltyTime.get(host.id()) + 10);
                                            installRule(host, host.mac(), hostIdCurrentPenaltyTime.get(host.id()));
                                        }
                                    }
                                } else {
                                    log.info("Current bit rate for host " + host.ipAddresses().toString() + " is: " + "0 b/s");
                                }
                            } else {
                                hostIdPrevBitrateMap.putIfAbsent(host.id(), portstat.bytesReceived());
                            }
                        }
                    }
                }
                try {
                    Thread.sleep((getDelay() * 1000));
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void installRule(Host host, MacAddress srcMac, int penaltyTime) {
        log.info("Installing drop rules on switch " + host.location().deviceId().toString()
                + " for device " + host.ipAddresses().toString());
        log.info("The penalty time is " + penaltyTime);
        // Dropping rule Rule (Transmission)
        TrafficSelector selectorSending = DefaultTrafficSelector.builder()
                .matchEthSrc(srcMac)
                .matchInPort(host.location().port())
                .build();
        TrafficTreatment treatmentSending = DefaultTrafficTreatment.builder()
                .build();
        FlowRule flowRuleSending = DefaultFlowRule.builder()
                .forDevice(host.location().deviceId())
                .fromApp(getAppId())
                .withSelector(selectorSending)
                .withTreatment(treatmentSending)
                .withPriority(60010)
                .makeTemporary(penaltyTime)
                .build();
        FlowRuleOperations flowRuleOperationsSending = FlowRuleOperations.builder()
                .add(flowRuleSending)
                .build();
        flowRuleService.apply(flowRuleOperationsSending);

        previousHostFlowRules.add(flowRuleSending);
    }

    private Set<FlowRule> previousHostFlowRules = new HashSet<>();

    private FlowRuleService flowRuleService;

    public FlowRuleService getFlowRuleService() {
        return flowRuleService;
    }

    public void setFlowRuleService(FlowRuleService flowRuleService) {
        this.flowRuleService = flowRuleService;
    }

    private Iterable<FlowEntry> flowEntries;

    public Iterable<FlowEntry> getFlowEntries() {
        return flowEntries;
    }

    public void setFlowEntries(Iterable<FlowEntry> flowEntries) {
        this.flowEntries = flowEntries;
    }

    private PortNumber portNumber;

    public PortNumber getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(PortNumber portNumber) {
        this.portNumber = portNumber;
    }

    public void schedule() {
        this.getTimer().schedule(new Task(), 0, 1000);
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    private Timer timer = new Timer();

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    private Logger log;

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    private boolean exit;

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    private long delay;

    public PortStatistics getPortStats() {
        return portStats;
    }

    public void setPortStats(PortStatistics portStats) {
        this.portStats = portStats;
    }

    private PortStatistics portStats;

    public Long getPort() {
        return port;
    }

    public void setPort(Long port) {
        this.port = port;
    }

    private Long port;

    public DeviceService getDeviceService() {
        return deviceService;
    }

    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    protected DeviceService deviceService;

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }


    protected HostService hostService;

    public void setHostService(HostService hostService) {
        this.hostService = hostService;
    }

    public HostService getHostService() {
        return hostService;
    }


    protected TopologyService topologyService;

    public void setTopologyService(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    public TopologyService getTopologyService() {
        return topologyService;
    }

    private ApplicationId appId;

    public ApplicationId getAppId() {
        return appId;
    }

    public void setAppId(ApplicationId appId) {
        this.appId = appId;
    }

    private Device device;
}
