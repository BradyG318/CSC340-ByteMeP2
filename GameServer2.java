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
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Gamelogic.GameManager;

public class GameServer2 {

    private static ExecutorService executorService;
    private ServerSocket serverSocket = null;
    private DatagramSocket pollSocket = null;
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private double initialStart, gameStartTime, pollEndTime, ansEndTime;
    private int currQuestion;
    private boolean isPollTime, isAnswerTime;
    private GameManager game;

    private ConcurrentHashMap<String, Double> clientToPollTimes;
    private ConcurrentHashMap<Double, String> pollTimesToClient; 


    //game server init
    public GameServer2(){
        //init server and timer thread
        executorService = Executors.newFixedThreadPool(2);

        //game time
        this.initialStart = System.currentTimeMillis();
        this.gameStartTime = initialStart + 20000; //universal start time (20s after server open)    
        
        //init times
        pollEndTime = 0;
        ansEndTime = 0;

        //game stats in manager
        game = new GameManager(); //send hashmap   
        currQuestion = 0;

        //open sockets
        try {
            serverSocket = new ServerSocket(1987);
            pollSocket = new DatagramSocket(1983);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //thread init for udp and tcp
    public void createSocketThreads(){
        try {
            while (true) {
                //udp (poll answer listener)
                Thread pollHandlerThread = new Thread(() -> handleUDP());
                pollHandlerThread.start();

                //tcp (client handler)
                while(true){
                    Socket tcpSocket = serverSocket.accept();
                    Thread clientThread = new Thread(() -> handleTCPClient(tcpSocket));
                    clientThread.start();
                }    
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void handleUDP(){
        //listen, should only get for button so stay open

        //if recieve, open packet and read file information: getIP- ip getfile- send timestamp
        //put in weighted q, weigh by timestamp 

        //poll recieve - update client last question

        try {
            byte[] incomingData = new byte[1024];
            while (true) {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                pollSocket.receive(incomingPacket);

                Protocol pollData = new Protocol(incomingPacket);
                //store ip in queue

                //UDP OutOfOrder prevention
                if(clientToPollTimes.get(pollData.getID()) == null || clientToPollTimes.get(pollData.getID()) > pollData.getPacketNum()) {
                    clientToPollTimes.put(pollData.getID(), pollData.getPacketNum());
                    pollTimesToClient.put(pollData.getPacketNum(), pollData.getID());
                }
                
                Thread.sleep(2000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    //for all tcps
    public void handleTCPClient(Socket tcpSocket){
        String client; //get id from protocol
        this.game.addPlayer();
        //while open 



        //if timerEnd != null
        //if timerEnd < currTime, then wait; if end check q if first ack, else -ack
        if(System.currentTimeMillis() > pollEndTime && isPollTime){
            //check if in queue
        }

        //if ansEnd != nulldo nothing
        //else if  ansEnd < currTime stream ans = or != corr, return pts (stor pts)
        //else if currTime > ansEnd then return -20pts and store
        if(System.currentTimeMillis() > ansEndTime && isAnswerTime){
            //check if in queue
        }

        //if gameend 

        try{
            while(socket.isConnected()){
                //init input and output details
                inStream = socket.getInputStream();
                outStream = socket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(tcpSocket.getOutputStream(), true);
                byte[] readBuffer = new byte[200];
                //connect to client
                
                //game running
                while(currQuestion < 20){
                    //send question
                    //writer.println(game.getQuestion(currQuestion););
                    //writer.println(game.getAnswers(currQuestion););

                    //wait until timer up
                    while(pollEndTime < System.currentTimeMillis()){
                        //polling 
                    }


                    //if first q, ack, else ()
                    //loop, find lowest player time, use hash to get ip
                    


                    //if ans = ans(currQuestion)

                }
            
                
            }


        } catch(SocketException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //for server, knows and sets all timestamps
    public void serverGame(){
        while(true){
            if(System.currentTimeMillis() < gameStartTime){ //wait for ppl to join
                //do nothing
            } else {
                while (currQuestion < 20){
                    game.startRound();
                    clientToPollTimes = new ConcurrentHashMap();
                    pollTimesToClient = new ConcurrentHashMap();


                    //set question then notify all
                    //         set question as currQuestion

                    //set times
                    pollEndTime = System.currentTimeMillis() + 15000;
                    ansEndTime = pollEndTime + 10000;

                    //makes polltime true if it's not after poll time
                    while(pollEndTime < System.currentTimeMillis()){
                        isPollTime = true;
                    }
                    isPollTime = false;
                    game.buzzIn(pollTimesToClient.get(Collections.min(clientToPollTimes.values())));

                    //makes answertime true if its not after answer time
                    while(ansEndTime < System.currentTimeMillis()){
                        isAnswerTime = true;
                    }
                    isAnswerTime = false;

                }

                //send winner
            }
        }
        
    }



     /**
     * Starts all threads for the server.
     */
    public void start() {
        System.out.println("Starting...");

        executorService.submit(() -> {
            //server
            serverGame();
        });

        executorService.submit(() -> {
            //udp and tcp
            createSocketThreads();
        });

    }

    
}
