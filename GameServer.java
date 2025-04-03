import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {

    //listen thread

    //new thread per client?
    /**
     * store the client ip
     * 
     * 1) client connect, send connect tcp
     * 2) server handshake w question info and timestamp
     * 3) client udp for poll push
     * 4) server (make queue of IPs)
     * 5) if ip first at timer end, allow submit (else, disable buttons)
     * 6) 
     * 
     */

    //TCP
    /**
     * 
     */

     // Datagram socket for communication
    DatagramSocket socket = null;

    // Maps to keep track of client statuses
    HashMap<InetAddress, Integer> map = new HashMap<>();
    HashMap<InetAddress, String> mapAvailable = new HashMap<>();
    HashMap<InetAddress, String> fileMap = new HashMap<>();

    // Scheduled executor service for periodic tasks
    ScheduledExecutorService timer = Executors.newScheduledThreadPool(2);

    /**
     * Constructor for UDPServer class.
     */
    public GameServer() {
        // Empty constructor
    }

    /**
     * Starts the server timer to periodically check the heartbeat of clients.
     */
    public void serverTimer() {
        // Schedule a task to check if clients are alive every 30 seconds
        timer.scheduleAtFixedRate(() -> {
            for (Map.Entry<InetAddress, Integer> entry : map.entrySet()) {
                if (entry.getValue() >= 30) {
                    mapAvailable.put(entry.getKey(), " - Dead");
                }
            }
            System.out.println("Current connections: " + mapAvailable);
        }, 0, 30, TimeUnit.SECONDS);

        // Schedule a task to increment the heartbeat counter every second
        timer.scheduleAtFixedRate(() -> {
            for (Map.Entry<InetAddress, Integer> entry : map.entrySet()) {
                map.put(entry.getKey(), entry.getValue() + 1);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Creates a socket and listens for incoming messages from clients.
     */
    public void createAndListenSocket() {
        try {
            // Create a datagram socket bound to port 9876
            socket = new DatagramSocket(9876);
            byte[] incomingData = new byte[1024];

            while (true) {
                // Receive incoming packet
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                socket.receive(incomingPacket);
                String message = new String(incomingPacket.getData());
                InetAddress IPAddress = incomingPacket.getAddress();
                int port = incomingPacket.getPort();

                // Print received message and client details
                System.out.println("Received message from client: " + message);
                System.out.println("Client IP: " + IPAddress.getHostAddress());
                System.out.println("Client port: " + port);

                // Update client status
                map.put(IPAddress, 0);
                mapAvailable.put(IPAddress, " - Alive");
                

                // Process the incoming packet using OurProtocol
                Protocol deconstructPacket = new Protocol(incomingPacket);
                deconstructPacket.protocolDetails();
                fileMap.put(IPAddress, deconstructPacket.files().toString());

                // Build a string with IP, active/deactive, and files
                String nodeDetails = "";
                for (InetAddress ip : map.keySet()) {
                    String availability = mapAvailable.get(ip);
                    String files = fileMap.get(ip);
                    nodeDetails += "IP: " + ip.getHostAddress() + " " + availability
                            + " - " + files + "$";
                }


                // Send reply to client
                byte[] data = nodeDetails.getBytes();
                Protocol replyPacket = new Protocol(IPAddress, InetAddress.getLocalHost(), port, 1010, 1, nodeDetails);
                socket.send(replyPacket.getPacket());

                // Sleep for 2 seconds before processing the next packet
                Thread.sleep(2000);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    
    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.serverTimer();
        server.createAndListenSocket();
    }
    
}
