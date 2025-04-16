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

    String pointPlayerID;
    int activeQuestion;
    int timerTotal, curTime;
    Boolean answeredCorrect;
    /**
     * Initiates an active game with a list of all connected players
     * @param players A list of Player objects (player1, player2, etc.) actively in the current managed game. Can enter as many as necessary
     */
    public GameManager(Player... playerList) {
        this.players = new HashMap<String, Player>();
        // for(Player p : playerList) {this.players.put(p.getID(), p); playerIDs.add(p.getID());}
        activeQuestion = 0;
        QuestionReader.refreshQuestionList();
    }
    /**
     * Initiates an active game with an empty list of players
     */
    public GameManager() {
        this.players = new HashMap<String, Player>();
        activeQuestion = 0;
        QuestionReader.refreshQuestionList();
    }
    /**
     * Starts a new round within the game by updating activity checks, and returning the next question in the game.
     * @return A {@code Question} object containing the question, answers, and answer num index from the connected Questions.txt file
     */
    public Question startRound() {
        for(String id : players.keySet()) { //Checking to make sure players have responded recently, setting them to inactive if they haven't
            if(activeQuestion - players.get(id).getLastQAns() > 3 && players.get(id).isActive() == true) {
                players.get(id).setInactive();
            }
        }
        return QuestionReader.getQuestion(activeQuestion);
    }
    /**
     * Ends the currently active round within game manager. 
     * Adjusts scores as needed, resets all round-scoped variables, and increments to the next question.
     */
    public void endRound() {
        System.out.println("Ending Round");
        if(pointPlayerID != null) { //Make sure someone buzzed to answer the question
            if(answeredCorrect != null) { //If they did, make sure that after they buzzed they actually answered the question
                if(answeredCorrect) { //If they did, see if they got the answer correct
                    players.get(pointPlayerID).scoreInc(10);
                    System.out.println("Adding 10pts");
                } else {players.get(pointPlayerID).scoreInc(-10); System.out.println("Removing 10pts");}
            } else {players.get(pointPlayerID).scoreInc(-20); System.out.println("Removing 20pts");}
        }
        pointPlayerID = null;
        answeredCorrect = null; 
        activeQuestion++;
    }
    /**Forces the points to be calculated early for the currently active playerID */
    public void forcePtCalc() {
        System.out.println("Forcing Pt Calculation");
        if(pointPlayerID != null) { //Make sure someone buzzed to answer the question
            if(answeredCorrect != null) { //If they did, make sure that after they buzzed they actually answered the question
                if(answeredCorrect) { //If they did, see if they got the answer correct
                    players.get(pointPlayerID).scoreInc(10);
                    System.out.println("Adding 10pts");
                } else {players.get(pointPlayerID).scoreInc(-10); System.out.println("Removing 10pts");}
            } else {players.get(pointPlayerID).scoreInc(-20); System.out.println("Removing 20pts");}
        }
        pointPlayerID = null;
        answeredCorrect = null; 
    }
    /**
     * Ends this instance of game and provides an organized list based on score
     * @return A {@code String} object containing a list of all players ordered by highest score labelled by their IDs
     */
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
    /**
     * Adds player(s) into the game. Can take in multiple fields or just one
     * @param newPlayer A {@code Player(s)} object or objects to be added into the current game.
     */
    public void addPlayer(Player... newPlayer) {for(Player p : newPlayer) {this.players.put(p.getID(), p);}}

    /**
     * A getter to retrieve an ArrayList of all players who have connected to an instance of gameManager
     * @return List of all players who have connected to the game
     */
    public ArrayList<Player> getPlayers() {return new ArrayList<Player>(players.values());}
    public int getTimer() {return curTime;}
    /**
     * A getter to request the index of the current question
     * @return The {@code int} index of the current question number
     */
    public int getCurQuestion() {return this.activeQuestion;}
    /**
     * Gives the ID of the player currently buzzed in to answer the question
     * @return Returns the {@code String} ID of the player currently buzzed in to answer the active question
     */
    public String getAnsweringID() {return pointPlayerID;}
    /**
     * Passes the activity request through game manager to the active player based on a submitted ID, then returns whether or not the player is still 'active'
     * @param ID A {@code String} representing the ID of an active player
     * @return A {@code Boolean} representing the activity of the player. True=Active, False=Inactive, Null=Dead
     */
    public Boolean clientActivityPassThrough(String ID) {return players.get(ID).isActive();}
    /**
     * 
     * @param ID A {@code String} representing the ID of the player you wish to mark as killed
     */
    public void killPlayer(String ID) {players.get(ID).setInactive();}
    /**
     * Function to retrieve the player from the game's hashmap
     * @param ID A {@code String} representing the ID of the player object you wish to retrieve
     * @return A {@code Player} object representing all the important stats of a player
     */
    public Player getPlayer(String ID) {return players.get(ID);}

    /**
     * Sets all "inactive" players to null, effectively killing the player as it causes the TCP connection to be closed and a new one to need to be opened
     */
    public void killAllTheDead() {
        // for(String id : playerIDs) { //Checking to make sure players have responded recently, setting their connected variables to null if not
        //     if(!players.get(id).isActive()) {
        //         players.get(id).kill(); 
        //     }
        // }
    }
    /**
     * Buzzs in the submitted player ID as the active answering player for the question
     * @param buzzingPlayerID A {@code String} representing the ID of the buzzing player
     */
    public void buzzIn(String buzzingPlayerID) {System.out.println("Buzzing in "); pointPlayerID = buzzingPlayerID;} //Convert playerlist to hashmap that uess ID as key, this takes key in

    /**@return True if inputted answer num is correct */
    public boolean answer(int answerNum) {
        if(QuestionReader.getQuestion(activeQuestion).getAnsNum() == answerNum) {answeredCorrect = true;} else {answeredCorrect = false;} return answeredCorrect;
    } 
}
