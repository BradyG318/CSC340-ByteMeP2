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
    public String getQuestion() {return question;}
    public String[] getAnswers() {return answers;}
    public int getAnsNum() {return correctAns;}
}
