import algorithm.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class IMTPS {

    public static void main(String[] args){

        // 1. MOCK THE USER
        // User wants: Work mode (1), Morning peak (1), Prefers quick tasks (true), 8 hours of work
        Questionnaire userPrefs = new Questionnaire(3, 1, true, 8);
        LocalDateTime now = LocalDateTime.now();

        // ====================================================================
        // THE MOCK DATA LOADER (Helper method to generate our obstacle course)
        // ====================================================================
        List<Task> allTasks = getStressTestTasks(now);

        // ====================================================================
        // 3. RUN ALGORITHM 1 (The Custom Balanced Engine)
        // ====================================================================
        System.out.println("=== RUNNING: CUSTOM BALANCED SCHEDULER ===");
        SchedulingAlg mainEngine = new Scheduler();
        List<Task> mainSchedule = mainEngine.genSchedule(new ArrayList<>(allTasks), userPrefs);

        for (Task t : mainSchedule) {
            System.out.println("- " + t.taskName + " | Start Time: " +
            (t.startTime != null ? t.startTime.toLocalTime() : "Unscheduled"));
        }

        System.out.println("\n------------------------------------------------\n");

        // ====================================================================
        // RESET THE TASKS SO ALGORITHM 2 GETS A CLEAN SLATE IN MEMORY
        // ====================================================================
        allTasks.clear();
        allTasks = getStressTestTasks(now);

        // ====================================================================
        // 4. RUN ALGORITHM 2 (The Earliest Deadline First Engine)
        // ====================================================================
        System.out.println("=== RUNNING: DEADLINE SCHEDULER ===");
        SchedulingAlg altEngine = new DeadlineScheduler();
        List<Task> deadlineSchedule = altEngine.genSchedule(new ArrayList<>(allTasks), userPrefs);

        for (Task t : deadlineSchedule) {
            System.out.println("- " + t.taskName + " | Start Time: " +
            (t.startTime != null ? t.startTime.toLocalTime() : "Unscheduled"));
        }
    }

    // Helper method to keep the main function clean
    private static List<Task> getStressTestTasks(LocalDateTime now) {
        List<Task> tasks = new ArrayList<>();
        LocalDate today = java.time.LocalDate.now();

        // --- PINNED TASKS (The Obstacles) ---
        // 08:30 AM to 10:00 AM (1.5 hours)
        tasks.add(new Task("999999","Morning Team Standup", 1, 5, 1.5, null, today.atTime(8, 30)));
        
        // 13:00 PM to 14:00 PM (1.0 hour)
        tasks.add(new Task("888888","Lunch with Nadim", 3, 5, 1.0, null, today.atTime(13, 0)));
        
        // 16:00 PM to 18:00 PM (2.0 hours)
        tasks.add(new Task("777777","Computer Networks Lecture", 1, 5, 2.0, null, today.atTime(16, 0)));

        // --- FLOATING TASKS (The Puzzle Pieces) ---
        // A. Urgent but low importance (Due in 2 hours)
        tasks.add(new Task("66666","Buy Groceries", 3, 2, 0.5, now.plusHours(2), null));

        // B. Extremely high priority, matches Work focus (Due in 5 hours)
        tasks.add(new Task("55555","Fix Critical Server Bug", 1, 5, 1.5, now.plusHours(5), null));

        // C. Quick win, high importance (Due in 4 hours)
        tasks.add(new Task("44444","Reply to Capstone Supervisor", 1, 4, 0.5, now.plusHours(4), null));

        // D. Mid-priority, standard length (Due in 8 hours)
        tasks.add(new Task("33333","Review Database SQL Code", 1, 3, 1.0, now.plusHours(8), null));

        // E. Health category, solid importance (Due in 12 hours)
        tasks.add(new Task("22222","10km Training Run", 2, 4, 1.0, now.plusHours(12), null));

        // F. Big project, low urgency (Due in 48 hours)
        tasks.add(new Task("11111","Write Algorithm Documentation", 1, 4, 2.0, now.plusHours(48), null));

        // G. The massive chore to trigger the Overflow Alert (Duration = 6 hours)
        tasks.add(new Task("12222","Deep clean entire apartment", 3, 1, 6.0, null, null));

        return tasks;
    }
}