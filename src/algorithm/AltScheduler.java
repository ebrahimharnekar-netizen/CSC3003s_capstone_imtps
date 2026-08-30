package algorithm;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
        LocalTime startHour = LocalTime.of(7, 0);
        LocalTime endHour = LocalTime.of(22, 0);

        pinnedTasks.sort(new Comparator<Task>() {

            @Override
            public int compare(Task t1, Task t2) {

                return t1.startTime.compareTo(t2.startTime);

            }

        });

        for (Task task : floatingTasks) {  // schedule each floating task one by one.

            // START of added date-range logic 
            LocalDate today = LocalDate.now();
            LocalDate searchStartDate = today;
            LocalDate searchEndDate = today;

            // If the task has a start date, do not schedule before that date.
            if (task.startDate != null && task.startDate.isAfter(today)) {
                searchStartDate = task.startDate;
            }

            // If the task has a due date, do not schedule after that date.
            if (task.dueDate != null) {
                LocalDate dueDate = task.dueDate.toLocalDate();
                if (dueDate.isBefore(searchStartDate)) {
                    // If due date is before the start date, start searching today anyway.
                    searchStartDate = today;
                }
                if (dueDate.isBefore(searchEndDate) && dueDate.isAfter(searchStartDate)) {
                    searchEndDate = dueDate;
                }
            }
            //END of added date-range logic

            long taskMinutes = (long) (task.duration * 60);  // converting the duration of the task to minutes
            boolean isScheduled = false;

            //START of multi‑day loop
            for (LocalDate currentDate = searchStartDate; 
                 !currentDate.isAfter(searchEndDate) && !isScheduled; 
                 currentDate = currentDate.plusDays(1)) {

                LocalDateTime dayStart = currentDate.atTime(startHour);
                LocalDateTime dayEnd = currentDate.atTime(endHour);

                LocalDateTime searchTime = dayStart;  // the search starts at 7am on this day

                while (!searchTime.plusMinutes(taskMinutes).isAfter(dayEnd)) {   // continue searching as long as the current search time is not after the end of the working day

                    LocalDateTime potEnd = searchTime.plusMinutes(taskMinutes);
                    boolean collision = false;
                    LocalDateTime nextFreeTime = null;

                    for (Task pinned : pinnedTasks) {    // checking the potential time slot to every other pinned task on the calendar.

                        long pinnedMins = (long) (pinned.duration * 60);
                        LocalDateTime pinnedEnd = pinned.startTime.plusMinutes(pinnedMins);

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
            } //END of multi‑day loop

            if (!isScheduled) {

                System.out.println(task.taskName + "could not fit in todays window.");

            }

        }

    }

}