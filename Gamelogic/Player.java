package Gamelogic;

/**
 * Class to track player stats (current score, ID (IP:portNum), connectionStatus, etc. of an active player)
 */
public class Player {
    private String id;
    private int curScore;
    private boolean isConnected;
    
    public Player(String id, int curScore) {
        this.id = id;
        this.curScore = curScore; 
        isConnected = true;
    }
}
