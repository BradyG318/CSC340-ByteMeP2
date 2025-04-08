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
    static File questionFile = new File("Gamelogic/Questions.txt");
    static Scanner scan;
    static int lineCt, questionCt; 
    static ArrayList<Question> questionList;
    /**Refreshes the question list to account for any updates */
    public static void refreshQuestionList() {
        System.out.println("Ooga1");
        lineCt = 0; //Yes resetting this every time is laughably inefficient, but it adds the likely useless feature of being able to add questions mid quiz and that's good enough excuse for me
        try {
            System.out.println("Ooga1.5");
            scan = new Scanner(questionFile);
            System.out.println("Ooga1.75");
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Ooga1.8");
        while(scan.hasNextLine()) {
            lineCt++;
            scan.nextLine();
            System.out.println("Ooga" + lineCt);
        }
        System.out.println("Ooga2");

        questionCt = lineCt/6; //Each "question" will take 6 lines, 1 for q, 4 for a, 1 for rightnum answer
        questionList = new ArrayList<Question>(questionCt);
        scan.close();
        try {
            scan = new Scanner(questionFile);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Ooga3");
        while(scan.hasNextLine()) {
            String tempQ = "";
            String[] tempAs = new String[4];
            int tempRightA = 99;
            for(int i = 0; i<6; i++) { //Going from next line to answerNum
                switch(i) {
                    case(0):
                    tempQ = scan.nextLine();
                    System.out.println("DEBUG: Q=" + tempQ);
                    break;
                    case(5):
                    tempRightA = Integer.parseInt(scan.nextLine().trim());
                    break;
                    default:
                    String debug = scan.nextLine();
                    tempAs[i-1] = debug;
                    System.out.println("DEBUG: New A=" + debug);
                    break;
                }
            }
            questionList.add(new Question(tempQ, tempAs, tempRightA));
        }
        System.out.println("Ooga4");
    }
    /**
     * @param index {@code int} representing the index of the question to retrieve
     * @return A {@code Question} object that contains the question, answers, and correct answer index of the questions
    */
    public static Question getQuestion(int index) {
        Question returnQ = questionList.get(index);
        return returnQ;
    }
}

