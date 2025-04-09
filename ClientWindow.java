import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
    private JLabel timerLabel;
    private JLabel score;
    private TimerTask pollTimerTask;
    private TimerTask answerTimerTask;
    private String scoreNum;

    private JFrame window;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort = 1983;
    private Socket tcpSocket;
    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private boolean pollAllowed = true;
    private boolean hasPolled = false;
    private int selectedAnswer = -1;
    private Timer gameTimer;
    private boolean canAnswer = false; // Flag to indicate if the answer timer is running
    private boolean acknowledged = false; // Flag to track if 'ack' has been received

    private String currentQuestionText = "";
    private String[] currentOptions = new String[4];
    private boolean receivingQuestion = false;
    private int expectedParts = 0;
    private String playerID;

    private static SecureRandom random = new SecureRandom();

    public ClientWindow(String serverIp) {
        try {
            serverAddress = InetAddress.getByName(serverIp);
            socket = new DatagramSocket();
            tcpSocket = new Socket(serverIp, 1987);
            tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, "Error connecting to the server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        JOptionPane.showMessageDialog(window, "Video Game Trivia");
        window = new JFrame("Video game Trivia");
        window.setSize(400, 400);
        window.setBounds(50, 50, 1000, 400);
        window.setLayout(null);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        window.getContentPane().setBackground(new Color(173, 216, 250));

        question = new JLabel("Waiting for question...");
        question.setBounds(10, 5, 750, 100);
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
            options[index].setBackground(new Color(173, 216, 250));
        }

        timerLabel = new JLabel("TIMER");
        timerLabel.setBounds(500, 250, 100, 20);
        window.add(timerLabel);

        scoreNum = "0";
        playerID = "Player 1"; // Assuming Player ID is initially "Player 1"
        score = new JLabel("(" + playerID + ") SCORE: " + scoreNum);
        score.setBounds(50, 250, 200, 20); // Increased width to accommodate Player ID
        window.add(score);

        poll = new JButton("Poll");
        poll.setBounds(10, 300, 100, 20);
        poll.addActionListener(this);
        window.add(poll);

        submit = new JButton("Submit");
        submit.setBounds(500, 300, 100, 20);
        submit.addActionListener(this);
        submit.setEnabled(false);
        window.add(submit);

        gameTimer = new Timer();
        new Thread(this::receiveServerMessages).start();
    }

    private void receiveServerMessages() {
        try {
            String serverMessage;
            while ((serverMessage = tcpIn.readLine()) != null) {
                System.out.println("Received from server (TCP): " + serverMessage);
                String[] parts = serverMessage.split(",");

                if (parts[0].equals("Byte-Me")) {
                    String dataType = parts[7]; // The actual content

                    // Three mutually exclusive else if statements
                    if (parts[7].startsWith("Player ")) {
                        playerID = parts[7];
                        SwingUtilities.invokeLater(() -> {
                            score.setText(playerID + " SCORE: " + scoreNum);
                            score.setBounds(50, 250, 200, 20);
                            window.repaint();
                        });
                    } else if (parts[7].startsWith("q")) {
                        currentQuestionText = parts[8];
                        receivingQuestion = true;
                        expectedParts = parts.length + 3; // Expecting 3 more parts for options
                        startPollTimer(); // Start the 15-second poll timer
                    } else if (parts[7].startsWith("answer")) {
                        if (parts.length >= 12) {
                            currentOptions[0] = parts[8];
                            currentOptions[1] = parts[9];
                            currentOptions[2] = parts[10];
                            currentOptions[3] = parts[11];
                            updateQuestion(currentQuestionText, currentOptions[0], currentOptions[1], currentOptions[2], currentOptions[3]);
                            resetForNewQuestion();
                            receivingQuestion = false;
                            currentQuestionText = "";
                            currentOptions = new String[4];
                            expectedParts = 0;
                        } else {
                            System.out.println("Received Byte-Me message with insufficient options: " + serverMessage);
                            receivingQuestion = false; // Reset on error
                            expectedParts = 0;
                            stopPollTimer();
                        }
                    } else if (receivingQuestion) {
                        System.out.println("Received intermediate Byte-Me data or incorrect number of parts: " + serverMessage);
                        if (parts.length >= 8) {
                            currentQuestionText = currentQuestionText + "," + dataType;
                        }
                    }

                    switch (dataType.trim()) { // Trim whitespace for robust comparison
                        case "score":
                        if (parts.length >= 9) {
                            try {
                                scoreNum = parts[8]; // Update the score string
                                SwingUtilities.invokeLater(() -> {
                                    score.setText(playerID + " SCORE: " + scoreNum);
                                });
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid score format received: " + parts[8]);
                            }
                        } else {
                            System.err.println("Score data missing in the packet.");
                        }
                        break;
                        case "ack":
                            System.out.println("Received ack!"); // Debugging log
                            stopPollTimer();
                            acknowledged = true;
                            poll.setEnabled(false); // Don't disable poll button after clicking
                            if (hasPolled) {
                                submit.setEnabled(true);
                                enableAllOptions(); // Enable options here, after receiving 'ack'
                                startAnswerTimer(); // Start the 10-second answer timer
                                canAnswer = true;
                                System.out.println("At line 191");
                            }
                            break;
                        case "-ack":
                            JOptionPane.showMessageDialog(window, "Too late to poll for this question.", "Too Late", JOptionPane.WARNING_MESSAGE);
                            poll.setEnabled(false);
                            pollAllowed = false;
                            break;
                        case "correct":
                            JOptionPane.showMessageDialog(window, "Correct Answer!", "Result", JOptionPane.INFORMATION_MESSAGE);
                            submit.setEnabled(false);
                            hasPolled=false;
                            selectedAnswer=-1;
                            optionGroup.clearSelection();
                            canAnswer = false;
                            acknowledged=false;
                            disableAllOptions();
                            break;
                        case "wrong":
                            JOptionPane.showMessageDialog(window, "Incorrect Answer.", "Result", JOptionPane.WARNING_MESSAGE);
                            submit.setEnabled(false);
                            hasPolled=false;
                            selectedAnswer=-1;
                            optionGroup.clearSelection();
                            canAnswer = false;
                            acknowledged = false;
                            disableAllOptions();
                            break;
                        case "next":
                            resetForNewQuestion();
                            enablePolling();
                            submit.setEnabled(false);
                            hasPolled = false;
                            selectedAnswer = -1;
                            optionGroup.clearSelection();
                            stopAnswerTimer();
                            canAnswer = false;
                            acknowledged = false; // Reset ack for the next question
                            disableAllOptions(); // Disable options until next poll and ack
                            break;
                        case "kill":
                            JOptionPane.showMessageDialog(window, "Game Over!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                            disableAll();
                            poll.setEnabled(false);
                            submit.setEnabled(false);
                            stopPollTimer();
                            stopAnswerTimer();
                            if (gameTimer != null) {
                                gameTimer.cancel();
                                gameTimer.purge();
                            }
                            System.exit(0);
                            break;
                        default: //shouldn't really get to here
                            if (serverMessage.startsWith("TIMER:")) {
                                try {
                                    int duration = Integer.parseInt(serverMessage.substring("TIMER:".length()));

                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid TIMER format: " + serverMessage);
                                }
                            } else if (serverMessage.startsWith("SCORE:")) {
                                try {
                                    //scoreNum = Integer.parseInt(serverMessage.substring("SCORE:".length()));
                                    SwingUtilities.invokeLater(() -> {
                                        score.setText("(" + playerID + ") SCORE: " + scoreNum);
                                    });
                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid SCORE format: " + serverMessage);
                                }
                            } else {
                                System.out.println("Received unknown message: " + serverMessage);
                            }
                            break;
                    }
                }
                System.out.println("At line 267");
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, "Connection to server lost.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        System.out.println("At line 272");;
        Thread.currentThread().notify();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("You clicked " + e.getActionCommand());
        String input = e.getActionCommand();

        if(input.equals(options[0].getText())){
            input = "Option 1";
        } else if(input.equals(options[1].getText())){
            input = "Option 2";
        } if(input.equals(options[2].getText())){
            input = "Option 3";
        } else if(input.equals(options[3].getText())){
            input = "Option 4";
        }

        switch (input) {
            case "Option 1":
                if (acknowledged) selectedAnswer = 0;
                break;
            case "Option 2":
                if (acknowledged) selectedAnswer = 1;
                break;
            case "Option 3":
                if (acknowledged) selectedAnswer = 2;
                break;
            case "Option 4":
                if (acknowledged) selectedAnswer = 3;
                break;
            case "Poll":
                if (pollAllowed && !hasPolled) {
                    sendUDP("POLL");
                    hasPolled = true;
                    disableAllOptions(); // Disable options after polling until ack
                    poll.setEnabled(true); // Don't disable poll button after clicking
                    // The poll timer will be stopped upon receiving 'ack'
                }
                break;
            case "Submit":
                System.out.println("poll" + hasPolled + " answer " + selectedAnswer + " canans " + canAnswer + " ack " + acknowledged);
                if (hasPolled && selectedAnswer != -1 && canAnswer && acknowledged) {
                    String[] ans = {"My Answer", selectedAnswer + ""}; 
                    try {
                        System.out.println("processing");
                        Protocol answerPacket = new Protocol(InetAddress.getLocalHost(), serverAddress, (Integer) tcpSocket.getPort(), (Integer) 1987, (double) System.currentTimeMillis(), ans);
                        System.out.println("pushing");
                        tcpOut.println(answerPacket.getData());
                        System.out.println("push work");
                        tcpOut.flush();
                    } catch (UnknownHostException ee) {
                        System.out.println("no host");
                    } 
                    disableAllOptions();
                    submit.setEnabled(false);
                    hasPolled = false;
                    canAnswer = false;
                    acknowledged = false; // Reset ack after submitting
                    selectedAnswer = -1; // Reset selected answer
                    optionGroup.clearSelection(); // Clear radio button selection
                } else {
                    Protocol answerPacket;
                    try {
                        answerPacket = new Protocol(InetAddress.getLocalHost(), serverAddress, (Integer) tcpSocket.getPort(), (Integer) 1987, (double) System.currentTimeMillis(), "No Answer");
                        System.out.println("pushing");
                        tcpOut.println(answerPacket.getData());
                    } catch (UnknownHostException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    
                }
                // Thread.currentThread().notifyAll();
                break;
            default:
                System.out.println("Incorrect Option");
        }
    }

    private void sendUDP(String msg) {
        try {
            Protocol send = new Protocol(serverAddress, InetAddress.getLocalHost(), serverPort, socket.getLocalPort(),(double) System.currentTimeMillis(), (msg + "%" + playerID));
            byte[] buffer = send.getData().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateQuestion(String questionText, String option1, String option2, String option3, String option4) {
        question.setText(questionText);
        updateOptions(option1, option2, option3, option4);
    }

    private void updateOptions(String option1, String option2, String option3, String option4) {
        options[0].setText(option1);
        options[1].setText(option2);
        options[2].setText(option3);
        options[3].setText(option4);
        // Options are enabled only after 'ack' is received
        window.repaint();
    }

    private void resetForNewQuestion() {
        enablePolling();
        submit.setEnabled(false);
        hasPolled = false;
        selectedAnswer = -1;
        optionGroup.clearSelection();
        pollAllowed = true;
        stopAnswerTimer();
        canAnswer = false;
        acknowledged = false;
        disableAllOptions(); // Disable options until next poll and ack
    }

    private void enableAllOptions() {
        if (acknowledged) {
            System.out.println("enableAllOptions() called. acknowledged is true. Enabling options."); // Debugging log
            for (JRadioButton option : options) {
                option.setEnabled(true);
            }
        } else {
            System.out.println("enableAllOptions() called. acknowledged is false. Options NOT enabled."); // Debugging log
        }
    }

    private void disableAllOptions() {
        for (JRadioButton option : options) {
            option.setEnabled(false);
        }
    }

    private void disableAll() {
        disableAllOptions();
        poll.setEnabled(false);
        submit.setEnabled(false);
    }

    private void enablePolling() {
        pollAllowed = true;
        poll.setEnabled(true);
    }

    private void startPollTimer() {
        pollAllowed = true;
        if (pollTimerTask != null) {
            pollTimerTask.cancel();
        }
        pollTimerTask = new TimerTask() {
            int duration = 15;
            @Override
            public void run() {
                if (duration < 0) {
                    SwingUtilities.invokeLater(() -> {
                        timerLabel.setText("Poll Timer Expired");
                        poll.setEnabled(false);
                        pollAllowed = false;
                        window.repaint();
                    });
                    this.cancel();
                    return;
                }
                Color textColor = (duration < 6) ? Color.red : Color.black;
                int currentDuration = duration;
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setForeground(textColor);
                    timerLabel.setText(String.valueOf(currentDuration));
                    window.repaint();
                });
                duration--;
            }
        };
        gameTimer.schedule(pollTimerTask, 0, 1000);
    }

    private void stopPollTimer() {
        if (pollTimerTask != null) {
            System.out.println("Stopping poll timer."); // Debugging log
            pollTimerTask.cancel();
            pollTimerTask = null;
        } else {
            System.out.println("stopPollTimer() called but pollTimerTask is null."); // Debugging log
        }
    }

    private void startAnswerTimer() {
        if (answerTimerTask != null) {
            answerTimerTask.cancel();
        }
        answerTimerTask = new TimerTask() {
            int duration = 10;
            @Override
            public void run() {
                if (duration < 0) {
                    SwingUtilities.invokeLater(() -> {
                        timerLabel.setText("Answer Timer Expired");
                        submit.setEnabled(false);
                        canAnswer = false;
                        window.repaint();
                    });
                    this.cancel();
                    return;
                }
                Color textColor = (duration < 6) ? Color.red : Color.black;
                int currentDuration = duration;
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setForeground(textColor);
                    timerLabel.setText(String.valueOf(currentDuration));
                    window.repaint();
                });
                duration--;
            }
        };
        gameTimer.schedule(answerTimerTask, 0, 1000);
    }

    private void stopAnswerTimer() {
        if (answerTimerTask != null) {
            answerTimerTask.cancel();
            answerTimerTask = null;
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