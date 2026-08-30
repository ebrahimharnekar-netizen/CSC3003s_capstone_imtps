package algorithm;
import java.time.*;

public class Task {
    
    // We added the taskId variable right here at the top
    public int taskId; 
    
    public String taskName;
    public int category;   // same as focus modes (work, health, personal)
    public double timeUntilDue;  // hours until the due date
    public int importance;  // out of 5
    public double duration;  // the duration set by the user for how long they think their event will be.
    public LocalDateTime dueDate;   // the due date of a task, it is optional to the user
    public LocalDateTime startTime;   // this tracks whether a user explicitly put a task on the calendar

    // We updated the constructor to require the taskId when creating the object
    public Task(int taskId, String taskName, int category, int importance, double duration, LocalDateTime dueDate, LocalDateTime startTime){
        
        this.taskId = taskId;
        this.category = category;
        this.taskName = taskName;
        this.importance = importance;
        this.duration = duration;
        this.dueDate = dueDate;
        this.startTime = startTime;  

        if (this.dueDate == null) {
            this.timeUntilDue = 87600.0;  
        } else {
            LocalDateTime now = LocalDateTime.now();  
            Duration timeDiff = Duration.between(now, this.dueDate);  
            this.timeUntilDue = timeDiff.toMinutes() / 60.0;  
        }
    }

    // this is an overload constructor used by the ui when creating a new task, it sets the task id to 0 until the database assigns a new one to it.
    public Task(String taskName, int category, int importance, double duration, LocalDateTime startTime, LocalDateTime dueDate){
        
        this.taskId = 0; 
        this.category = category;
        this.taskName = taskName;
        this.importance = importance;
        this.duration = duration;
        this.startTime = startTime;
        this.dueDate = dueDate;

        if (this.dueDate == null) {
            this.timeUntilDue = 87600.0;  
        } else {
            LocalDateTime now = LocalDateTime.now();  
            Duration timeDiff = Duration.between(now, this.dueDate);  
            this.timeUntilDue = timeDiff.toMinutes() / 60.0;  
        }
    }

    public boolean isPinned(){   
        return this.startTime != null;
    }

    public int getCategoryID() {
        return category;
    }
}
