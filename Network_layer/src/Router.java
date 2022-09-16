//Work needed
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Router {
    private int routerId;
    private int numberOfInterfaces;
    private ArrayList<IPAddress> interfaceAddresses;//list of IP address of all interfaces of the router
    private ArrayList<RoutingTableEntry> routingTable;//used to implement DVR
    private ArrayList<Integer> neighborRouterIDs;//Contains both "UP" and "DOWN" state routers
    private Boolean state;//true represents "UP" state and false is for "DOWN" state
    private Map<Integer, IPAddress> gatewayIDtoIP;

    public Router() {
        interfaceAddresses = new ArrayList<>();
        routingTable = new ArrayList<>();
        neighborRouterIDs = new ArrayList<>();

        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p < 0.80) state = true;
        else state = false;

        numberOfInterfaces = 0;
    }

    public Router(int routerId, ArrayList<Integer> neighborRouters,
                  ArrayList<IPAddress> interfaceAddresses, Map<Integer, IPAddress> gatewayIDtoIP) {
        this.routerId = routerId;
        this.interfaceAddresses = interfaceAddresses;
        this.neighborRouterIDs = neighborRouters;
        this.gatewayIDtoIP = gatewayIDtoIP;
        routingTable = new ArrayList<>();



        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p < 0.80) state = true;
        else state = false;

        numberOfInterfaces = interfaceAddresses.size();
    }

    @Override
    public String toString() {
        String string = "";
        string += "Router ID: " + routerId + "\n" + "Interfaces: \n";
        for (int i = 0; i < numberOfInterfaces; i++) {
            string += interfaceAddresses.get(i).getString() + "\t";
        }
        string += "\n" + "Neighbors: \n";
        for(int i = 0; i < neighborRouterIDs.size(); i++) {
            string += neighborRouterIDs.get(i) + "\t";
        }
        return string;
    }



    /**
     * Initialize the distance(hop count) for each router.
     * for itself, distance=0; for any connected router with state=true, distance=1; otherwise distance=Constants.INFTY;
     */
    public void initiateRoutingTable() {
        for (int i = 0; i < NetworkLayerServer.routers.size() ; i++) {
            if (routerId == NetworkLayerServer.routers.get(i).routerId)
            {
                RoutingTableEntry routingTableEntry = new RoutingTableEntry(routerId,0,routerId);
                routingTable.add(routingTableEntry);
            }
            else if(neighborRouterIDs.contains(NetworkLayerServer.routers.get(i).getRouterId()) &&
                    NetworkLayerServer.routers.get(i).getState())
            {
                RoutingTableEntry routingTableEntry =
                        new RoutingTableEntry(NetworkLayerServer.routers.get(i).getRouterId(),
                                1, NetworkLayerServer.routers.get(i).getRouterId());
                routingTable.add(routingTableEntry);
            }
            else
            {
                RoutingTableEntry routingTableEntry =
                        new RoutingTableEntry(NetworkLayerServer.routers.get(i).getRouterId(),Constants.INFINITY, -1 );
                routingTable.add(routingTableEntry);
            }
        }
    }

    /**
     * Delete all the routingTableEntry
     */
    public void clearRoutingTable() {
        /*routingTable.clear();
        for (int i = 0; i < NetworkLayerServer.routers.size() ; i++) {
            if (routerId == NetworkLayerServer.routers.get(i).routerId)
            {
                RoutingTableEntry routingTableEntry = new RoutingTableEntry(routerId,0,routerId);
                routingTable.add(routingTableEntry);
            }
            else
            {
                RoutingTableEntry routingTableEntry =
                        new RoutingTableEntry(routerId,Constants.INFINITY, -1 );
                routingTable.add(routingTableEntry);
            }
        }*/

        for (int i = 0; i < routingTable.size(); i++) {
            routingTable.get(i).setDistance(Constants.INFINITY);
            routingTable.get(i).setGatewayRouterId(-1);
        }
    }

    /**
     * Update the routing table for this router using the entries of Router neighbor
     * @param neighbor
     */
    public boolean updateRoutingTable(Router neighbor) {

        boolean changed = false ;
        ArrayList<RoutingTableEntry> neighborRoutingTable = neighbor.routingTable;
        if (neighbor.getState())
        {
            for (int i = 0; i < routingTable.size(); i++) {
                /*if(routingTable.get(i).getGatewayRouterId() != -1 &&
                        NetworkLayerServer.routerMap.get(routingTable.get(i).getGatewayRouterId()).getState() == false)
                {
                    routingTable.get(i).setDistance(Constants.INFINITY);
                    routingTable.get(i).setGatewayRouterId(-1);
                    changed = true ;
                }*/
                double distanceThroughNeighbour = neighborRoutingTable.get(i).getDistance() + 1 ;
                if (distanceThroughNeighbour > Constants.INFINITY)
                    distanceThroughNeighbour = Constants.INFINITY;
                if (distanceThroughNeighbour < routingTable.get(i).getDistance())
                {
                    routingTable.get(i).setGatewayRouterId(neighbor.routerId);
                    routingTable.get(i).setDistance(distanceThroughNeighbour);
                    changed = true ;
                }
            }
        }


        return changed ;
    }

    public boolean sfupdateRoutingTable(Router neighbor) {
        boolean changed = false ;
        ArrayList<RoutingTableEntry> neighborRoutingTable = neighbor.routingTable;
        if (neighbor.getState())
        {
            for (int i = 0; i < neighborRoutingTable.size() && i < NetworkLayerServer.routerCount; i++) {

                //System.out.println("DDDDDDDDDDDDD "+ i);
                //System.out.println(neighborRoutingTable.get(i));
                //System.out.println(neighborRoutingTable.get(i).getDistance());
                double distanceThroughNeighbour = neighborRoutingTable.get(i).getDistance() + 1 ;
                if (distanceThroughNeighbour > Constants.INFINITY)
                    distanceThroughNeighbour = Constants.INFINITY;

                System.out.println(i);
                if ((distanceThroughNeighbour < routingTable.get(i).getDistance()         //split horizon
                        && neighborRoutingTable.get(i).getGatewayRouterId()!= routerId )
                        ||
                        (routingTable.get(i).getGatewayRouterId() == neighbor.routerId
                                && routingTable.get(i).getDistance() !=distanceThroughNeighbour))    //forced update
                {
                    routingTable.get(i).setGatewayRouterId(neighbor.routerId);
                    routingTable.get(i).setDistance(distanceThroughNeighbour);
                    changed = true ;
                }
            }
        }

        return changed ;

    }

    /**
     * If the state was up, down it; if state was down, up it
     */
    public void revertState() {
        state = !state;
        if(state) { initiateRoutingTable(); }
        else { clearRoutingTable(); }
    }

    public int getRouterId() {
        return routerId;
    }

    public void setRouterId(int routerId) {
        this.routerId = routerId;
    }

    public int getNumberOfInterfaces() {
        return numberOfInterfaces;
    }

    public void setNumberOfInterfaces(int numberOfInterfaces) {
        this.numberOfInterfaces = numberOfInterfaces;
    }

    public ArrayList<IPAddress> getInterfaceAddresses() {
        return interfaceAddresses;
    }

    public void setInterfaceAddresses(ArrayList<IPAddress> interfaceAddresses) {
        this.interfaceAddresses = interfaceAddresses;
        numberOfInterfaces = interfaceAddresses.size();
    }

    public ArrayList<RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public void addRoutingTableEntry(RoutingTableEntry entry) {
        this.routingTable.add(entry);
    }

    public ArrayList<Integer> getNeighborRouterIDs() {
        return neighborRouterIDs;
    }

    public void setNeighborRouterIDs(ArrayList<Integer> neighborRouterIDs) { this.neighborRouterIDs = neighborRouterIDs; }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }

    public Map<Integer, IPAddress> getGatewayIDtoIP() { return gatewayIDtoIP; }

    public void printRoutingTable() {
        System.out.println("Router " + routerId);
        System.out.println("DestID Distance Nexthop");
        for (RoutingTableEntry routingTableEntry : routingTable) {
            System.out.println(routingTableEntry.getRouterId() + " " + routingTableEntry.getDistance() + " " + routingTableEntry.getGatewayRouterId());
        }
        System.out.println("-----------------------");
    }
    public String strRoutingTable() {
        String string = "Router" + routerId + "\n";
        string += "DestID Distance Nexthop\n";
        for (RoutingTableEntry routingTableEntry : routingTable) {
            string += routingTableEntry.getRouterId() + " " + routingTableEntry.getDistance() + " " + routingTableEntry.getGatewayRouterId() + "\n";
        }

        string += "-----------------------\n";
        return string;
    }

}
