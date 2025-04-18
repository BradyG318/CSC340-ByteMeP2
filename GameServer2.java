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

public class GameServer2 {

    private static ExecutorService executorService;
    private ServerSocket serverSocket = null;
    private DatagramSocket pollSocket = null;
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private volatile double initialStart, gameStartTime, pollEndTime, ansEndTime;
    private int currQuestion, playerID;
    private volatile boolean isPollTime, isAnswerTime;
    private boolean hasBeenPolled;
    private GameManager game;

    private ConcurrentHashMap<String, Double> clientToPollTimes;
    private ConcurrentHashMap<Double, String> pollTimesToClient; 


    /**
     * Initialize game server
     */
    public GameServer2(){
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


        //game stats in manager
 
        game = new GameManager(); //send hashmap   
        currQuestion = 0;
        playerID = 0;


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
                // Thread.currentThread().notifyAll();
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
                    game.getPlayer(client).heartbeat(currQuestion);
                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * TCP client maker
     * @param tcpSocket
     */
    public void handleTCPClient(Socket tcpSocket){
        //Make player
        this.playerID ++;
        String client = "Player " + playerID;
        this.game.addPlayer(new Player(client, 0, currQuestion));

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
                
                //game running
                while(currQuestion < 20){

                    System.out.println(client + " status=" + game.clientActivityPassThrough(client));
                    System.out.println(client + " last Q num=" + game.getPlayer(client).getLastQAns());
                    if(game.clientActivityPassThrough(client)) {
                        Question question = game.startRound();
                        Protocol packet;

                        //send question                 
                        String[] questionSend = {"q", question.getQuestion()}; 

                        //send answer
                        String[] answerSend =   {"answer", null, null, null, null};
                        String[] pollAnswers = question.getAnswers();
                        for(int ansEnter = 1; ansEnter < 5; ansEnter++){
                            answerSend[ansEnter] = pollAnswers[ansEnter-1];
                        }
                        
                        packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), questionSend);
                        writer.println(packet.getData());
                        packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), answerSend);
                        writer.println(packet.getData());    

                        //wait to start
                        while(System.currentTimeMillis() < gameStartTime){
                            //wait for game start
                        }
                                                   
                        //wait until timer up
                        while(pollEndTime > System.currentTimeMillis()){
                            //polling 
                            // System.out.println("waiting");
                        }

                        if(hasBeenPolled){
                            while(!isAnswerTime){
                            //System.out.println("oblivion");
                            }
                        }
                        


