package Gamelogic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**Class to handle the server-side game logic of the quiz. An instance of Game Manager is broadly a game instance, able to handle score, active player list, and which players are still connected to the game.*/

/*Necessary functionality
 *  - Can pick a random question and it's answers from a read in text/config file
 *  - Can package a "Question" element to be sent to the server class and blasted out to clients
 *  - Tracks scores of "Players" (Potentially need a player object to track who's who)
 *  - Handles the logic of a "Round" (a period of time in which the players can buzz in to answer the question, 
 *  and the steadily reducing point total for how long it takes players to answer)
 */
public class GameManager {
    HashMap<String, Player> players; //A list of all players who have been added to the game at any point during runtime
    ArrayList<String> playerIDs; 

    String pointPlayerID;
    int activeQuestion;
    int timerTotal, curTime;
    Boolean answeredCorrect;
    /**
     * Initiates an active game with a list of all connected players
     * @param players A list of objects (player1, player2, etc.) actively in the current managed game
     */
    public GameManager(Player... playerList) {
        this.players = new HashMap<String, Player>();
        this.playerIDs = new ArrayList<String>();
        for(Player p : playerList) {this.players.put(p.getID(), p); playerIDs.add(p.getID());}
        activeQuestion = 0;
        QuestionReader.refreshQuestionList();
    }
    public GameManager() {
        this.players = new HashMap<String, Player>();
        this.playerIDs = new ArrayList<String>();
        activeQuestion = 0;
        QuestionReader.refreshQuestionList();
    }

    public Question startRound() {
        for(String id : playerIDs) { //Checking to make sure players have responded recently, setting them to inactive if they haven't
            if(activeQuestion - players.get(id).getLastQAns() > 3) {
                players.get(id).setInactive();
            }
        }
        return QuestionReader.getQuestion(activeQuestion);
    }

    public void endRound() {
        if(pointPlayerID != null) { //Make sure someone buzzed to answer the question
            if(answeredCorrect != null) { //If they did, make sure that after they buzzed they actually answered the question
                if(answeredCorrect) { //If they did, see if they got the answer correct
                    players.get(pointPlayerID).scoreInc(10);
                } else {players.get(pointPlayerID).scoreInc(-10);}
            } else {players.get(pointPlayerID).scoreInc(-20);}
        }
        pointPlayerID = null;
        answeredCorrect = null; 
        activeQuestion++;
    }
    public String endGame() {
        String returnString = "";
        List<Player> playerList = new ArrayList<Player>(players.values());
        playerList.sort(Comparator.comparingInt(p -> p.getScore()));
        playerList = playerList.reversed();
        for(int i = 0; i<playerList.size(); i++) {
            returnString += "" + (i+1) + " place: " + playerList.get(i).getID() + "\n";
        }
        return returnString;
    }

    public void addPlayer(Player... newPlayer) {for(Player p : newPlayer) {this.players.put(p.getID(), p);}}

    /**
     * @return List of all players who have connected to the game
     */
    public ArrayList<Player> getPlayers() {return new ArrayList<Player>(players.values());}
    public int getTimer() {return curTime;}
    /**
     * @return The {@code int} index of the current question number
     */
    public int getCurQuestion() {return this.activeQuestion;}
    /**
     * 
     * @return Returns the {@code String} ID of the player currently buzzed in to answer the active question
     */
    public String getAnsweringID() {return pointPlayerID;}
    /**
     * 
     * @param ID A {@code String} representing the ID of an active player
     * @return A {@code Boolean} representing the activity of the player. True=Active, False=Inactive, Null=Dead
     */
    public Boolean clientActivityPassThrough(String ID) {return players.get(ID).isActive();}
    /**
     * 
     * @param ID A {@code String} representing the ID of the player you wish to mark as killed
     */
    public void killPlayer(String ID) {players.get(ID).kill();}
    /**
     * 
     * @param ID A {@code String} representing the ID of the player object you wish to retrieve
     * @return A {@code Player} object representing all the important stats of a player
     */
    public Player getPlayer(String ID) {return players.get(ID);}

    public void killAllTheDead() {
        for(String id : playerIDs) { //Checking to make sure players have responded recently, setting their connected variables to null if not
            if(!players.get(id).isActive()) {
                players.get(id).kill(); 
            }
        }
    }

    public void buzzIn(String buzzingPlayerID) {System.out.println("DEBUG: Buzzing in "); pointPlayerID = buzzingPlayerID;} //Convert playerlist to hashmap that uess ID as key, this takes key in

    /**@return True if inputted answer num is correct */
    public boolean answer(int answerNum) {
        if(QuestionReader.getQuestion(activeQuestion).getAnsNum() == answerNum) {answeredCorrect = true;} else {answeredCorrect = false;} return answeredCorrect;
    } 
}
