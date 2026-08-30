package algorithm;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

            //  START of date-range logic
            LocalDate today = LocalDate.now();
            LocalDate searchStartDate = today;
            LocalDate searchEndDate = today;

            // Use original fields for the search window (if available) so we know where to look
            LocalDate taskStart = (task.originalStartDate != null) ? task.originalStartDate : task.startDate;
            LocalDate taskDue = (task.originalDueDate != null) ? task.originalDueDate.toLocalDate() : 
                                (task.dueDate != null ? task.dueDate.toLocalDate() : null);

            // If the task has a start date, do not schedule before that date.
            if (taskStart != null && taskStart.isAfter(today)) {
                searchStartDate = taskStart;
            }

            // If the task has a due date, we can schedule up to that date.
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
                // No due date: only schedule on today.
                searchEndDate = today;
            }
            // END of date-range logic

            long taskMinutes = (long) (task.duration * 60);  // converting the duration of the task to minutes
            boolean isFirstDayScheduled = true;
            boolean scheduledAtLeastOnce = false;

            // START of multi‑day loop (Removed early exit to schedule every day in range)
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

                        // Determine the date range for this pinned task using its CURRENT fields
                        // (NOT original fields, because scheduled floating tasks have been turned into single‑day blockers)
                        LocalDate pinnedStartDate;
                        LocalDate pinnedEndDate;

                        if (pinned.startDate != null && pinned.dueDate != null) {
                            // Task has both startDate and dueDate (user‑defined pinned task or scheduled with range)
                            pinnedStartDate = pinned.startDate;
                            pinnedEndDate = pinned.dueDate.toLocalDate();
                        } else if (pinned.isPinned() && pinned.dueDate != null) {
                            // Pinned task with startTime and dueDate but no startDate (start is inferred from startTime)
                            pinnedStartDate = pinned.startTime.toLocalDate();
                            pinnedEndDate = pinned.dueDate.toLocalDate();
                        } else {
                            // Single‑day task (e.g., a floating task that was scheduled and now blocks only that day)
                            pinnedStartDate = pinned.startTime.toLocalDate();
                            pinnedEndDate = pinnedStartDate;
                        }

                        // Only consider this pinned task if the current date falls within its range
                        if (currentDate.isBefore(pinnedStartDate) || currentDate.isAfter(pinnedEndDate)) {
                            continue;
                        }

                        // Build the start and end times for the pinned task on this day
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
                            // After scheduling, set the task to be a single‑day blocker (so it doesn't block future days)
                            task.startDate = task.startTime.toLocalDate();
                            task.dueDate = null;  // clear due date so it doesn't extend beyond today
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
                
            } // END of multi‑day loop

            if (!scheduledAtLeastOnce) {

                System.out.println(task.taskName + " could not fit anywhere in its date range.");

            }

        }
        
        floatingTasks.addAll(additionalTasks);

    }

}