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

        findTimeSlots(floatingTasks, pinnedTasks, userPrefs);  // running the sorted task through the gap finder to give them starting times

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

    private void findTimeSlots(List<Task> floatingTasks, List<Task> pinnedTasks, Questionnaire userPrefs) {

        // the work day window is dynamically pulled from user preferences
        LocalTime startHour = userPrefs.getDayStartTime();
        LocalTime endHour = userPrefs.getDayEndTime();

        // Track work minutes per day
        java.util.Map<LocalDate, Integer> dailyWorkMinutes = new java.util.HashMap<>();
        int maxWorkMinsPerDay = userPrefs.getDailyProdHours() * 60;

        pinnedTasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task t1, Task t2) {
                return t1.startTime.compareTo(t2.startTime);
            }
        });
        
        List<Task> additionalTasks = new ArrayList<>();

        for (Task task : floatingTasks) {  

            LocalDate today = LocalDate.now();
            LocalDate searchStartDate = today;
            LocalDate searchEndDate = today;

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
                searchEndDate = today.plusYears(1);  // give task a 1 year window to find a slot on another day
            }

            long taskMinutes = (long) (task.duration * 60);  
            boolean isFirstDayScheduled = true;
            boolean scheduledAtLeastOnce = false;

            for (LocalDate currentDate = searchStartDate; 
                 !currentDate.isAfter(searchEndDate); 
                 currentDate = currentDate.plusDays(1)) {

                // Gatekeeper Check
                // If it's a Work task and this day is already maxed out, skip to tomorrow
                if (task.category == 1) {
                    int currentWorkMins = dailyWorkMinutes.getOrDefault(currentDate, 0);
                    if (currentWorkMins + taskMinutes > maxWorkMinsPerDay) {
                        continue; 
                    }
                }

                LocalDateTime dayStart = currentDate.atTime(startHour);
                LocalDateTime dayEnd = currentDate.atTime(endHour);

                LocalDateTime searchTime = dayStart;  
                boolean scheduledToday = false;

                while (!searchTime.plusMinutes(taskMinutes).isAfter(dayEnd)) {   

                    LocalDateTime potEnd = searchTime.plusMinutes(taskMinutes);
                    boolean collision = false;
                    LocalDateTime nextFreeTime = null;

                    for (Task pinned : pinnedTasks) {    

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

                        if (searchTime.isBefore(pinnedEnd) && potEnd.isAfter(pinnedStartOfDay)) {    
                            collision = true;
                            nextFreeTime = pinnedEnd;  
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

                        // Update the tracker if a Work task was scheduled
                        if (task.category == 1) {
                            int currentWorkMins = dailyWorkMinutes.getOrDefault(currentDate, 0);
                            dailyWorkMinutes.put(currentDate, currentWorkMins + (int)taskMinutes);
                        }

                        scheduledToday = true;
                        scheduledAtLeastOnce = true;
                        break; 

                    } else {
                        searchTime = nextFreeTime;  
                    }
                }
                
                if (!scheduledToday) {
                    System.out.println(task.taskName + " could not fit in the window on " + currentDate);
                } else if (taskDue == null) {
                    // The task was scheduled successfully and has no due date.
                    break;
                }
                
            } 

            if (!scheduledAtLeastOnce) {
                System.out.println(task.taskName + " could not fit anywhere in its date range.");
            }
        }
        
        floatingTasks.addAll(additionalTasks);
    }

}