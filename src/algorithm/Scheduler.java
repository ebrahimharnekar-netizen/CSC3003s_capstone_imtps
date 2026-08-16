package algorithm;
import java.util.List;    // we will be creating lists of items
import java.util.ArrayList;   // cant predict number of tasks user will input so need flexible arrays.
import java.util.Comparator;  // this allows us to compare task objects in our own way.


// the equation we will be using for this scheduling algorithm: priority = focus_multiplier x duration_mult x ((impW x importance) + (urgW x 1/time))


public class Scheduler implements SchedulingAlg{
    
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

        findTimeSlots(floatingTasks, pinnedTasks);  // running the sorted task through the gap finder to give them starting times

        return floatingTasks;  // returning the sorted list of tasks to be scheduled.

    }

    private double calcPriority(Task task, Questionnaire userPrefs) {

        double mFocus = 1.0;  // the focus mode multiplier
        double mDuration = 1.0;  // the duration multiplier
        double urgency;
        double baseScore;

        if (task.getCategoryID() == userPrefs.getCurrentMode()) {   // check if the current tasks category matches the users toggled focus mode

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

    private void findTimeSlots(List<Task> floatingTasks, List<Task> pinnedTasks) {

        // the work day window is 7am to 10pm
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime dayStart = today.atTime(7, 0);
        java.time.LocalDateTime dayEnd = today.atTime(22, 0);

        pinnedTasks.sort(new Comparator<Task>() {

            @Override
            public int compare(Task t1, Task t2) {

                return t1.startTime.compareTo(t2.startTime);

            }

        });

        for (Task task : floatingTasks) {  // schedule each floating task one by one.

            long taskMinutes = (long) (task.duration * 60);  // converting the duration of the task to minutes

            java.time.LocalDateTime searchTime = dayStart;  // the search starts at 7am
            boolean isScheduled = false;

            while (!searchTime.plusMinutes(taskMinutes).isAfter(dayEnd)) {   // continue searching as long as the current search time is not after the end of the working day

                java.time.LocalDateTime potEnd = searchTime.plusMinutes(taskMinutes);
                boolean collision = false;
                java.time.LocalDateTime nextFreeTime = null;

                for (Task pinned : pinnedTasks) {    // checing the potential time slot to every other pinned task on the calendar.

                    long pinnedMins = (long) (pinned.duration * 60);
                    java.time.LocalDateTime pinnedEnd = pinned.startTime.plusMinutes(pinnedMins);

                    if (searchTime.isBefore(pinnedEnd) && potEnd.isAfter(pinned.startTime)) {    // does the time slot collide with the current pinned task?

                        collision = true;
                        nextFreeTime = pinnedEnd;  // gap is too small therefore search again after pinned task ends.
                        break;

                    }

                }

                if (!collision) {    // a gap was found successfully

                    task.startTime = searchTime;
                    pinnedTasks.add(task);  // becomes a pinned "obstacle" for upcoming floating task

                    pinnedTasks.sort(new Comparator<Task>() {
                        
                        @Override
                        public int compare(Task t1, Task t2) {

                            return t1.startTime.compareTo(t2.startTime);    // sort the obstacles again.

                        }

                    });

                    isScheduled = true;
                    break;

                } else {

                    searchTime = nextFreeTime;  // go to end of obstacle and try the search again

                }

            }

            if (!isScheduled) {

                System.out.println(task.taskName + "could not fit in todays window.");

            }

        }

    }

}
