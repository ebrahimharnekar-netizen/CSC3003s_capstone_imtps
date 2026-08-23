package algorithm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// this code is used to test the scheduler algorithm

public class SchedulerTest {

    @Test
    public void testAlgorithmPrioritizesHighStressTasks() {
        // create tasks
        Task lowPriority = new Task("Read Chapter 4", 1, 2, 2.0, LocalDateTime.now().plusDays(2), null); 
        Task highPriority = new Task("Finish Capstone Code", 1, 5, 5.0, LocalDateTime.now().plusHours(2), null);
        
        List<Task> rawList = new ArrayList<>();
        rawList.add(lowPriority);
        rawList.add(highPriority);
        
        // Create a dummy Questionnaire for the algorithm to use for preferences
        // Update the parameters in these brackets to match your real Questionnaire constructor
        Questionnaire dummyPrefs = new Questionnaire(1, 1, false, 8); 
        
        Scheduler myAlgorithm = new Scheduler();

        // Call the exact method name and pass both the task list AND the preferences
        List<Task> sortedList = myAlgorithm.genSchedule(rawList, dummyPrefs);

        assertNotNull(sortedList, "The generated schedule should not be null");
        assertEquals(2, sortedList.size(), "The schedule should contain exactly 2 items");
        
        // We expect the high-importance, closer-deadline task to be moved to index 0
        assertEquals("Finish Capstone Code", sortedList.get(0).taskName, "Algorithm failed to prioritize the high-stress task!");
    }

    @Test
    public void testFloatingTaskAvoidsPinnedObstacles() {
        
        // creating preferences
        Questionnaire dummyPrefs = new Questionnaire(1, 1, true, 8); 

        // Define exactly 7:00 AM today (when the algorithm's workday starts)
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime today7AM = today.atTime(7, 0);

        // Create a pinned task that blocks 7:00 AM to 9:00 AM
        Task pinnedTask = new Task("Morning Meeting", 1, 5, 2.0, null, today7AM); 

        // Create a floating task that needs 1 hour of time
        Task floatingTask = new Task("Do Homework", 1, 3, 1.0, java.time.LocalDateTime.now().plusDays(1), null); 
        
        List<Task> rawList = new ArrayList<>();
        rawList.add(pinnedTask);
        rawList.add(floatingTask);

        Scheduler myAlgorithm = new Scheduler();

        // genSchedule method returns the list of floating tasks
        List<Task> scheduledTasks = myAlgorithm.genSchedule(rawList, dummyPrefs);

        assertEquals(1, scheduledTasks.size(), "Should only return the scheduled floating tasks");
        
        Task scheduledFloatingTask = scheduledTasks.get(0);
        
        // We expect the algorithm to see the 7AM-9AM block and place this task at exactly 9:00 AM
        java.time.LocalDateTime expectedStartTime = today7AM.plusHours(2);
        
        assertEquals(expectedStartTime, scheduledFloatingTask.startTime, "Floating task failed to avoid the pinned calendar obstacle!");
    }
}