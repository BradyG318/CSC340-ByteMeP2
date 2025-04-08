
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class ClientWindow implements ActionListener {
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
    private InetAddress serverAddress;
    private int serverPort = 1983; // UDP port
    private Socket tcpSocket;
    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private String clientID;
    private boolean gameStarted = false;
    private boolean pollAllowed = true;
    private boolean hasPolled = false;
    private int selectedAnswer = -1;

    private static SecureRandom random = new SecureRandom();

    public ClientWindow(String serverIp) {
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(serverIp);
            tcpSocket = new Socket(serverIp, 1987); // TCP port
            tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);

            // Receive initial client ID from the server
            clientID = tcpIn.readLine();
            System.out.println("Connected to server with ID: " + clientID);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, "Error connecting to the server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
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
            options[index].setEnabled(false); // Initially disabled
        }

        timer = new JLabel("TIMER");
        timer.setBounds(250, 250, 100, 20);
        window.add(timer);

        scoreNum = 0;
        score = new JLabel("SCORE: " + scoreNum);
        score.setBounds(50, 250, 100, 20);
        window.add(score);

        poll = new JButton("Poll");
        poll.setBounds(10, 300, 100, 20);
        poll.addActionListener(this);
        window.add(poll);

        submit = new JButton("Submit");
        submit.setBounds(200, 300, 100, 20);
        submit.addActionListener(this);
        submit.setEnabled(false); // Initially disabled
        window.add(submit);

        window.setSize(400, 400);
        window.setBounds(50, 50, 400, 400);
        window.setLayout(null);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                try {
                    if (tcpSocket != null && !tcpSocket.isClosed()) {
                        tcpSocket.close();
                    }
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        });

        // Start a thread to listen for server messages
        new Thread(this::receiveServerMessages).start();
    }

    private void receiveServerMessages() {
        try {
            String serverMessage;
            while ((serverMessage = tcpIn.readLine()) != null) {
                System.out.println("Received from server: " + serverMessage);
                if (serverMessage.startsWith("QUESTION:")) {
                    String questionData = serverMessage.substring("QUESTION:".length());
                    String[] parts = questionData.split(":");
                    if (parts.length == 5) {
                        updateQuestion(parts[0], parts[1], parts[2], parts[3], parts[4]);
                        resetForNewQuestion();
                    } else {
                        System.err.println("Invalid QUESTION format: " + serverMessage);
                    }
                } else if (serverMessage.startsWith("TIMER:")) {
                    try {
                        int newDuration = Integer.parseInt(serverMessage.substring("TIMER:".length()));
                        resetTimer(newDuration);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid TIMER format: " + serverMessage);
                    }
                } else if (serverMessage.startsWith("SCORE:")) {
                    try {
                        scoreNum = Integer.parseInt(serverMessage.substring("SCORE:".length()));
                        score.setText("SCORE: " + scoreNum);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid SCORE format: " + serverMessage);
                    }
                } else if (serverMessage.equals("ROUND_END")) {
                    enablePolling();
                    submit.setEnabled(false);
                    hasPolled = false;
                    selectedAnswer = -1;
                    optionGroup.clearSelection();
                } else if (serverMessage.equals("GAME_OVER")) {
                    JOptionPane.showMessageDialog(window, "Game Over!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                    disableAll();
                    poll.setEnabled(false);
                    submit.setEnabled(false);
                } else if (serverMessage.equals("TOO_LATE_POLL")) {
                    JOptionPane.showMessageDialog(window, "Polling is closed for this question.", "Too Late", JOptionPane.WARNING_MESSAGE);
                    poll.setEnabled(false);
                } else if (serverMessage.equals("DISABLE_POLL")) {
                    poll.setEnabled(false);
                } else if (serverMessage.equals("ENABLE_SUBMIT")) {
                    submit.setEnabled(true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, "Connection to server lost.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void updateQuestion(String questionText, String option1, String option2, String option3, String option4) {
        question.setText(questionText);
        options[0].setText(option1);
        options[1].setText(option2);
        options[2].setText(option3);
        options[3].setText(option4);
        window.repaint();
    }

    private void resetTimer(int duration) {
        if (clock != null) {
            clock.cancel();
        }
        clock = new TimerCode(duration);
        Timer t = new Timer();
        t.schedule(clock, 0, 1000);
        pollAllowed = true;
    }

    private void resetForNewQuestion() {
        enableAllOptions();
        poll.setEnabled(true);
        submit.setEnabled(false);
        hasPolled = false;
        selectedAnswer = -1;
        optionGroup.clearSelection();
    }

    private void enableAllOptions() {
        for (JRadioButton option : options) {
            option.setEnabled(true);
        }
    }

    private void enablePolling() {
        pollAllowed = true;
        poll.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("You clicked " + e.getActionCommand());

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
                    sendUDP("POLL");
                    hasPolled = true;
                    disableAll();
                    poll.setEnabled(false);
                    // Server should enable submit after the poll phase
                    // submit.setEnabled(true);
                }
                break;
            case "Submit":
                if (hasPolled && selectedAnswer != -1) {
                    sendUDP("ANSWER:" + selectedAnswer);
                    disableAll();
                    submit.setEnabled(false);
                }
                break;
            default:
                System.out.println("Incorrect Option");
        }
    }

    private void sendUDP(String msg) {
        try {
            String messageWithID = clientID + ":" + msg; // Include client ID
            byte[] buffer = messageWithID.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disableAll() {
        for (JRadioButton option : options) {
            option.setEnabled(false);
        }
    }

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

    public static void main(String[] args) {
        String serverIP = JOptionPane.showInputDialog("Enter Server IP Address:");
        if (serverIP != null && !serverIP.isEmpty()) {
            new ClientWindow(serverIP);
        } else {
            JOptionPane.showMessageDialog(null, "Server IP address cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
}