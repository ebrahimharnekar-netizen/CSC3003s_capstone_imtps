package algorithm;
import java.util.List;    // we will be creating lists of items
import java.util.ArrayList;   // cant predict number of tasks user will input so need flexible arrays.
import java.util.Comparator;  // this allows us to compare task objects in our own way.


// the equation we will be using for this scheduling algorithm: priority = focus_multiplier x duration_mult x ((impW x importance) + (urgW x 1/time))


public class Scheduler {
    
    // this is where the scheduler math will be created.

    // declaring weight variables
    private final double impW = 1.0;  // set to 1 by default but changes depending on users set importance (1-5)
    private final double urgW = 10.0;  // this scales the time fraction (1/time) up so that it can compare against importance

    public List<Task> genSchedule(List<Task> allTasks, Questionnaire userPrefs) {

        List<Task> pinnedTasks = new ArrayList<>();  // array of all tasks ,manually added by user to calendar
        List<Task> floatingTasks = new ArrayList<>();  // array of tasks added to be scheduled for user.

        for (Task task : allTasks) {  // loop through all tasks

            if (task.isPinned()) {

                pinnedTasks.add(task);  // becomes pinned task that floating tasks work around.

            } else {

                floatingTasks.add(task);  // tasks that get scheduled by the algorithm.

            }

        }

        floatingTasks.sort(new Comparator<Task>() {
            
            @Override  // makes sure that the required method "compare" is created
            public int compare(Task t1, Task t2) {

                double score1 = calcPriority(t1, userPrefs);  // calculate the priority score of first task
                double score2 = calcPriority(t2, userPrefs);  // calculate the priority score the other task

                return Double.compare(score2, score1);  // sort the t5asks in descending order

            }

        });

        return floatingTasks;  // returning the sorted list of tasks to be scheduled.

    }

    private double calcPriority(Task task, Questionnaire userPrefs) {

        double mFocus = 1.0;  // the focus mode multiplier
        double mDuration = 1.0;  // the duration multiplier
        double urgency;
        double baseScore;

        if (task.category.equalsIgnoreCase(userPrefs.getCurrentMode())) {   // check if the current tasks category matches the users toggled focus mode

            mFocus = 1.5;

        }

        if (userPrefs.isPrefQuickFirst() && task.duration <= 1.0) {   // if the user prefers doing quick tasks first, then the algorithm priioritizes the short tasks first(but also less than 1 hour long tasks)

            mDuration = 1.5;

        }

        // the equation logic:
        if (task.timeUntilDue > 0) {

            urgency = 1.0 / task.timeUntilDue;  // (1/time) part of equation

        } else {

            urgency = 0.0;


        }

        baseScore = (impW * task.importance) + (urgW * urgency);  // finalising the equation calculation

        return mFocus * mDuration * baseScore;

    }

}
