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

import Gamelogic.GameManager;

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
    private double initialStart, gameStartTime;

    //game stuff
    Queue<InetAddress> answerOrder = new LinkedList<>(); //using player number 
    HashMap players = new HashMap(); 

    //constructor
    public GameServer(){

        this.initialStart = System.currentTimeMillis();
        this.gameStartTime = initialStart + 20000; //universal start time
        this.currQuestion = 0;

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
            byte[] readBuffer = new byte[200];

            

            while(true){ //game true

                if(System.currentTimeMillis() < this.gameStartTime){

                } else {

                    //code here :3

                    //while currQ < 20
                    
                    

                }




                //confirm
                writer.println("timer"); //timestamps
                writer.println("Q"); //curr question #

                //client measures if 
                
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


        } catch (SocketException e) {
            //active player no longer active, shut down thread but keep score & ip in game manager
            //brady hashmap of player id - ip num, curr score
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        GameManager game = new GameManager(null); //send hashmap
        server.createSocketThreads();
    }

}


/*
 * random shit
 * 
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class ClientWindow implements ActionListener
{
	private JButton poll;
    private JButton submit;
    private JRadioButton options[];
    private ButtonGroup optionGroup;
    private JLabel question;
    private JLabel timer;
    private JLabel score;
    private TimerTask clock;
    private int scoreNum;

    private JFrame window;
    private DatagramSocket socket;
    private Socket socketTCP = null;
    private InetAddress serverAddress;
    private int serverPort = 5000;
    private boolean pollAllowed = true;
    private boolean hasPolled = false;
    private int selectedAnswer = -1;

    private InputStream inStream = null;
    private OutputStream outStream = null;
	
	private static SecureRandom random = new SecureRandom();
	
	// write setters and getters as you need
	
	public ClientWindow(String serverIp)
	{
		try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(serverIp);
        } catch (Exception e) {
            e.printStackTrace();
        }

		JOptionPane.showMessageDialog(window, "Video Game Trivia");
		window = new JFrame("Video game Trivia");



		question = new JLabel("Waiting for question...");
        question.setBounds(10, 5, 350, 100);
        window.add(question);
		
		options = new JRadioButton[4];
        optionGroup = new ButtonGroup();
        for (int index = 0; index < options.length; index++) {
            options[index] = new JRadioButton("Option " + (index + 1));
            options[index].addActionListener(this);
            options[index].setBounds(10, 110 + (index * 20), 350, 20);
            window.add(options[index]);
            optionGroup.add(options[index]);
        }

		timer = new JLabel("TIMER");  // represents the countdown shown on the window
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(30);  // represents clocked task that should run after X seconds
		Timer t = new Timer();  // event generator
		t.schedule(clock, 0, 1000); // clock is called every second
		window.add(timer);
		
		scoreNum = 0;
		score = new JLabel("SCORE: " + scoreNum); // represents the score
		score.setBounds(50, 250, 100, 20);
		window.add(score);

		poll = new JButton("Poll");  // button that use clicks/ like a buzzer
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this);  // calls actionPerformed of this class
		window.add(poll);
		
		submit = new JButton("Submit");  // button to submit their answer
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this);  // calls actionPerformed of this class
		window.add(submit);
		
		
		window.setSize(400,400);
		window.setBounds(50, 50, 400, 400);
		window.setLayout(null);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);
	}

	// this method is called when you check/uncheck any radio button
	// this method is called when you press either of the buttons- submit/poll
	@Override
	public void actionPerformed(ActionEvent e)
	{
		System.out.println("You clicked " + e.getActionCommand());
		
		// input refers to the radio button you selected or button you clicked
		String input = e.getActionCommand();  
		switch (input) {
            case "Option 1":
                selectedAnswer = 0;
                break;
            case "Option 2":
                selectedAnswer = 1;
                break;
            case "Option 3":
                selectedAnswer = 2;
                break;
            case "Option 4":
                selectedAnswer = 3;
                break;
            case "Poll":
                if (pollAllowed) {
                    sendUDP("POLL"); //place holder name 
                    hasPolled = true;
                    disableAll();
                    submit.setEnabled(true);
                    poll.setEnabled(false);
                } else {
                    sendUDP("TOO_LATE"); //place holder name 
                }
                break;
            case "Submit":
                if (hasPolled && selectedAnswer != -1) {
                    sendUDP("ANSWER:" + selectedAnswer); //place holder name 
                    disableAll();
                }
                break;
            default:
                System.out.println("Incorrect Option");
        }
    }
	private void sendUDP(String msg) {
        try {
            byte[] buffer = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recieveTCP() {
        //stay open listen to server        
        while (socket.isConnected()) 
        {
            try 
            {
                inStream = socketTCP.getInputStream();
                outStream = socketTCP.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socketTCP.getInputStream()));
                PrintWriter writer = new PrintWriter(socketTCP.getOutputStream(), true);
                byte[] readBuffer = new byte[200];

                String message = "Client joined";
                System.out.println(message);
                Protocol packet = new Protocol(InetAddress.getLocalHost(), serverAddress, socket.getLocalPort(), (Integer) 1987, (double) System.currentTimeMillis(), message);
                writer.println(packet.getData());

                        
            }
            catch (SocketException se)
            {
                System.exit(0);
            }
            catch (IOException i) 
            {
                i.printStackTrace();
            }
        }
    }
              


    public void createSocket()
    {
        try 
        {
        	//fetch the streams
            inStream = socketTCP.getInputStream();
            outStream = socketTCP.getOutputStream();
            recieveTCP();
        } 
        catch (UnknownHostException u) 
        {
            u.printStackTrace();
        } 
        catch (IOException io) 
        {
            io.printStackTrace();
        }
    }

    private void disableAll() {
        for (JRadioButton option : options) {
            option.setEnabled(false);
        }
    }
	
	// this class is responsible for running the timer on the window
	public class TimerCode extends TimerTask {
        private int duration;

        public TimerCode(int duration) {
            this.duration = duration;
        }

        @Override
        public void run() {
            if (duration < 0) {
                timer.setText("Timer expired");
                window.repaint();
                this.cancel();
                pollAllowed = false;
                submit.setEnabled(false);
                return;
            }

            timer.setForeground(duration < 6 ? Color.red : Color.black);
            timer.setText(duration + "");
            duration--;
            window.repaint();
        }
    }
	
}
 */