# OnosNetworkUserLimiter

Network User Limiter is a behavior that limits the users’ bandwidth and bans the users from sending any data when they use more traffic bandwidth than allowed. The flowchart of the aforementioned behavior is as follows.

As it is shown in the figure, in the main loop, the application calculates the current bandwidth of the links that are connected to the users and the edge switch. In the next step, it checks if the currently used bandwidth is greater than the predefined threshold (we used 10Mb/s as the threshold). After that, it checks if there is any flow rule for the user with action=DROP. This condition prevents the duplicate rule installation. The next steps before the rule installation are increasing the penalty time and save it to the database. Each time the user exceeds the bandwidth usage limitation, the penalty time increases by 10 seconds. In the end, a new rule would install into the switch. Notice that the priority of the flow rule is set to 60010. The reason is to give higher priority to this new rule compare to other rules that initially have been installed by the controller to the switch.

To implement this algorithm, we used a developed java application on top of the ONOS SDN controller. Also, the links’ bit rate monitoring window interval is 5 seconds

![Alt text](GithubReadmeResource/Picture1.png?raw=true "UserLimitter")


## Topology

![Alt text](GithubReadmeResource/topo.png?raw=true "Topology")


Regarding the topology, there is 2 users and 3 Iperf servers. The User Limiter application only installs rules to the edge switch (in our case switch S2). So, we are going to monitor the flow table of this switch for the evaluation of my developed application. At the beginning of the scenario, there are only default rules in the flow table of this switch.

![Alt text](GithubReadmeResource/p2.png?raw=true "Topology")

Now let’s try to send ping traffic from user 1 (10.0.0.1) to server 1 (10.0.0.3).

![Alt text](GithubReadmeResource/p3.png?raw=true "Topology")

![Alt text](GithubReadmeResource/p4.png?raw=true "Topology")

Two new rules have been added to the switch (Notice that all the rules that the ONOS controller adds to the switch for forwarding the packets, have an idle timeout of 10 seconds by default). The User Limiter application’s log is shown below.


![Alt text](GithubReadmeResource/p5.png?raw=true "Topology")

The bandwidth of the ping traffic is about 600 ~ 999 b/s in our case so the threshold (which is 10Mb/s in our case) did not trigger. 

Now let’s send more traffic to server 1 by using the Iperf tools.

![Alt text](GithubReadmeResource/p6.png?raw=true "Topology")

![Alt text](GithubReadmeResource/p7.png?raw=true "Topology")


In the above figure, rules #2 and #3 are the default rules that ONOS controllers added to the switch in order to forward the Iperf traffic. However, as the traffic bandwidth increases, the bandwidth became greater than the predefined threshold (10Mb/s), so the User Limiter application installed the new rule (Rule #1) with the DROP action to the switch with the higher priority than Rule #2 and #3. This resulted in the packet drop for user 1. The User limiter application’s log is as follows.

![Alt text](GithubReadmeResource/p8.png?raw=true "Topology")

In the above log, “penalty time 10”, indicates the duration that the user will be banned from sending any data to the network. As we mentioned before, the idle timeout for the flow with action drop is set equal to the penalty time and it increases by 10 for each time that the user exceeds the bandwidth limitation. 
For the last test let’s send Iperf traffic again.

![Alt text](GithubReadmeResource/p9.png?raw=true "Topology")

![Alt text](GithubReadmeResource/p10.png?raw=true "Topology")

![Alt text](GithubReadmeResource/p11.png?raw=true "Topology")


To sum up, the User Limiter application is a developed Java application that is running on top of the ONOS SDN controller. It limits the usage of network bandwidth for each user when they use more traffic bandwidth than allowed. This behavior is implemented by defining a threshold and compare the current user link’s bandwidth with the threshold. If the bandwidth is greater than the threshold, a new flow rule with action=DROP will be installed to the switch to prevent the user to send traffic. Moreover, the duration of this prevention will increase by 10 seconds each time that the user exceeds the allowed bandwidth.


- Demo on YouTube:
    - https://youtu.be/rk-YflnSPXc