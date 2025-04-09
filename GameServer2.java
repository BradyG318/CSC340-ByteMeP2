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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Gamelogic.GameManager;
import Gamelogic.Player;
import Gamelogic.Question;
import Gamelogic.QuestionReader;

public class GameServer2 {

    private static ExecutorService executorService;
    private ServerSocket serverSocket = null;
    private DatagramSocket pollSocket = null;
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private double initialStart, gameStartTime, pollEndTime, ansEndTime;
    private volatile int currQuestion, playerID;
    private boolean isPollTime, isAnswerTime;
    private boolean hasBeenPolled;
    private GameManager game;

    private ConcurrentHashMap<String, Double> clientToPollTimes;
    private ConcurrentHashMap<Double, String> pollTimesToClient; 
    private volatile long sleepUntilTime;


    /**
     * Initialize game server
     */
    public GameServer2(){
        sleepUntilTime = System.currentTimeMillis();
        //init server and timer thread
        executorService = Executors.newFixedThreadPool(2);

        //game time
        this.initialStart = System.currentTimeMillis();
        this.gameStartTime = initialStart + 5000; //universal start time (20s after server open) 
        
        //init times
        pollEndTime = 0;
        ansEndTime = 0;

        //init misc
        hasBeenPolled = false;

        System.out.println("OOga Booga");
        //game stats in manager
        System.out.println("hello");
        game = new GameManager(); //send hashmap   
        currQuestion = 0;
        playerID = 0;
        System.out.println("Ooooooog");

        //open sockets
        try {
            serverSocket = new ServerSocket(1987);
            pollSocket = new DatagramSocket(1983);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create socket threads for UDP and TCP
     */
    public void createSocketThreads(){
        try {            
            //udp (poll answer listener)
            Thread pollHandlerThread = new Thread(() -> handleUDP());
            pollHandlerThread.start();

            //tcp (client handler)
            while(true){
                Socket tcpSocket = serverSocket.accept();
                Thread clientThread = new Thread(() -> handleTCPClient(tcpSocket));
                clientThread.start();
            }    
            
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    /**
     * Takes in UDP requests, in this case, any poll request. Only listens for UDP,
     */
    public void handleUDP(){
        //try socket
        try {
            while (true) {
                //receive packet
                byte[] incomingData = new byte[1024];
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                pollSocket.receive(incomingPacket);

                //take in data
                Protocol pollData = new Protocol(incomingPacket);
                String[] dataParts = pollData.files().split("%");
                String client = dataParts[1]; 
                
                //store client ID in queue
                if(clientToPollTimes.get(client) == null || clientToPollTimes.get(client) > pollData.getPacketNum()) {
                    hasBeenPolled = true;
                    clientToPollTimes.put(client, pollData.getPacketNum());
                    pollTimesToClient.put(pollData.getPacketNum(), client);
                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //for all tcps
    public void handleTCPClient(Socket tcpSocket) {

        // Wait for server to set a valid sleepUntilTime
        // Wait until it's actually time
        while (System.currentTimeMillis() < sleepUntilTime) {
            long wait = sleepUntilTime - System.currentTimeMillis();
            System.out.println("Waiting for game to start... TCP thread sleeping for " + wait + "ms");
            try {
                Thread.sleep(Math.min(wait, 100)); // sleep in small chunks to avoid oversleep
            } catch (InterruptedException e) {
                System.out.println("FUCK\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
                e.printStackTrace();
            }
        }

        System.out.println("DEBUG: Escaping First While");
        //Make player
        this.playerID ++;
        String client = "Player " + playerID;
        this.game.addPlayer(new Player(client, 0, currQuestion));
        
            
        //if timerEnd != null
        //if timerEnd < currTime, then wait; if end check q if first ack, else -ack
        // if(System.currentTimeMillis() > pollEndTime && isPollTime){
        //     //check if in queue
        // }

        //if ansEnd != nulldo nothing
        //else if  ansEnd < currTime stream ans = or != corr, return pts (stor pts)
        //else if currTime > ansEnd then return -20pts and store
        // if(System.currentTimeMillis() > ansEndTime && isAnswerTime){
        //     //check if in queue
        // }

        //if gameend 

        try{
            while(tcpSocket.isConnected()){
                //initial input and output details
                inStream = tcpSocket.getInputStream();
                outStream = tcpSocket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(tcpSocket.getOutputStream(), true);
                byte[] readBuffer = new byte[200];


                //send client ID
                Protocol name = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), client);
                writer.println(name.getData());
                
                long waitTime = sleepUntilTime-System.currentTimeMillis();
                System.out.println("DEBUG: TCP First Wait Time=" + waitTime);
                //game running
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                while(currQuestion < 20){
                    //System.out.println("DEBUG: In question loop");

                    if(game.clientActivityPassThrough(client)) {
                        Question question = QuestionReader.getQuestion(game.getCurQuestion());
                        Protocol packet;

                        //send question                        
                        packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), question.getQuestion());
                        writer.println(packet.getData());
                        packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), question.getAnswers());
                        writer.println(packet.getData());    
                        //System.out.println("bug galore");

                        //wait to start
                        // Thread.sleep(sleepUntilTime-System.currentTimeMillis());

                        //System.out.println("DEBUG: TCP GameStartTime");
                                                                         
                        //wait until timer up
                        

                        //System.out.println("DEBUG: TCP IsEndPollTime");
                        long waitTime2 = sleepUntilTime-System.currentTimeMillis();
                        System.out.println("DEBUG: TCP WAIT2 = " + waitTime2);
                        try {
                            Thread.sleep(waitTime2);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                        if(game.getAnsweringID() != null) {                 
                            //if this person is first
                            if(game.getAnsweringID().equals(client)){
                                packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "ack");
                                writer.println(packet.getData());
                            } else {
                                packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "-ack");
                                writer.println(packet.getData());
                            }

                            //answer time
                            while(ansEndTime > System.currentTimeMillis() && isAnswerTime){
                                //if this person is polling
                                if(game.getAnsweringID().equals(client)){
                                    //read in answers
                                    reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                                    readBuffer = new byte[200];

                                    //put readbuffer into packet and get answer number, put answer number in game.answer();
                                    String line;
                                    if ((line = reader.readLine()) != null) {
                                        System.out.println("Received: " + line); //debug
                                        
                                        //unpack packet   
                                        Protocol unloadPacket = new Protocol(line);   
                                        
                                        //send answer
                                        System.out.println(unloadPacket.files());
                                        int number = Integer.parseInt(unloadPacket.files());
                                        game.answer(number);

                                        //send correct/incorrect
                                        if(game.answer(number)){
                                            packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "correct");
                                            writer.println(packet.getData());
                                        } else {
                                            packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "wrong");
                                            writer.println(packet.getData());
                                        }
                                    }
                                }            
                            } 
                            
                            System.out.println("DEBUG: TCP EndOfAnswerTime");
                            //if answer in time? return score to player
                            if(game.getAnsweringID().equals(client)){
                                String score = game.getPlayer(client).getScore() + "";
                                packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), score);
                                writer.println(packet.getData());
                            }
                        }

                        //send next
                        packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "next");
                        writer.println(packet.getData());


                    } else if(game.clientActivityPassThrough(client) == null) {
                        //Thread go bye bye here
                    } else {
                        //Send killswitch msg
                        game.killPlayer(client);

                        Protocol packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "kill");
                        writer.println(packet.getData());
                    }

                                       

                }

                //after game

                //print out scores
                //print out winner
                
            
                
            }


        } catch(SocketException e){
            game.killPlayer(client);
            //kick out person / mark inactive
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //for server, knows and sets all timestamps
    public void serverGame() {
        // while(true){
            sleepUntilTime = System.currentTimeMillis()+10000;
            long waitTime = sleepUntilTime-System.currentTimeMillis();
            try {
                Thread.sleep(waitTime);
                System.out.println("DEBUG: Server firstWait=" + waitTime);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

                System.out.println("Game Start!");
                while (currQuestion < 20){
                    //System.out.println("DEBUG: Round Num: " + currQuestion);
                    game.startRound();
                    clientToPollTimes = new ConcurrentHashMap<String, Double>();
                    pollTimesToClient = new ConcurrentHashMap<Double, String>();


                    //set question then notify all
                    //         set question as currQuestion

                    //set times
                    System.out.println("DEBUG: Time in Milli=" + System.currentTimeMillis());
                    sleepUntilTime = System.currentTimeMillis() + 15000;
                    
                    //makes polltime true if it's not after poll time
                    try {
                        Thread.sleep(sleepUntilTime-(System.currentTimeMillis()));
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    System.out.println("DEBUG: Server PollEndTime");
                    //System.out.println("DEBUG: Polling Complete");
                    isPollTime = false;
                    
                    if(hasBeenPolled) {
                        System.out.println("answer time!");
                        game.buzzIn(pollTimesToClient.get(Collections.min(clientToPollTimes.values())));
                        //System.out.println("DEBUG: Making it past Buzz-In");

                        //System.out.println("do we get here");

                        //if poller exists aka nobody polls or not
                        //makes answertime true if its not after answer time
                        sleepUntilTime = System.currentTimeMillis() + 10000;
                        try {
                            Thread.sleep(sleepUntilTime-System.currentTimeMillis());
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        //System.out.println("DEBUG: Answer time complete");
                        System.out.println("DEBUG: TCP EndOfAnswerTime");
                        isAnswerTime = false;
                    } else {System.out.println("DEBUG: No polls submitted, sending next");}

                    System.out.println("DEBUG: Round Complete");
                    hasBeenPolled = false;
                    currQuestion++;
                    game.endRound();
                }

                //send winner
                System.out.println("Game Over");
            }
        //}
        
    



     /**
     * Starts all threads for the server.
     */
    public void start() {
        System.out.println("Starting...");

        //Thread 1
        executorService.submit(() -> {
            //server
            System.out.println("start server"); 

            serverGame();
        });

        //Thread(s) 2
        executorService.submit(() -> {
            //udp and tcp
            System.out.println("start threads"); 

            createSocketThreads();
        });

    }

    /**
     * Run server
     * @param args none needed
     */
    public static void main(String[] args){
        GameServer2 trivia = new GameServer2();
        trivia.start();
    } 
}