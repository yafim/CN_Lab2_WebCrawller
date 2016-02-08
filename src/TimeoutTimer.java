import java.util.Timer;
import java.util.TimerTask;

public class TimeoutTimer {
    Timer timer;
    public boolean timeOut = false;

    public TimeoutTimer(int i_Seconds) {
        timer = new Timer();
//        System.out.println("timer started");
        timer.schedule(new RemindTask(), i_Seconds * 1000);
	}

    class RemindTask extends TimerTask {
        public void run() {
        	timeOut = true;
//        	System.out.println("debug: time...");
            timer.cancel(); //Terminate the timer thread
        }
    }
}