package Gamelogic;

/**
 * Class to track player stats (current score, ID (IP:portNum), connectionStatus, etc. of an active player)
 */
public class Player {
    private String id;
    private int curScore;
    private Boolean isConnected; //True connected, false inactive, null dead 
    private int lastQAns;
    
    /**
     * Constructor for a player object, storing all necessary data to maintain stats and connection status for a given user
     * @param id A {@code String} object formatted as IP:PortNum
     * @param curScore A {@code int} containing the current score of the player, most likely just set to 0
     * @param lastQAns A {@code int} containing the last answered question
     */
    public Player(String id, int curScore, int lastQAns) {
        this.id = id;
        this.curScore = curScore; 
        isConnected = true;
    }
    /**
     * Increments score based on inputted integer
     * @param scoreAdd A {@code int} value to increment score by
     */
    public void scoreInc(int scoreAdd) {
        curScore+=scoreAdd;
    }
    /**
     * @return An {@code int} value representing the score of the Player
     */
    public int getScore() {return curScore;}
    /**
     * heartbeat system to increment the last time the player interacted with the game
     * @param questionNum A {@code int} representing the last question index the Player interacted with
     */
    public void heartbeat(int questionNum) {lastQAns = questionNum;}
    /**
     * @return A {@code String} object representing the ID of the Player
     */
    public String getID() {return id;}
    /**
     * @return A {@code int} representing the index of the last question the Player has answered
     */
    public int getLastQAns() {return lastQAns;}
    /**
     * @return A {@code Boolean} representing connection status. true=active, false=inactive, null=killed
     */
    public Boolean isActive() {return isConnected;}
    /**
     * A function to set a player's activity to inactive
     */
    public void setInactive() {isConnected = false;}
    /**
     * A function to set a player's activity to active
     */
    public void setActive() {isConnected = true;}
    /**
     * A function to set a player's activity to dead
     */
    public void kill() {isConnected = null; }
}
