package apps.amirNetworkUserLimiter.src.main.java.org.onosproject.amirNetworkUserLimiter;

import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;


@Component(immediate = true)
public class AmirNetworkUserLimiter {
    private final Logger log = LoggerFactory.getLogger(getClass());


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private LimiterTask task = new LimiterTask();




    @Activate
    protected void activate() {

        log.info("Started");

        try {
            Timer timer = new Timer();
            task.setDelay(5);
            task.setExit(false);
            task.setLog(log);
            task.setTimer(timer);
            task.setDeviceService(deviceService);
            task.setHostService(hostService);
            task.setTopologyService(topologyService);
            task.setFlowRuleService(flowRuleService);
            task.setAppId(coreService.registerApplication("org.onosproject.amirNetworkUserLimiter"));
            task.schedule();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Deactivate
    protected void deactivate() {
        task.setExit(true);
        task.getTimer().cancel();
        log.info("Stopped");
    }
}
