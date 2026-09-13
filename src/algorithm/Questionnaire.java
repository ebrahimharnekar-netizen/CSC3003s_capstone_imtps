package algorithm;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Questionnaire {
    
    public static final int WorkMode = 1;
    public static final int HealthMode = 2;
    public static final int PersonalMode = 3;

    public static final int MorningPeriod = 1;
    public static final int AfternoonPeriod = 2;
    public static final int EveningPeriod = 3;

    private int currentMode;  // this is the current focus mode the user has toggled (work, health, personal)
    private int peakProdTime;  // the widnow they are most productive (morning->1, afternoon->2, evening->3)
    private boolean prefQuickFirst;  // if the user wants to prioritise the shorter tasks first
    private int dailyProdHours; // the number of hours a user want to be productive (1-10 hours)

    private LocalTime dayStartTime = LocalTime.of(7, 0);    // set default window to 7am to 10pm
    private LocalTime dayEndTime = LocalTime.of(22, 0);

    public Questionnaire(int currentMode, int peakProdTime, boolean prefQuickFirst, int dailyProdHours) {   // constructor

        this.currentMode = currentMode;
        this.peakProdTime = peakProdTime;
        this.prefQuickFirst = prefQuickFirst;
        this.dailyProdHours = dailyProdHours;

    }

    public void setWindow(String startStr, String endStr) {
        // Split into two separate try catch blocks so one typo does not ruin the whole window
        try {

            if (startStr != null && !startStr.trim().isEmpty()) {

                // .trim() removes hidden spaces. automatically handles HH:mm and HH:mm:ss
                this.dayStartTime = LocalTime.parse(startStr.trim());

            }
        } catch (Exception e) {

            System.out.println("Debug: Failed to parse start time -> '" + startStr + "'");

        }

        try {

            if (endStr != null && !endStr.trim().isEmpty()) {

                this.dayEndTime = LocalTime.parse(endStr.trim());

            }
        } catch (Exception e) {

            System.out.println("Debug: Failed to parse end time -> '" + endStr + "'");
            
        }
    }

    // getters
    public int getCurrentMode() {

        return currentMode;

    }

    public int getPeakProdTime() {

        return peakProdTime;

    }

    public boolean isPrefQuickFirst() {

        return prefQuickFirst;

    }

    public int getDailyProdHours() {

        return dailyProdHours;

    }

    // new getters
    public LocalTime getDayStartTime() {

        return dayStartTime;

    }

    public LocalTime getDayEndTime() {

        return dayEndTime;
        
    }

}
