import java.util.ArrayList;
import java.util.Random;

//Work needed
public class Client {
    public static void main(String[] args) throws InterruptedException {
        NetworkUtility networkUtility = new NetworkUtility("127.0.0.1", 4444);
        System.out.println("Connected to server");
        /**
         * Tasks
         */
        
        /*
        1. Receive EndDevice configuration from server
        2. Receive active client list from server
        3. for(int i=0;i<100;i++)
        4. {
        5.      Generate a random message
        6.      Assign a random receiver from active client list
        7.      if(i==20)
        8.      {
        9.            Send the message and recipient IP address to server and a special request "SHOW_ROUTE"
        10.           Display routing path, hop count and routing table of each router [You need to receive
                            all the required info from the server in response to "SHOW_ROUTE" request]
        11.     }
        12.     else
        13.     {
        14.           Simply send the message and recipient IP address to server.
        15.     }
        16.     If server can successfully send the message, client will get an acknowledgement along with hop count
                    Otherwise, client will get a failure message [dropped packet]
        17. }
        18. Report average number of hops and drop rate
        */

        System.out.println("Connected to server");
        //Thread.sleep(10000);
        EndDevice endDevice = (EndDevice) networkUtility.read();


        int successCount = 0, dropCount = 0, totalHopCount = 0 ;


        for(int i=0;i<100;i++)
        {
            Thread.sleep(500);
            /*ArrayList<EndDevice> endDevices = (ArrayList<EndDevice>)networkUtility.read();
            System.out.println("*******"+endDevices.size());

            Random random = new Random(System.currentTimeMillis());
            int r = Math.abs(random.nextInt(endDevices.size()));
            System.out.println("random device : "+ r);
            */
            EndDevice recerver_device = (EndDevice) networkUtility.read();

            String massage = "massage sent from client"+ endDevice.getIpAddress() ;
            Packet packet =
                    new Packet(massage,null,endDevice.getIpAddress(),recerver_device.getIpAddress());

            if (i == 20) {
                packet.setSpecialMessage("SHOW_ROUTE");
                networkUtility.write(packet);
                //networkUtility.read();
            }
            else {
                networkUtility.write(packet);
            }

            String acknowledgement = (String)networkUtility.read();
            if (acknowledgement.equals("Delivered")){
                int hopCount = (int)networkUtility.read();
                System.out.println((i)+ "  massage delivered  & hopCount = "+hopCount );
                totalHopCount+= hopCount;
                successCount++ ;

                if (i == 20){
                    String routing_path = (String)networkUtility.read();
                    ArrayList<Integer> routersId = (ArrayList<Integer>) networkUtility.read();
                    ArrayList<RoutingTableEntry>[] routingTables =
                            (ArrayList<RoutingTableEntry>[]) networkUtility.read() ;

                    System.out.println("routing path  :  "+routing_path);
                    for (int j = 0; j < routingTables.length; j++) {
                        System.out.println(routersId.get(j));
                        for (int k = 0; k < routingTables[j].size(); k++) {
                            System.out.println(routingTables[j].get(k));
                        }
                    }

                }
            }else if (acknowledgement.equals("Dropped")) {
                System.out.println((i)+ "  massage dropped ");
                dropCount++ ;
            }
        }

        double avg_number_hop = (totalHopCount* 1.0)/successCount;
        double drop_rate = (dropCount*1.0)/100 ;
        System.out.println("average number of hops : "+avg_number_hop);
        System.out.println("Drop rate : "+ drop_rate);

    }
}
