package algorithm;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;

// this is the knapsack scheduler, which finds the optimal subset of tasks for 'today' and pushes whatever doesnt fit
// in todays schedule to the back of the line of tomorrows schedule

// It looks at the same list and realizes that if it skips Task A, it can 
// fit both Task B and Task C into the 10 hour window (8 hours total)

public class KnapsackScheduler implements SchedulingAlg{
    
    public List<Task> genSchedule(List<Task> allTasks, Questionnaire userPrefs) {

        List<Task> pinnedTasks = new ArrayList<>();  // array of all tasks ,manually added by user to calendar
        List<Task> floatingTasks = new ArrayList<>();  // array of tasks added to be scheduled for user.

        for (Task task : allTasks) {

            if (task.isPinned()) {

                pinnedTasks.add(task);     // if current task is pinned, add it to the pinned tasks list.

            } else {

                floatingTasks.add(task);  // if it is not pinned, it is assumed to be floating and is added to the floating tasks list.

            }

        }

        // Calculate the exact minutes between the users custom start and end time
        long capacityMins = Duration.between(userPrefs.getDayStartTime(), userPrefs.getDayEndTime()).toMinutes();
        int totalCapMins = (int) capacityMins;

        if (totalCapMins < 0) {

            totalCapMins += 24 * 60;  // handling overnight shifts in case the start time is after the end time

        }

        for (Task pinned : pinnedTasks) {

            totalCapMins -= (int) (pinned.duration * 60);   // subtract pinned tasks from todays capacity

        }

        if (totalCapMins < 0) {

            totalCapMins = 0;      // preventing out of bounds exception for tasks that exceed working hours.

        }

        int n = floatingTasks.size();

        if (n > 0) {

            int[] weights = new int[n];
            int[] values = new int[n];
            
            for (int i = 0; i < n; i++) {

                weights[i] = (int) (floatingTasks.get(i).duration * 60); // weight is duration in mins
                values[i] = (int) floatingTasks.get(i).importance;       // value is the importance score
            
            }

            // Build the DP Table
            int[][] dp = new int[n + 1][totalCapMins + 1];   // dp[i][w], stores max importance with first i tasks using w minutes.
            
            for (int i = 1; i <= n; i++) {

                for (int w = 0; w <= totalCapMins; w++) {

                    if (weights[i - 1] <= w) {
                        // the task fits, therefore take the max between leving the task out and including the task.
                        dp[i][w] = Math.max(values[i - 1] + dp[i - 1][w - weights[i - 1]], dp[i - 1][w]);
                    
                    } else {
                        // the task is too large to fit in this window, therefore carry over the previous optimal value.
                        dp[i][w] = dp[i - 1][w];

                    }

                }

            }

            // Backtrack to find exactly which tasks got packed
            List<Task> optimalTasks = new ArrayList<>();
            List<Task> leftoverTasks = new ArrayList<>();
            
            int res = dp[n][totalCapMins];
            int w = totalCapMins;
            
            for (int i = n; i > 0; i--) {
                // If we still have 'res' to account for, AND the value changed from the row above it
                if (res > 0 && res != dp[i - 1][w]) {
                    // Task got packed
                    optimalTasks.add(floatingTasks.get(i - 1));
                    res = res - values[i - 1];
                    w = w - weights[i - 1];
                } else {
                    // Task didn't make the cut for the optimal pack
                    leftoverTasks.add(floatingTasks.get(i - 1));
                }
            }

            // Rebuild the floating list: Optimal tasks get scheduled first
            floatingTasks.clear();
            floatingTasks.addAll(optimalTasks);
            floatingTasks.addAll(leftoverTasks);
        
        }

        // Pass the userPrefs into findTimeSlots so it can read the dynamic start and end times
        findTimeSlots(floatingTasks, pinnedTasks, userPrefs);

        return floatingTasks;

    }

    private void findTimeSlots(List<Task> floatingTasks, List<Task> pinnedTasks, Questionnaire userPrefs) {

        // the work day window is now dynamically pulled from user preferences
        LocalTime startHour = userPrefs.getDayStartTime();
        LocalTime endHour = userPrefs.getDayEndTime();

        pinnedTasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task t1, Task t2) {
                return t1.startTime.compareTo(t2.startTime);
            }
        });
        
        List<Task> additionalTasks = new ArrayList<>();

        for (Task task : floatingTasks) { 
            // search strictly within today
            LocalDate today = LocalDate.now();
            LocalDate searchStartDate = today;
            LocalDate searchEndDate = today;

            // restricting the search window based on the users custom set window
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

            long taskMinutes = (long) (task.duration * 60);  
            boolean isFirstDayScheduled = true;
            boolean scheduledAtLeastOnce = false;

            for (LocalDate currentDate = searchStartDate; 
                 !currentDate.isAfter(searchEndDate); 
                 currentDate = currentDate.plusDays(1)) {

                LocalDateTime dayStart = currentDate.atTime(startHour);
                LocalDateTime dayEnd = currentDate.atTime(endHour);

                LocalDateTime searchTime = dayStart;    // sliding window starts at the beginning of the custom period
                boolean scheduledToday = false;

                while (!searchTime.plusMinutes(taskMinutes).isAfter(dayEnd)) {   

                    LocalDateTime potEnd = searchTime.plusMinutes(taskMinutes);
                    boolean collision = false;
                    LocalDateTime nextFreeTime = null;

                    for (Task pinned : pinnedTasks) {      // check the current slot against all tasks pinned to the calendar

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

                        // colllision is noted
                        if (searchTime.isBefore(pinnedEnd) && potEnd.isAfter(pinnedStartOfDay)) {    
                            
                            collision = true;
                            nextFreeTime = pinnedEnd;  // go to the end of the pinned task as it is obstructing the sliding window.
                            break;

                        }

                    }

                    if (!collision) {    
                    
                        if (isFirstDayScheduled) {

                            task.startTime = searchTime;
                            task.startDate = task.startTime.toLocalDate();
                            task.dueDate = null;
                            pinnedTasks.add(task);  
                            isFirstDayScheduled = false;

                        } else {

                            // blocking out time on different days (multi day scheduling)
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

                                return t1.startTime.compareTo(t2.startTime); 

                            }

                        });

                        scheduledToday = true;
                        scheduledAtLeastOnce = true;
                        break; 

                    } else {

                        searchTime = nextFreeTime; 

                    }

                }
                
                if (!scheduledToday) {

                    System.out.println(task.taskName + " could not fit in the window on " + currentDate);
                
                }
                
            } 

            if (!scheduledAtLeastOnce) {

                System.out.println(task.taskName + " could not fit anywhere in its date range.");
            
            }

        }
        
        floatingTasks.addAll(additionalTasks);

    }

}
