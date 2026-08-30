package algorithm;
import java.time.*;

public class Task {
    
    public int taskId; 
    public String taskName;
    public int category;
    public double timeUntilDue;
    public int importance;
    public double duration;
    public LocalDateTime dueDate;
    public LocalDateTime startTime;
    public LocalDate startDate; 

    // Constructor with taskId
    public Task(int taskId, String taskName, int category, int importance, double duration, 
                LocalDateTime dueDate, LocalDateTime startTime) {
        this.taskId = taskId;
        this.category = category;
        this.taskName = taskName;
        this.importance = importance;
        this.duration = duration;
        this.dueDate = dueDate;
        this.startTime = startTime;
        this.startDate = null;

        if (this.dueDate == null) {
            this.timeUntilDue = 87600.0;
        } else {
            LocalDateTime now = LocalDateTime.now();
            Duration timeDiff = Duration.between(now, this.dueDate);
            this.timeUntilDue = timeDiff.toMinutes() / 60.0;
        }
    }

    // NEW: Constructor with startDate
    public Task(int taskId, String taskName, int category, int importance, double duration, 
                LocalDateTime dueDate, LocalDateTime startTime, LocalDate startDate) {
        this.taskId = taskId;
        this.category = category;
        this.taskName = taskName;
        this.importance = importance;
        this.duration = duration;
        this.dueDate = dueDate;
        this.startTime = startTime;
        this.startDate = startDate;

        if (this.dueDate == null) {
            this.timeUntilDue = 87600.0;
        } else {
            LocalDateTime now = LocalDateTime.now();
            Duration timeDiff = Duration.between(now, this.dueDate);
            this.timeUntilDue = timeDiff.toMinutes() / 60.0;
        }
    }

    // Overload constructor used by the UI
    public Task(String taskName, int category, int importance, double duration, 
                LocalDateTime startTime, LocalDateTime dueDate) {
        this.taskId = 0;
        this.category = category;
        this.taskName = taskName;
        this.importance = importance;
        this.duration = duration;
        this.startTime = startTime;
        this.dueDate = dueDate;
        this.startDate = null;

        if (this.dueDate == null) {
            this.timeUntilDue = 87600.0;
        } else {
            LocalDateTime now = LocalDateTime.now();
            Duration timeDiff = Duration.between(now, this.dueDate);
            this.timeUntilDue = timeDiff.toMinutes() / 60.0;
        }
    }

    public boolean isPinned() {
        // A task is pinned if:
        // 1. It has a start time
        // 2. The start time is NOT 00:00 (which indicates floating with start date)
        // 3. The start date matches the due date
        if (this.startTime == null) return false;
        if (this.startTime.toLocalTime().equals(LocalTime.of(0, 0))) return false;
        if (this.dueDate == null) return false;
        return this.startTime.toLocalDate().equals(this.dueDate.toLocalDate());
    }

    public boolean isFloatingWithStartDate() {
        return this.startDate != null || 
               (this.startTime != null && this.startTime.toLocalTime().equals(LocalTime.of(0, 0)));
    }

    public int getCategoryID() {
        return category;
    }
}