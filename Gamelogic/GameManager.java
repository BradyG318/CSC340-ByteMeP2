package Gamelogic;

import java.util.ArrayList;
import java.util.Arrays;

/**Class to handle the server-side game logic of the quiz. An instance of Game Manager is broadly a game instance, able to handle score, active player list, and which players are still connected to the game.*/

/*Necessary functionality
 *  - Can pick a random question and it's answers from a read in text/config file
 *  - Can package a "Question" element to be sent to the server class and blasted out to clients
 *  - Tracks scores of "Players" (Potentially need a player object to track who's who)
 *  - Handles the logic of a "Round" (a period of time in which the players can buzz in to answer the question, 
 *  and the steadily reducing point total for how long it takes players to answer)
 */
public class GameManager {
    ArrayList<Player> players; //A list of all players who have been added to the game at any point during runtime
    ArrayList<Player> activePlayers; //A list of all players who have responded since last heartbeat check (May not be needed)
    Question activeQuestion;
    /**
     * Initiates an active game with a list of all connected players
     * @param players A list of objects (player1, player2, etc.) actively in the current managed game
     */
    public GameManager(Player... players) {
        this.players = new ArrayList<Player>(Arrays.asList(players));
        QuestionReader.refreshQuestionList();
    }
    public void startRound() {
        
    }
    public void addPlayer(Player newPlayer) {players.add(newPlayer);}
    /**
     * @return List of all players who have connected to the game
     */
    public ArrayList<Player> getPlayers() {return players;}
}
