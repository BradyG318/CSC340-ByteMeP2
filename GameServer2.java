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
import Gamelogic.Player;
import Gamelogic.Question;

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
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //for all tcps
    public void handleTCPClient(Socket tcpSocket){
        String client = tcpSocket.getInetAddress().getHostAddress() + ":" + tcpSocket.getPort(); //get id from protocol
        this.game.addPlayer(new Player(client, 0, currQuestion));
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
                    if(game.clientActivityPassThrough(client)) {
                        Question question = game.startRound();

                    //send question
                    Protocol packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), question.getQuestion());
                    writer.println(packet.getData());
                    packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), question.getAnswers());
                    writer.println(packet.getData());

                    //wait until timer up
                    while(pollEndTime < System.currentTimeMillis() && isPollTime){
                        //polling 
                    }
                                        
                    //if this person is first
                    if(game.getAnsweringID().equals(client)){
                        packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "ack");
                        writer.println(packet.getData());
                    } else {
                        packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "-ack");
                        writer.println(packet.getData());
                    }

                        //answer time
                        while(System.currentTimeMillis() > ansEndTime && isAnswerTime){
                            //if this person is polling
                            if(game.getAnsweringID().equals(client)){
                                //read in answers
                                reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                                readBuffer = new byte[200];

                                //put readbuffer into packet and get answer number, put answer number in game.answer();
                                String line;
                                if ((line = reader.readLine()) != null) {
                                    System.out.println("Received: " + line); //debug
                        
                                    
                                }
                            }
            
                        } 
                    } else {
                        game.killPlayer(client);
                        //Add thread kill stuff here
                    }

                    


                    //if first q, ack, else ()
                    //loop, find lowest player time, use hash to get ip
                    


                    //if ans = ans(currQuestion)
                    

                }
            
                
            }


        } catch(SocketException e){

            //kick out person / mark inactive
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
                    game.buzzIn(pollTimesToClient.get(Collections.min(clientToPollTimes.values())));
                    isPollTime = false;

                    //makes answertime true if its not after answer time
                    while(ansEndTime < System.currentTimeMillis()){
                        isAnswerTime = true;
                    }
                    isAnswerTime = false;
                    game.endRound();
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
