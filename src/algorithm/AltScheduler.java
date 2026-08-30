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
        
        List<Task> additionalTasks = new ArrayList<>();

        for (Task task : floatingTasks) {  // schedule each floating task one by one.

            // START of date-range logic 
            LocalDate today = LocalDate.now();
            LocalDate searchStartDate = today;
            LocalDate searchEndDate = today;

            // Use original fields for the search window (if available)
            LocalDate taskStart = (task.originalStartDate != null) ? task.originalStartDate : task.startDate;
            LocalDate taskDue = (task.originalDueDate != null) ? task.originalDueDate.toLocalDate() : 
                                (task.dueDate != null ? task.dueDate.toLocalDate() : null);

            if (taskStart != null && taskStart.isAfter(today)) {
                searchStartDate = taskStart;
            }

            if (taskDue != null) {
                if (taskDue.isAfter(today)) {
                    searchEndDate = taskDue;
                } else {
                    searchEndDate = today;
                }
                if (taskStart != null && taskStart.isAfter(searchEndDate)) {
                    searchStartDate = today;
                    if (taskDue.isAfter(today)) {
                        searchEndDate = taskDue;
                    } else {
                        searchEndDate = today;
                    }
                }
            } else {
                searchEndDate = today;
            }
            //END of date-range logic

            long taskMinutes = (long) (task.duration * 60);  // converting the duration of the task to minutes
            boolean isFirstDayScheduled = true;
            boolean scheduledAtLeastOnce = false;

            //START of multi‑day loop (Removed early exit to schedule every day in range)
            for (LocalDate currentDate = searchStartDate; 
                 !currentDate.isAfter(searchEndDate); 
                 currentDate = currentDate.plusDays(1)) {

                LocalDateTime dayStart = currentDate.atTime(startHour);
                LocalDateTime dayEnd = currentDate.atTime(endHour);

                LocalDateTime searchTime = dayStart;  // the search starts at 7am on this day
                boolean scheduledToday = false;

                while (!searchTime.plusMinutes(taskMinutes).isAfter(dayEnd)) {   // continue searching as long as the current search time is not after the end of the working day

                    LocalDateTime potEnd = searchTime.plusMinutes(taskMinutes);
                    boolean collision = false;
                    LocalDateTime nextFreeTime = null;

                    for (Task pinned : pinnedTasks) {    // checking the potential time slot to every other pinned task on the calendar.

                        // Determine date range for this pinned task using its CURRENT fields
                        LocalDate pinnedStartDate;
                        LocalDate pinnedEndDate;

                        if (pinned.startDate != null && pinned.dueDate != null) {
                            pinnedStartDate = pinned.startDate;
                            pinnedEndDate = pinned.dueDate.toLocalDate();
                        } else if (pinned.isPinned() && pinned.dueDate != null) {
                            pinnedStartDate = pinned.startTime.toLocalDate();
                            pinnedEndDate = pinned.dueDate.toLocalDate();
                        } else {
                            pinnedStartDate = pinned.startTime.toLocalDate();
                            pinnedEndDate = pinnedStartDate;
                        }

                        if (currentDate.isBefore(pinnedStartDate) || currentDate.isAfter(pinnedEndDate)) {
                            continue;
                        }

                        LocalDateTime pinnedStartOfDay = currentDate.atTime(pinned.startTime.toLocalTime());
                        long pinnedMins = (long) (pinned.duration * 60);
                        LocalDateTime pinnedEnd = pinnedStartOfDay.plusMinutes(pinnedMins);

                        if (searchTime.isBefore(pinnedEnd) && potEnd.isAfter(pinnedStartOfDay)) {    // does the time slot collide with the current pinned task?

                            collision = true;
                            nextFreeTime = pinnedEnd;  // gap is too small therefore search again after pinned task ends.
                            break;

                        }

                    }

                    if (!collision) {    // a gap was found successfully
                    
                        if (isFirstDayScheduled) {
                            task.startTime = searchTime;
                            // Make scheduled floating tasks single‑day blockers
                            task.startDate = task.startTime.toLocalDate();
                            task.dueDate = null;
                            pinnedTasks.add(task);  // becomes a pinned "obstacle" for upcoming floating task
                            isFirstDayScheduled = false;
                        } else {
                            // Create a new schedule block for the following days
                            Task clone = new Task(task.taskId, task.taskName, task.category, task.importance, 
                                                  task.duration, null, searchTime, searchTime.toLocalDate());
                            clone.originalStartDate = task.originalStartDate;
                            clone.originalDueDate = task.originalDueDate;
                            clone.originalStartTime = task.originalStartTime;
                            pinnedTasks.add(clone);
                            additionalTasks.add(clone);
                        }

                        pinnedTasks.sort(new Comparator<Task>() {
                            
                            @Override
                            public int compare(Task t1, Task t2) {

                                return t1.startTime.compareTo(t2.startTime);    // sort the obstacles again.

                            }

                        });

                        scheduledToday = true;
                        scheduledAtLeastOnce = true;
                        break; // Move on to the next date

                    } else {

                        searchTime = nextFreeTime;  // go to end of obstacle and try the search again

                    }

                }
                
                if (!scheduledToday) {
                    System.out.println(task.taskName + " could not fit in the window on " + currentDate);
                }
                
            } //END of multi‑day loop

            if (!scheduledAtLeastOnce) {

                System.out.println(task.taskName + " could not fit anywhere in its date range.");

            }

        }
        
        floatingTasks.addAll(additionalTasks);

    }

}