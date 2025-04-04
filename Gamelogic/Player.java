package Gamelogic;

/**
 * Class to track player stats (current score, ID (IP:portNum), connectionStatus, etc. of an active player)
 */
public class Player {
    private String id;
    private int curScore;
    private boolean isConnected;
    private int lastComm;
    
    /**
     * 
     * @param id A {@code String} object formatted as IP:PortNum
     * @param curScore A {@code int} containing the current score of the player, most likely just set to 0
     * @param lastComm A {@code double} containing the timestamp of last communication
     */
    public Player(String id, int curScore, int lastComm) {
        this.id = id;
        this.curScore = curScore; 
        isConnected = true;
    }
    public void scoreInc(int scoreAdd) {
        curScore+=scoreAdd;
    }
    public void heartbeat(int questionNum) {lastComm = questionNum;}
    public String getID() {return id;}
}
