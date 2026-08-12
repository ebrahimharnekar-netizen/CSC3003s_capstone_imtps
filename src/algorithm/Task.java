package algorithm;
import java.time.*;

public class Task {
    
    // will declare task variables here.

    public String taskName;
    public int category;   // same as focus modes (work, health, personal)
    public double timeUntilDue;  // hours until the due date
    public int importance;  // out of 5
    public double duration;  // the duration set by the user for how long they think their event will be.
    public LocalDateTime dueDate;   // the due date of a task, it is optional to the user
    public LocalDateTime startTime;   // this tracks whether a user explicitly put a task on the calendar, therefore the algorithm has to work around that task.

    public Task(String taskName, int category, int importance, double duration, LocalDateTime dueDate, LocalDateTime startTime){

        this.category = category;
        this.taskName = taskName;
        this.importance = importance;
        this.duration = duration;
        this.dueDate = dueDate;
        this.startTime = startTime;  // so if the user doesnt explicitly put a task on the calendar(clicks calendar block instead of add task to scheduler button) the algorithm schedules it.

        if (this.dueDate == null) {

            this.timeUntilDue = 87600.0;  // if user does not give a task a due date, it is thereofre not as important as deadline tasks and is assigned 10 years to make sure the deadline tasks are given higher priority.

        } else{

            LocalDateTime now = LocalDateTime.now();  // get the exact time 'now'
            Duration timeDiff = Duration.between(now, this.dueDate);  // get the time between 'now' and the due date of the task.
            this.timeUntilDue = timeDiff.toMinutes() / 60.0;  // for example this converts 2hr30min to 2.5 hours.

        }

    }

    public boolean isPinned(){   // this method is to check if a task is pinned or not by the user.

        return this.startTime != null;

    }

    public int getCategoryID() {

        return category;

    }

}
