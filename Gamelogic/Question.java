package Gamelogic;
/**
 * Question object containing multiple Strings and string arrays including
 */
public class Question {
    String question;
    String[] answers;
    int correctAns;
    /**
     * @param question {@code String} object of the question being asked
     * @param answers {@code String[]} list of potential answers to the question
     * @param correctAns {@code int} value representing the index of the correct answer 
     */
    public Question(String question, String[] answers, int correctAns) {
        this.question = question;
        this.answers = answers;
        this.correctAns = correctAns;
    }
    /**
     * @return A {@code String} to give question part of the Question
     */
    public String getQuestion() {return question;}
    /**
     * @return A {@code String[]} to return a list of the 4 potential answers to the question
     */
    public String[] getAnswers() {return answers;}
    /**
     * @return A {@code int} representing the index of the correct answer to the question
     */
    public int getAnsNum() {return correctAns;}
}
