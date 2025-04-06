
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
    private InetAddress serverAddress;
    private int serverPort = 5000;
    private boolean pollAllowed = true;
    private boolean hasPolled = false;
    private int selectedAnswer = -1;
	
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