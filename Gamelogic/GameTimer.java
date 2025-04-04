package Gamelogic;

public class GameTimer {
    private static double curTime = 0;

    private static void startTimer(int timerTotal) {
        curTime = timerTotal;
        double tempPrev =0, temp;
        temp = System.currentTimeMillis()/1000;
        while(curTime > 0) {
            if(temp-tempPrev > 1) {curTime--; tempPrev = temp;}
            temp = System.currentTimeMillis();
        }
    }

    private static boolean isActive() {
        if(curTime > 0) {
            return true;
        } else {
            return false;
        }
    }
    private static double timeRemaining() {
        return curTime; 
    }
}