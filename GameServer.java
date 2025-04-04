import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
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
     * 6) server takes in answer 
     * 
     * 1 udp listen thread -- datagram socket
     * 
     */

    //TCP
    /**
     * 
     */

     // Datagram socket for communication
    private ServerSocket serverSocket = null;
    private DatagramSocket pollSocket = null;
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;

    //game stuff
    Queue<InetAddress> answerOrder = new LinkedList<>(); //using player number 
    ArrayList players = new ArrayList<>(5);

    //constructor
    public GameServer(){
        try {
            serverSocket = new ServerSocket(1987);
            pollSocket = new DatagramSocket(1983);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void createSocketThreads() {
        try {
            while (true) {
                //udp (poll answer listener)
                Thread pollHandlerThread = new Thread(() -> handleUDP());
                pollHandlerThread.start();

                //tcp (client handler)
                while(true){
                    Socket tcpSocket = serverSocket.accept();
                    Thread clientThread = new Thread(() -> handleTcpClient(tcpSocket));
                    clientThread.start();
                }
    
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    

    /**
     * Handles poll data from UDP packets
     */
    private void handleUDP() {
        try {
            byte[] incomingData = new byte[1024];
            while (true) {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                pollSocket.receive(incomingPacket);

                Protocol pollData = new Protocol(incomingPacket);
                //store ip in queue
                answerOrder.add(incomingPacket.getAddress());
                
                Thread.sleep(2000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleTcpClient(Socket tcpSocket){
        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(tcpSocket.getOutputStream(), true);

            while(true){ //game true

                //confirm
                writer.println("Handshake");

                byte[] readBuffer = new byte[200];
                //read the data from client
                int num = inStream.read(readBuffer);
                if (num > 0) {
                    byte[] arrayBytes = new byte[num];
                    System.arraycopy(readBuffer, 0, arrayBytes, 0, num);
                    String recvedMessage = new String(arrayBytes, "UTF-8");
                    System.out.println("Received message :" + recvedMessage);
                } 
                else {
                    notifyAll();
                }
    
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.createSocketThreads();
    }

}
