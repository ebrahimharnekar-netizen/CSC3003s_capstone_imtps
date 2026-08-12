package algorithm;
import java.util.List;    // we will be creating lists of items
import java.util.ArrayList;   // cant predict number of tasks user will input so need flexible arrays.
import java.util.Comparator;  // this allows us to compare task objects in our own way.


// the equation we will be using for this scheduling algorithm: priority = focus_multiplier x duration_mult x ((impW x urgW) + (urgW x 1/time))


public class Scheduler {
    
    // this is where the scheduler math will be created.

    // declaring weight variables
    private final double impW = 1.0;  // set to 1 by default but changes depending on users set importance (1-5)
    private final double urgW = 10.0;  // this scales the time fraction (1/time) up so that it can compare against importance

    public List<Task> genSchedule(List<Task> allTasks, Questionnaire userPrefs) {

        List<Task> pinnedTasks = new ArrayList<>();  // array of all tasks ,manually added by user to calendar
        List<Task> floatingTasks = new ArrayList<>();  // array of tasks added to be scheduled for user.
        
        return floatingTasks;

    }

}
