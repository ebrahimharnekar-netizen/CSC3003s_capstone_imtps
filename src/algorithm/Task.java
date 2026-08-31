package algorithm;
import java.lang.invoke.StringConcatException;
import java.time.*;

public class Task {
    
    public String taskId; 
    public String taskName;
    public int category;
    public double timeUntilDue;
    public int importance;
    public double duration;
    public LocalDateTime dueDate;
    public LocalDateTime startTime;
    public LocalDate startDate; 

    // Original range (preserved for display)
    public LocalDate originalStartDate;
    public LocalDateTime originalDueDate;
    // Original start time (used to distinguish user‑pinned tasks)
    public LocalDateTime originalStartTime;

        public Task(String taskId, String taskName, int category, int importance, double duration, 
                LocalDateTime dueDate, LocalDateTime startTime, LocalDate startDate) { // main constructor
        this.taskId = taskId;
        this.category = category;
        this.taskName = taskName;
        this.importance = importance;
        this.duration = duration;
        this.dueDate = dueDate;
        this.startTime = startTime;
        this.startDate = startDate;
        this.originalStartDate = startDate;
        this.originalDueDate = dueDate;
        this.originalStartTime = startTime;   

        if (this.dueDate == null) {
            this.timeUntilDue = 87600.0;
        } else {
            LocalDateTime now = LocalDateTime.now();
            Duration timeDiff = Duration.between(now, this.dueDate);
            this.timeUntilDue = timeDiff.toMinutes() / 60.0;
        }
    }

    public Task(String taskId, String taskName, int category, int importance, double duration, 
                LocalDateTime dueDate, LocalDateTime startTime) { // Constructor for tasks without start date (USED FOR TESTING)
        this.taskId = taskId;
        this.category = category;
        this.taskName = taskName;
        this.importance = importance;
        this.duration = duration;
        this.dueDate = dueDate;
        this.startTime = startTime;
        this.startDate = null;
        this.originalStartDate = null;
        this.originalDueDate = dueDate;
        this.originalStartTime = startTime;   

        if (this.dueDate == null) {
            this.timeUntilDue = 87600.0;
        } else {
            LocalDateTime now = LocalDateTime.now();
            Duration timeDiff = Duration.between(now, this.dueDate);
            this.timeUntilDue = timeDiff.toMinutes() / 60.0;
        }
    }



    public Task(String taskName, int category, int importance, double duration, 
                LocalDateTime startTime, LocalDateTime dueDate) { // Constructor for tasks without taskId (used in testing)
        this.taskId = "0";
        this.category = category;
        this.taskName = taskName;
        this.importance = importance;
        this.duration = duration;
        this.startTime = startTime;
        this.dueDate = dueDate;
        this.startDate = null;
        this.originalStartDate = null;
        this.originalDueDate = dueDate;
        this.originalStartTime = startTime;   

        if (this.dueDate == null) {
            this.timeUntilDue = 87600.0;
        } else {
            LocalDateTime now = LocalDateTime.now();
            Duration timeDiff = Duration.between(now, this.dueDate);
            this.timeUntilDue = timeDiff.toMinutes() / 60.0;
        }
    }

    public boolean isPinned() {
        // A task is considered pinned if it originally had a non‑midnight start time
        return this.originalStartTime != null && !this.originalStartTime.toLocalTime().equals(LocalTime.of(0, 0));
    }

    public boolean isFloatingWithStartDate() {
        return this.startDate != null || 
               (this.startTime != null && this.startTime.toLocalTime().equals(LocalTime.of(0, 0)));
    }

    public int getCategoryID() {
        return category;
    }
}