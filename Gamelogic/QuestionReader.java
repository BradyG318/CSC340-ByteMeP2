package Gamelogic;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * Reads in a text file, grabs a random question, and returns a packaged question object of that question
 * Only refresh question list if new questions are added, or you want to recycle already "used" questions
 */
public class QuestionReader {
    static File questionFile = new File("Question.txt");
    static Scanner scan;
    static int lineCt, questionCt; 
    static ArrayList<Question> questionList;
    public static void refreshQuestionList() {
        lineCt = 0; //Yes resetting this every time is laughably inefficient, but it adds the likely useless feature of being able to add questions mid quiz and that's good enough excuse for me
        try {
            scan = new Scanner(questionFile);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        while(scan.hasNextLine()) {
            lineCt++;
        }

        questionCt = lineCt/6; //Each "question" will take 6 lines, 1 for q, 4 for a, 1 for rightnum answer
        questionList = new ArrayList<Question>(questionCt);
        try {
            scan = new Scanner(questionFile);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        while(scan.hasNextLine()) {
            String tempQ = "";
            String[] tempAs = new String[4];
            int tempRightA = 99;
            for(int i = 0; i<6; i++) { //Going from next line to answerNum
                switch(i) {
                    case(0):
                    tempQ = scan.nextLine();
                    break;
                    case(5):
                    tempRightA = scan.nextInt();
                    scan.nextLine();
                    break;
                    default:
                    tempAs[i-1] = scan.nextLine();
                    break;
                }
            }
            questionList.add(new Question(tempQ, tempAs, tempRightA));
        }
    }
    public static Question getRandomQuestion() {
        Random random = new Random();
        int randomIndex = random.nextInt(0,questionList.size());
        Question returnQ = questionList.get(randomIndex);
        questionList.remove(randomIndex);
        return returnQ;
    }
}

