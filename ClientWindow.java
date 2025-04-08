
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
    private JLabel timerLabel; // Renamed to avoid conflict
    private JLabel score;
    private TimerTask clock;
    private int scoreNum;

    private JFrame window;
    private DatagramSocket udpSocket; // Separate UDP socket for polling
    private InetAddress serverAddress;
    private int udpServerPort = 1983; // UDP port for polling
    private Socket tcpSocket; // TCP socket for questions, answers, etc.
    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private boolean pollAllowed = true;
    private boolean hasPolled = false;
    private int selectedAnswer = -1;
    private Timer gameTimer;

    private static SecureRandom random = new SecureRandom();

    public ClientWindow(String serverIp) {
        try {
            serverAddress = InetAddress.getByName(serverIp);
            udpSocket = new DatagramSocket();
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
            options[index].setEnabled(false);
        }

        timerLabel = new JLabel("TIMER");
        timerLabel.setBounds(250, 250, 100, 20);
        window.add(timerLabel);

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
        submit.setEnabled(false);
        window.add(submit);

        window.setSize(400, 400);
        window.setBounds(50, 50, 400, 400);
        window.setLayout(null);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        gameTimer = new Timer();
        new Thread(this::receiveServerMessages).start();
    }

    private void receiveServerMessages() {
        try {
            String serverMessage;
            while ((serverMessage = tcpIn.readLine()) != null) {
                System.out.println("Received from server (TCP): " + serverMessage);
                if (serverMessage.startsWith("QUESTION:")) {
                    String questionData = serverMessage.substring("QUESTION:".length());
                    String[] parts = questionData.split("\n"); // Split by newline
                    if (parts.length == 6) { // Expecting question + 4 options + correct index
                        String questionText = parts[0];
                        String option1 = parts[1];
                        String option2 = parts[2];
                        String option3 = parts[3];
                        String option4 = parts[4];
                        // We don't need to display the correct answer index on the client
                        updateQuestion(questionText, option1, option2, option3, option4);
                        resetForNewQuestion();
                    } else {
                        System.err.println("Invalid QUESTION format: " + serverMessage);
                    }
                } else if (serverMessage.equals("ack")) {
                    submit.setEnabled(true);
                } else if (serverMessage.equals("-ack")) {
                    JOptionPane.showMessageDialog(window, "Too late to poll for this question.", "Too Late", JOptionPane.WARNING_MESSAGE);
                    poll.setEnabled(false);
                } else if (serverMessage.equals("correct")) {
                    JOptionPane.showMessageDialog(window, "Correct Answer!", "Result", JOptionPane.INFORMATION_MESSAGE);
                    // Optionally update score immediately
                } else if (serverMessage.equals("wrong")) {
                    JOptionPane.showMessageDialog(window, "Incorrect Answer.", "Result", JOptionPane.WARNING_MESSAGE);
                    // Optionally update score immediately
                } else if (serverMessage.equals("next")) {
                    enablePolling();
                    submit.setEnabled(false);
                    hasPolled = false;
                    selectedAnswer = -1;
                    optionGroup.clearSelection();
                } else if (serverMessage.equals("kill")) {
                    JOptionPane.showMessageDialog(window, "Game Over!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                    disableAll();
                    poll.setEnabled(false);
                    submit.setEnabled(false);
                    if (gameTimer != null) {
                        gameTimer.cancel();
                        gameTimer.purge();
                    }
                    System.exit(0);
                } else if (serverMessage.startsWith("TIMER:")) {
                    try {
                        int duration = Integer.parseInt(serverMessage.substring("TIMER:".length()));
                        startOrUpdateTimer(duration);
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
                } else {
                    System.out.println("Received unknown message: " + serverMessage);
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
        updateOptions(option1, option2, option3, option4);
    }

    private void updateOptions(String option1, String option2, String option3, String option4) {
        options[0].setText(option1);
        options[1].setText(option2);
        options[2].setText(option3);
        options[3].setText(option4);
        enableAllOptions();
        window.repaint();
    }

    private void startOrUpdateTimer(int duration) {
        pollAllowed = true;
        if (clock != null) {
            clock.cancel();
        }
        clock = new TimerCode(duration, timerLabel);
        gameTimer.schedule(clock, 0, 1000);
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
                    // Wait for 'ack' from server over TCP to enable submit
                }
                break;
            case "Submit":
                if (hasPolled && selectedAnswer != -1) {
                    tcpOut.println("ANSWER:" + selectedAnswer); // Send answer over TCP
                    disableAll();
                    submit.setEnabled(false);
                    hasPolled = false; // Reset polled state after submitting
                }
                break;
            default:
                System.out.println("Incorrect Option");
        }
    }

    private void sendUDP(String msg) {
        try {
            byte[] buffer = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, udpServerPort);
            udpSocket.send(packet);
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
        private JLabel timerLabel;

        public TimerCode(int duration, JLabel timerLabel) {
            this.duration = duration;
            this.timerLabel = timerLabel;
        }

        @Override
        public void run() {
            if (duration < 0) {
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setText("Timer expired");
                    window.repaint();
                    pollAllowed = false;
                    submit.setEnabled(false);
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