package algorithm;
import java.util.*;

public interface SchedulingAlg {
    
    // the two sheduling algorithms have to have this method within them and any new scheduling algorithms.

    List<Task> genSchedule(List<Task> allTasks, Questionnaire userPrefs);

}
