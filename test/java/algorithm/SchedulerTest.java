package algorithm;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SchedulerTest {

    @Test  // testing if calendar items and to do list items are recognized correctly
    public void testTaskIsPinned() {
        // create one task with a start time, and one without
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Task pinnedTask = new Task(1, "Pinned", 1, 5, 1.0, null, startTime);
        Task floatingTask = new Task(2, "Floating", 1, 3, 2.0, null, null);

        // the class should correctly identify if it is pinned
        assertTrue(pinnedTask.isPinned(), "Task with a start time should be pinned");
        assertFalse(floatingTask.isPinned(), "Task without a start time should be floating");
    }

    @Test    // testing to see if the constructor can convert a due date to the correct time remaining
    public void testTaskTimeUntilDueCalculation() {
        // create a task due 3 hours from now
        LocalDateTime dueInThreeHours = LocalDateTime.now().plusHours(3);
        Task testTask = new Task(3, "Urgent Task", 2, 4, 1.0, dueInThreeHours, null);

        // the constructor calculates timeUntilDue automatically
        assertEquals(3.0, testTask.timeUntilDue, 0.1, "Task should calculate exactly 3 hours remaining");
    }

    @Test  // testig to see if a task with no due date is pushed to the back of the line instead of throwing a null pointer exception
    public void testFloatingTaskFallbackDeadline() {
        // ACreate a task with no due date
        Task noDeadlineTask = new Task(4, "Chill Task", 3, 1, 1.0, null, null);

        // the system should assign the fallback of 87600.0 hours (10 years)
        assertEquals(87600.0, noDeadlineTask.timeUntilDue, 0.1, "Null due date should default to 10 years");
    }

    @Test    // testing to see if two identical tasks change sequence if only their category is changed
    public void testFocusModeChangesPriority() {
        // Create 2 identical tasks that only differ by Category
        Task workTask = new Task(1, "Capstone Coding", 1, 5, 1.0, LocalDateTime.now().plusDays(1), null);
        Task healthTask = new Task(2, "10km Run", 2, 5, 1.0, LocalDateTime.now().plusDays(1), null);

        // Test 1: Set Focus Mode to Work
        Questionnaire workPrefs = new Questionnaire(1, 1, false, 8); 
        Scheduler algorithm = new Scheduler();
        
        List<Task> rawList1 = new ArrayList<>();
        rawList1.add(workTask);
        rawList1.add(healthTask);
        
        List<Task> workSchedule = algorithm.genSchedule(rawList1, workPrefs);
        assertEquals("Capstone Coding", workSchedule.get(0).taskName, "Work task should be scheduled first in Work Mode");

        // Test 2: Set Focus Mode to Health
        // We recreate the tasks so they don't have leftover startTimes from the first run
        Task workTask2 = new Task(1, "Capstone Coding", 1, 5, 1.0, LocalDateTime.now().plusDays(1), null);
        Task healthTask2 = new Task(2, "10km Run", 2, 5, 1.0, LocalDateTime.now().plusDays(1), null);
        
        Questionnaire healthPrefs = new Questionnaire(2, 1, false, 8); 
        
        List<Task> rawList2 = new ArrayList<>();
        rawList2.add(workTask2);
        rawList2.add(healthTask2);

        List<Task> healthSchedule = algorithm.genSchedule(rawList2, healthPrefs);
        assertEquals("10km Run", healthSchedule.get(0).taskName, "Health task should be scheduled first in Health Mode");
    }

    @Test  // testing to see if algorithm respects 15 hour work day set by us
    public void testTimeSlotExhaustion() {
        // The day is 7 AM to 10 PM
        // We create a massive pinned task that takes up 14 hours (7 AM to 9 PM)
        LocalDateTime today7AM = java.time.LocalDate.now().atTime(7, 0);
        Task giantPinnedTask = new Task(1, "Long Shift", 1, 5, 14.0, null, today7AM);
        
        // We create two 1 hour floating tasks
        Task highPriorityTask = new Task(2, "Important Task", 1, 5, 1.0, LocalDateTime.now().plusDays(1), null);
        Task lowPriorityTask = new Task(3, "Chill Task", 1, 1, 1.0, today7AM.plusHours(15), null);

        List<Task> rawList = new ArrayList<>();
        rawList.add(giantPinnedTask);
        rawList.add(highPriorityTask);
        rawList.add(lowPriorityTask);

        Questionnaire dummyPrefs = new Questionnaire(1, 1, false, 8);
        Scheduler algorithm = new Scheduler();
        algorithm.genSchedule(rawList, dummyPrefs);

        // The high priority task should claim the final 1 hour slot from 9 PM to 10 PM
        assertNotNull(highPriorityTask.startTime, "Higher priority task should be scheduled in the last available slot");
        assertEquals(today7AM.plusHours(14), highPriorityTask.startTime, "Task should start exactly at 9 PM");

        // The low priority task should be completely skipped because the 15-hour day is full
        assertNull(lowPriorityTask.startTime, "Lower priority task should remain unscheduled because the day ended");
    }

    @Test  // testing to see if the alternate scheduling algorithm correctly ignores the importance and schedules tasks based on the due date
    public void testAltSchedulerPrioritizesDeadlines() {
        // High importance, but due next week
        Task importantButLate = new Task(1, "Important, Due Late", 1, 5, 1.0, LocalDateTime.now().plusDays(7), null);
        
        // Low importance, but due in exactly 1 hour
        Task chillButUrgent = new Task(2, "Chill, Due Soon", 1, 1, 1.0, LocalDateTime.now().plusHours(1), null);

        List<Task> rawList = new ArrayList<>();
        rawList.add(importantButLate);
        rawList.add(chillButUrgent);

        AltScheduler altAlgorithm = new AltScheduler();
        Questionnaire dummyPrefs = new Questionnaire(1, 1, false, 8);
        
        List<Task> schedule = altAlgorithm.genSchedule(rawList, dummyPrefs);

        // AltScheduler strictly looks at timeUntilDue, ignoring importance entirely
        assertEquals("Chill, Due Soon", schedule.get(0).taskName, "AltScheduler failed to prioritize the absolute earliest deadline!");
    }

}