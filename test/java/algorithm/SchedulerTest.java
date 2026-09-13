package algorithm;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SchedulerTest {

    @Test  // testing if calendar items and to do list items are recognized correctly
    public void testTaskIsPinned() {
        // create one task with a start time, and one without
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Task pinnedTask = new Task("1", "Pinned", 1, 5, 1.0, null, startTime);
        Task floatingTask = new Task("2", "Floating", 1, 3, 2.0, null, null);

        // the class should correctly identify if it is pinned
        assertTrue(pinnedTask.isPinned(), "Task with a start time should be pinned");
        assertFalse(floatingTask.isPinned(), "Task without a start time should be floating");
    }

    @Test    // testing to see if the constructor can convert a due date to the correct time remaining
    public void testTaskTimeUntilDueCalculation() {
        // create a task due 3 hours from now
        LocalDateTime dueInThreeHours = LocalDateTime.now().plusHours(3);
        Task testTask = new Task("3", "Urgent Task", 2, 4, 1.0, dueInThreeHours, null);

        // the constructor calculates timeUntilDue automatically
        assertEquals(3.0, testTask.timeUntilDue, 0.1, "Task should calculate exactly 3 hours remaining");
    }

    @Test  // testig to see if a task with no due date is pushed to the back of the line instead of throwing a null pointer exception
    public void testFloatingTaskFallbackDeadline() {
        // ACreate a task with no due date
        Task noDeadlineTask = new Task("4", "Chill Task", 3, 1, 1.0, null, null);

        // the system should assign the fallback of 87600.0 hours (10 years)
        assertEquals(87600.0, noDeadlineTask.timeUntilDue, 0.1, "Null due date should default to 10 years");
    }

    @Test    // testing to see if two identical tasks change sequence if only their category is changed
    public void testFocusModeChangesPriority() {
        // Create 2 identical tasks that only differ by Category
        Task workTask = new Task("1", "Capstone Coding", 1, 5, 1.0, LocalDateTime.now().plusDays(1), null);
        Task healthTask = new Task("2", "10km Run", 2, 5, 1.0, LocalDateTime.now().plusDays(1), null);

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
        Task workTask2 = new Task("1", "Capstone Coding", 1, 5, 1.0, LocalDateTime.now().plusDays(1), null);
        Task healthTask2 = new Task("2", "10km Run", 2, 5, 1.0, LocalDateTime.now().plusDays(1), null);
        
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
        Task giantPinnedTask = new Task("1", "Long Shift", 1, 5, 14.0, null, today7AM);
        
        // We create two 1 hour floating tasks
        Task highPriorityTask = new Task("2", "Important Task", 1, 5, 1.0, LocalDateTime.now().plusDays(1), null);
        Task lowPriorityTask = new Task("3", "Chill Task", 1, 1, 1.0, today7AM.plusHours(15), null);

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
        Task importantButLate = new Task("1", "Important, Due Late", 1, 5, 1.0, LocalDateTime.now().plusDays(7), null);
        
        // Low importance, but due in exactly 1 hour
        Task chillButUrgent = new Task("2", "Chill, Due Soon", 1, 1, 1.0, LocalDateTime.now().plusHours(1), null);

        List<Task> rawList = new ArrayList<>();
        rawList.add(importantButLate);
        rawList.add(chillButUrgent);

        DeadlineScheduler altAlgorithm = new DeadlineScheduler();
        Questionnaire dummyPrefs = new Questionnaire(1, 1, false, 8);
        
        List<Task> schedule = altAlgorithm.genSchedule(rawList, dummyPrefs);

        // AltScheduler strictly looks at timeUntilDue, ignoring importance entirely
        assertEquals("Chill, Due Soon", schedule.get(0).taskName, "AltScheduler failed to prioritize the absolute earliest deadline!");
    }


    @Test // testing to ensure the dynamic work window logic functions correctly in the Deadline Scheduler
    public void testDeadlineSchedulerDynamicWindow() {
        // Create a 2 hour task
        Task eveningTask = new Task("1", "Evening Task", 1, 5, 2.0, null, null);
        List<Task> rawList = new ArrayList<>();
        rawList.add(eveningTask);

        // Set a custom short window from 8 PM to 10 PM
        Questionnaire customPrefs = new Questionnaire(1, 1, false, 8);
        customPrefs.setWindow("20:00", "22:00");

        DeadlineScheduler deadlineAlg = new DeadlineScheduler();
        deadlineAlg.genSchedule(rawList, customPrefs);

        // The task should snap to the new custom start time (20:00), not the default 07:00
        assertNotNull(eveningTask.startTime, "Task should be scheduled within the dynamic window");
        assertEquals(LocalTime.of(20, 0), eveningTask.startTime.toLocalTime(), "Task did not start at the dynamic 20:00 start time");
    }

    @Test // testing to ensure the 0/1 DP matrix correctly selects the maximum importance value under the time capacity
    public void testKnapsackOptimizesImportance() {
        // Our default knapsack capacity is 15 hours (900 minutes)
        
        // Task A takes up 10 hours but gives 5 importance
        Task heavyLowValue = new Task("1", "Heavy Low Value", 1, 5, 10.0, null, null);
        
        // Task B and C take up 15 hours combined, but give a total of 8 importance
        Task mediumHighValue1 = new Task("2", "Medium High Value 1", 1, 4, 8.0, null, null);
        Task mediumHighValue2 = new Task("3", "Medium High Value 2", 1, 4, 7.0, null, null);

        List<Task> rawList = new ArrayList<>();
        rawList.add(heavyLowValue);
        rawList.add(mediumHighValue1);
        rawList.add(mediumHighValue2);

        Questionnaire dummyPrefs = new Questionnaire(1, 1, false, 15);
        KnapsackScheduler knapsack = new KnapsackScheduler();
        
        List<Task> optimalSchedule = knapsack.genSchedule(rawList, dummyPrefs);

        // The DP table should realize that (Task B + Task C) = 15 hours and 8 Importance
        // While (Task A) = 10 hours and 5 Importance, and no other tasks fit.
        // It must pack B and C first, pushing A to the leftover pile (index 2).
        assertEquals("Medium High Value 2", optimalSchedule.get(0).taskName, "Knapsack failed to pack the optimal subset");
        assertEquals("Medium High Value 1", optimalSchedule.get(1).taskName, "Knapsack failed to pack the optimal subset");
        assertEquals("Heavy Low Value", optimalSchedule.get(2).taskName, "The less optimal task should have been pushed to leftovers");
    }

    @Test // testing to ensure Work tasks respect the dailyProdHours limit and overflow to tomorrow
    public void testKnapsackRespectsWorkLimit() {
        // Set a 10-hour day (08:00 to 18:00) but a strict 3 hour work limit
        Questionnaire burnoutPrefs = new Questionnaire(1, 1, false, 3);
        burnoutPrefs.setWindow("08:00", "18:00");
        
        // We give work2 a due date of tomorrow so the sliding window is allowed to look at tomorrow.
        // work1 gets a null due date so it restricts itself purely to today.
        LocalDateTime dueTomorrow = LocalDateTime.now().plusDays(1);
        Task work1 = new Task("1", "High Priority Work", 1, 5, 2.0, null, null);
        Task work2 = new Task("2", "Low Priority Work", 1, 3, 2.0, dueTomorrow, null);
        
        List<Task> rawList = new ArrayList<>();
        rawList.add(work1);
        rawList.add(work2);
        
        KnapsackScheduler knapsack = new KnapsackScheduler();
        knapsack.genSchedule(rawList, burnoutPrefs);
        
        LocalDate today = LocalDate.now();
        
        // Work 1 fits perfectly in today's 3-hour limit
        assertEquals(today, work1.startTime.toLocalDate(), "Highest importance work task should schedule today");
        
        // Work 2 pushes today's total to 4 hours, which breaks the 3-hour limit. 
        // It must be successfully pushed to tomorrow.
        assertEquals(today.plusDays(1), work2.startTime.toLocalDate(), "Second work task must be pushed to tomorrow due to the burnout limit");
    }

    @Test // testing to ensure Health and Personal bypass the work limit and fill the day
    public void testKnapsackHealthBypassesWorkLimit() {
        // Set a 10-hour day (08:00 to 18:00) but a strict 2-hour work limit
        Questionnaire mixedPrefs = new Questionnaire(1, 1, false, 2);
        mixedPrefs.setWindow("08:00", "18:00");
        
        // 2 hours of Work, 4 hours of Health, 3 hours of Personal (Total = 9 hours)
        Task workTask = new Task("1", "Coding", 1, 5, 2.0, null, null);
        Task healthTask = new Task("2", "Long Run", 2, 4, 4.0, null, null);
        Task personalTask = new Task("3", "Gaming", 3, 3, 3.0, null, null);
        
        List<Task> rawList = new ArrayList<>();
        rawList.add(workTask);
        rawList.add(healthTask);
        rawList.add(personalTask);
        
        KnapsackScheduler knapsack = new KnapsackScheduler();
        knapsack.genSchedule(rawList, mixedPrefs);
        
        LocalDate today = LocalDate.now();
        
        // Because Health and Personal tasks only check total capacity (10 hrs), not work capacity (2 hrs),
        // ALL of these tasks should successfully schedule on Day 1.
        assertEquals(today, workTask.startTime.toLocalDate(), "Work task should schedule today");
        assertEquals(today, healthTask.startTime.toLocalDate(), "Health task ignores work limit and should schedule today");
        assertEquals(today, personalTask.startTime.toLocalDate(), "Personal task ignores work limit and should schedule today");
    }

    @Test // testing to ensure Priority Scheduler also respects the dailyProdHours burnout limit
    public void testPrioritySchedulerRespectsWorkLimit() {
        // Set a 10-hour day (08:00 to 18:00) but a strict 3-hour work limit
        Questionnaire burnoutPrefs = new Questionnaire(1, 1, false, 3);
        burnoutPrefs.setWindow("08:00", "18:00");
        
        LocalDateTime dueTomorrow = LocalDateTime.now().plusDays(1);
        Task work1 = new Task("1", "High Priority Work", 1, 5, 2.0, null, null);
        Task work2 = new Task("2", "Lower Priority Work", 1, 3, 2.0, dueTomorrow, null);
        
        List<Task> rawList = new ArrayList<>();
        rawList.add(work1);
        rawList.add(work2);
        
        Scheduler priorityScheduler = new Scheduler();
        priorityScheduler.genSchedule(rawList, burnoutPrefs);
        
        LocalDate today = LocalDate.now();
        
        assertEquals(today, work1.startTime.toLocalDate(), "Highest priority work task should schedule today");
        assertEquals(today.plusDays(1), work2.startTime.toLocalDate(), "Second work task must be pushed to tomorrow due to the burnout limit");
    }

    @Test // testing to ensure Deadline Scheduler also respects the dailyProdHours burnout limit
    public void testDeadlineSchedulerRespectsWorkLimit() {
        // Set a 10 hour day (08:00 to 18:00) but a strict 3 hour work limit
        Questionnaire burnoutPrefs = new Questionnaire(1, 1, false, 3);
        burnoutPrefs.setWindow("08:00", "18:00");
        
        // DeadlineScheduler sorts by earliest deadline first.
        // work1 is due today, so it gets scheduled first.
        LocalDateTime dueToday = LocalDateTime.now().plusHours(5);
        // work2 is due tomorrow, so it gets scheduled second, and has permission to slide to tomorrow.
        LocalDateTime dueTomorrow = LocalDateTime.now().plusDays(1);
        
        Task work1 = new Task("1", "Urgent Work", 1, 5, 2.0, dueToday, null);
        Task work2 = new Task("2", "Less Urgent Work", 1, 3, 2.0, dueTomorrow, null);
        
        List<Task> rawList = new ArrayList<>();
        rawList.add(work1);
        rawList.add(work2);
        
        DeadlineScheduler deadlineScheduler = new DeadlineScheduler();
        deadlineScheduler.genSchedule(rawList, burnoutPrefs);
        
        LocalDate today = LocalDate.now();
        
        assertEquals(today, work1.startTime.toLocalDate(), "Most urgent work task should schedule today");
        assertEquals(today.plusDays(1), work2.startTime.toLocalDate(), "Second work task must be pushed to tomorrow due to the burnout limit");
    }

}