package algorithm;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class AltScheduler implements SchedulingAlg{
    
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

        findTimeSlots(floatingTasks, pinnedTasks);

        return floatingTasks;

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