                        if(game.getAnsweringID() != null) {    
                            String tempAnsweringID = game.getAnsweringID();              
                            //if this person is first
                            if(tempAnsweringID.equals(client)){
                                packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "ack");
                                writer.println(packet.getData());
                            } else {
                                packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "-ack");
                                writer.println(packet.getData());
                            }

                            //answer time
                            if(ansEndTime > System.currentTimeMillis()){
                                //if this person is polling
                                if(tempAnsweringID.equals(client)){
                                    //read in answers
                                    //reader = new BufferedReader(new InputStreamReader(inStream));
                                    readBuffer = new byte[200];

                                    //put readbuffer into packet and get answer number, put answer number in game.answer();                                    
                                    String line = reader.readLine();

                                    //System.out.println(" at");
                                    //System.out.println("DEBUG: Reader=" + line);
                                    if (line != null) {
                                        //System.out.println("Received: " + line); //debug
                                        
                                        //unpack packet   
                                        Protocol unloadPacket = new Protocol(line);
                                        //unloadPacket.protocolDetails();   
                                        
                                        //send answer
                                        //System.out.println(unloadPacket.files() + "  228");

                                        if(unloadPacket.files().equals("No Answer")){
                                            //do nothing
                                        } else {
                                            int number = Integer.parseInt(unloadPacket.files());
                                            //game.answer(number);
                                            //send correct/incorrect
                                            if(game.answer(number)){
                                                packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "correct");
                                                writer.println(packet.getData());                                            
                                            } else {
                                                packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "wrong");
                                                writer.println(packet.getData());
                                            }

                                        }
                                    


                                        
                        
                                    } else {System.out.println("Reader != null");} //It's somehow skipping this line
                                
                                    // String score = "score," + game.getPlayer(client).getScore();
                                    // packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), score);
                                    // writer.println(packet.getData());
                                } else {
                                    //System.out.println("Skipping Everything Cuz Bugged"); 
                                }    
                                
                                //System.out.println("stuck at 251?");
                            } //end
                            //System.out.println("outside if 253");
                            
                            //if answer in time? return score to player
                            //System.out.println("DEBUG: Client = " + client);
                            if(tempAnsweringID.equals(client)){
                                game.forcePtCalc();
                                //System.out.println("DEBUG: Sending " + client + " score");
                                String score = "score," + game.getPlayer(client).getScore();
                                packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), score);
                                writer.println(packet.getData());
                                writer.flush();
                                //System.out.println("Sending score at 261");
                                //System.out.println("DEBUG: Internal Score for " + client + "=" + game.getPlayer(client).getScore());
                            }
                        }

                        while(isAnswerTime){
                            //wait
                        }

                        //send next
                        packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "next");
                        writer.println(packet.getData());


                    } else if(game.clientActivityPassThrough(client) == null) {
                        //Thread go bye bye here
                    } else {
                        //Send killswitch msg
                        game.killPlayer(client);
                        System.out.println("Killing " + client + " for disconnection");
                        Protocol packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "kill");
                        writer.println(packet.getData());
                        Thread.currentThread().stop();
                    }

                    if(!game.getPlayer(client).isActive()) {
                        System.out.println("Killing " + client + " for inactivity");
                        Protocol packet = new Protocol(InetAddress.getLocalHost(), tcpSocket.getInetAddress(), (Integer) 1987, (Integer) tcpSocket.getPort(), (double) System.currentTimeMillis(), "kill");
                        writer.println(packet.getData());
                        Thread.currentThread().stop();
                    }                  

                }
                
                //after game

                //print out scores
                //print out winner
                
            
                
            }


        } catch(SocketException e){
            game.killPlayer(client);
            //kick out person / mark inactive
            System.out.println(client + " has left the game");
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    
    /**
     * Runs game as server
     */
    public void serverGame(){
        while(true){
            if(System.currentTimeMillis() < gameStartTime){ //wait for ppl to join
                //do nothing
            } else {
                System.out.println("Game Start!");
                pollEndTime = System.currentTimeMillis() + 15000;
                ansEndTime = pollEndTime + 10000;

                while (currQuestion < 20){
                    //start times
                    pollEndTime = System.currentTimeMillis() + 15000;
                    ansEndTime = pollEndTime + 10000;


                    //init game
                    System.out.println("Round Num: " + currQuestion);
                    game.startRound();
                    clientToPollTimes = new ConcurrentHashMap<String, Double>();
                    pollTimesToClient = new ConcurrentHashMap<Double, String>();

                    System.out.println("Polling Time!");
                    //makes polltime true if it's not after poll time
                    while(pollEndTime > System.currentTimeMillis()){
                        isPollTime = true;
                        //System.out.println(System.currentTimeMillis());
                    }
                    System.out.println("Polling Complete");
                    isPollTime = false;
                    
                    if(hasBeenPolled) {
                        System.out.println("Answer time!");
                        game.buzzIn(pollTimesToClient.get(Collections.min(clientToPollTimes.values())));


                        //if poller exists aka nobody polls or not
                        //makes answertime true if its not after answer time
                        while(ansEndTime > System.currentTimeMillis()){
                            isAnswerTime = true;
                        }
                        System.out.println(" Answer time complete");
                        isAnswerTime = false;
                    } else {System.out.println("No polls submitted, sending next");}

                    System.out.println("Round Complete");
                    hasBeenPolled = false;
                    currQuestion++;
                    game.endRound();
                }

                //send winner
                System.out.println("Game Over");
                double expire = System.currentTimeMillis() + 100000; //after 100 sec
                while(expire > System.currentTimeMillis()){
                    //wait
                }

                System.out.println(game.endGame());
                System.exit(-1);
            }
        }
        
    }



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