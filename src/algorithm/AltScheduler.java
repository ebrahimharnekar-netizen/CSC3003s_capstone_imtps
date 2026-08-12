package algorithm;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class AltScheduler {
    
    // this algorithm will prioritize the earliest deadline tasks first
    // this means that it will completely ignore the users preferences

    public List<Task> genSchedule(List<Task> allTasks, Questionnaire userPrefs) {

        List<Task> pinnedTasks = new ArrayList<>(); 
        List<Task> floatingTasks = new ArrayList<>();

        for (Task task : allTasks) {

            if (task.isPinned()) {

                pinnedTasks.add(task);

            } else {

                floatingTasks.add(task);

            }

        }

        floatingTasks.sort(new Comparator<Task>() {
            
            @Override
            public int compare(Task t1, Task t2) {

                return Double.compare(t1.timeUntilDue, t2.timeUntilDue);   // sorted in ascending order according to timeUntilDue.

            }

        });

        return floatingTasks;

    }

}
