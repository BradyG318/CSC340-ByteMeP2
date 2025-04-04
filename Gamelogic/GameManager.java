package Gamelogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

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
    ArrayList<Player> activePlayers; //A list of all players who have responded since last heartbeat check (May not be needed)
    String pointPlayerID;
    int activeQuestion;
    int timerTotal, curTime;
    Boolean answeredCorrect;
    /**
     * Initiates an active game with a list of all connected players
     * @param players A list of objects (player1, player2, etc.) actively in the current managed game
     */
    public GameManager(Player... players) {
        for(Player p : players) {this.players.put(p.getID(), p);}
        timerTotal = 15;
        activeQuestion = 0;
        QuestionReader.refreshQuestionList();
    }
    public GameManager() {
        timerTotal = 15;
        activeQuestion = 0;
        QuestionReader.refreshQuestionList();
    }

    public void startRound() {
        
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
    }

    public void addPlayer(Player... newPlayer) {for(Player p : newPlayer) {this.players.put(p.getID(), p);}}

    /**
     * @return List of all players who have connected to the game
     */
    public ArrayList<Player> getPlayers() {return new ArrayList<Player>(players.values());}
    public int getTimer() {return curTime;}


    public void buzzIn(String buzzingPlayerID) {pointPlayerID = buzzingPlayerID;} //Convert playerlist to hashmap that uess ID as key, this takes key in

    /**@return True if inputted answer num is correct */
    public boolean answer(int answerNum) {
        if(QuestionReader.getQuestion(activeQuestion).getAnsNum() == answerNum) {answeredCorrect = true;} else {answeredCorrect = false;} return answeredCorrect;
    }

}
