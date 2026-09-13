package algorithm;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DeadlineScheduler implements SchedulingAlg{
    
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

        findTimeSlots(floatingTasks, pinnedTasks, userPrefs);

        return floatingTasks;

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
                searchEndDate = today;
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
                }
                
            } 

            if (!scheduledAtLeastOnce) {
                System.out.println(task.taskName + " could not fit anywhere in its date range.");
            }
        }
        
        floatingTasks.addAll(additionalTasks);
    }

}