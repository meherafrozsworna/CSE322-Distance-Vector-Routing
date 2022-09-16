

//import sun.jvm.hotspot.debugger.win32.coff.DebugVC50ReservedTypes;

import java.util.ArrayList;
import java.util.Random;

public class ServerThread implements Runnable {

    NetworkUtility networkUtility;
    EndDevice endDevice;
    //Display routing path, hop count and routing table of each router
    String routing_path = "";
    int hop_count = 0 ;
    ArrayList<RoutingTableEntry>[] routingTables = new ArrayList[NetworkLayerServer.routers.size()] ;


    ServerThread(NetworkUtility networkUtility, EndDevice endDevice) {
        this.networkUtility = networkUtility;
        this.endDevice = endDevice;
        System.out.println("Server Ready for client " + NetworkLayerServer.clientCount);
        NetworkLayerServer.clientCount++;
        new Thread(this).start();
    }

    @Override
    public void run() {
        /**
         * Synchronize actions with client.
         */
        
        /*
        Tasks:
        1. Upon receiving a packet and recipient, call deliverPacket(packet)
        2. If the packet contains "SHOW_ROUTE" request, then fetch the required information
                and send back to client
        3. Either send acknowledgement with number of hops or send failure message back to client
        */

        //System.out.println("Sending EndDevice configuration to client");
        networkUtility.write(endDevice);

        int p = 0 ;
        while (p < 100)
        {
            p++ ;
            //System.out.println("Sending active client list to client");
            hop_count = 0 ;
            System.out.println("end ........"+NetworkLayerServer.endDevices.size());
            Random random = new Random(System.currentTimeMillis());
            int r = Math.abs(random.nextInt(NetworkLayerServer.endDevices.size()));
            System.out.println("random device : "+ r);
            EndDevice receiver_device = NetworkLayerServer.endDevices.get(r);
            networkUtility.write(receiver_device);

            Packet packet = (Packet)networkUtility.read() ;
            System.out.println("packet recieved");
            boolean packetDelivered = false;
            try {
                packetDelivered = deliverPacket(packet);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(packetDelivered) {
                networkUtility.write("Delivered");
                networkUtility.write(hop_count);
                /*
                Display routing path, hop count and routing table of each router [You need to receive
                            all the required info from the server in response to "SHOW_ROUTE" request]
                 */
                if (packet.getSpecialMessage() != null) {
                    networkUtility.write(routing_path);
                    ArrayList<Integer> routersId = new ArrayList<>();
                    for (int i = 0; i < routingTables.length; i++) {
                        routersId.add(NetworkLayerServer.routers.get(i).getRouterId());
                        routingTables[i] = NetworkLayerServer.routers.get(i).getRoutingTable() ;
                    }
                    networkUtility.write(routersId);
                    networkUtility.write(routingTables);
                }
            }
            else{
                networkUtility.write("Dropped");
            }
        }

    }


    public Boolean deliverPacket(Packet p) throws InterruptedException {


        
        /*
        1. Find the router s which has an interface
                such that the interface and source end device have same network address.
        2. Find the router d which has an interface
                such that the interface and destination end device have same network address.
        3. Implement forwarding, i.e., s forwards to its gateway router x considering d as the destination.
                similarly, x forwards to the next gateway router y considering d as the destination,
                and eventually the packet reaches to destination router d.

            3(a) If, while forwarding, any gateway x, found from routingTable of router r is in down state[x.state==FALSE]
                    (i) Drop packet
                    (ii) Update the entry with distance Constants.INFTY
                    (iii) Block NetworkLayerServer.stateChanger.t
                    (iv) Apply DVR starting from router r.
                    (v) Resume NetworkLayerServer.stateChanger.t

            3(b) If, while forwarding, a router x receives the packet from router y,
                    but routingTableEntry shows Constants.INFTY distance from x to y,
                    (i) Update the entry with distance 1
                    (ii) Block NetworkLayerServer.stateChanger.t
                    (iii) Apply DVR starting from router x.
                    (iv) Resume NetworkLayerServer.stateChanger.t

        4. If 3(a) occurs at any stage, packet will be dropped,
            otherwise successfully sent to the destination router
        */
        // router er id theke router
        //device id theke router er id
        //enddevice id

        //System.out.println("**************"+ NetworkLayerServer.deviceIDtoRouterID.get(NetworkLayerServer.endDeviceMap.get(p.getSourceIP()).getDeviceID()));
        System.out.println("enter deliver packet");
        Router sourceRouter = NetworkLayerServer.routerMap.get(
                NetworkLayerServer.deviceIDtoRouterID.get(
                        NetworkLayerServer.endDeviceMap.get(p.getSourceIP()).getDeviceID())) ;

        /*Router sourceRouter = NetworkLayerServer.routerMap.get(
                NetworkLayerServer.deviceIDtoRouterID.get(endDevice.getDeviceID())) ;
                */

        int destRouterId = NetworkLayerServer.deviceIDtoRouterID.get(
                NetworkLayerServer.endDeviceMap.get(p.getDestinationIP()).getDeviceID()) ;
        Router destRouter = NetworkLayerServer.routerMap.get(destRouterId) ;


        routing_path = sourceRouter.getRouterId()+"" ;
        hop_count++ ;
        int gatewayRouterID = -1;
        int position_dest = -1 ;


        for (int i = 0; i < sourceRouter.getRoutingTable().size() ; i++) {
            if (sourceRouter.getRoutingTable().get(i).getRouterId() == destRouterId ) {
                gatewayRouterID = sourceRouter.getRoutingTable().get(i).getGatewayRouterId();
                position_dest = i ;
            }
        }

        Router prevRouter = sourceRouter ;
        while (gatewayRouterID != destRouterId)
        {
            hop_count++ ;
            Router gatewayRouter = null ;
            int prevRouterPosInGateway = -1 ;
            if (gatewayRouterID != -1){
                System.out.println("BBBBB"+ gatewayRouterID);
                gatewayRouter = NetworkLayerServer.routerMap.get(gatewayRouterID);
                //System.out.println("BBBBB"+ gatewayRouter);
                for (int i = 0; i < gatewayRouter.getRoutingTable().size(); i++) {
                    if (prevRouter.getRouterId() == gatewayRouter.getRoutingTable().get(i).getRouterId()){
                        prevRouterPosInGateway = i ;
                        break;
                    }
                }
                routing_path += "->"+ gatewayRouterID ;
            }else{
                NetworkLayerServer.stateChanger.suspend();
                //NetworkLayerServer.DVR(1);
                NetworkLayerServer.DVR(1);
                NetworkLayerServer.stateChanger.resume();
                System.out.println("1.....not reachable");

                return false ;
            }

            if (gatewayRouterID != -1 && NetworkLayerServer.routerMap.get(gatewayRouterID).getState() == false){
                System.out.println("packet dropped");
                prevRouter.getRoutingTable().get(position_dest).setDistance(Constants.INFINITY);
                prevRouter.getRoutingTable().get(position_dest).setGatewayRouterId(-1);
//                if (NetworkLayerServer.stateChanger != null)
//                    NetworkLayerServer.stateChanger.wait();
                NetworkLayerServer.stateChanger.suspend();
                NetworkLayerServer.DVR(prevRouter.getRouterId());
                NetworkLayerServer.stateChanger.resume();
                //NetworkLayerServer.simpleDVR(prevRouter.getRouterId());
//                if (NetworkLayerServer.stateChanger != null)
//                    NetworkLayerServer.stateChanger.notify();
                System.out.println("2.......not reachable");
                return false ;
            }
            else if(gatewayRouter.getRoutingTable().get(prevRouterPosInGateway).getDistance() == Constants.INFINITY){

                System.out.println("enter critical ................");
                gatewayRouter.getRoutingTable().get(prevRouterPosInGateway).setDistance(1);
                gatewayRouter.getRoutingTable().get(prevRouterPosInGateway).setGatewayRouterId(prevRouter.getRouterId());

//                if (NetworkLayerServer.stateChanger != null)
//                    NetworkLayerServer.stateChanger.wait();
                NetworkLayerServer.stateChanger.suspend();
                NetworkLayerServer.DVR(gatewayRouter.getRouterId());
                NetworkLayerServer.stateChanger.resume();
                //NetworkLayerServer.simpleDVR(gatewayRouter.getRouterId());
//                if (NetworkLayerServer.stateChanger != null)
//                    NetworkLayerServer.stateChanger.notify();

            }
            prevRouter = gatewayRouter ;
            gatewayRouterID = prevRouter.getRoutingTable().get(position_dest).getGatewayRouterId() ;

        }
        System.out.println("Complete");
        return true ;

    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }
}
