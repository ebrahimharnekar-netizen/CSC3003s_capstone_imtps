package algorithm;

public class Questionnaire {
    
    public static final int WorkMode = 1;
    public static final int HealthMode = 2;
    public static final int PersonalMode = 3;

    public static final int MorningPeriod = 1;
    public static final int AfternoonPeriod = 2;
    public static final int EveningPeriod = 3;

    private int currentMode;  // this is the current focus mode the user has toggled (work, health, personal)
    private int peakProdTime;  // the widnow they are most productive (morning, afternoon, evening)
    private boolean prefQuickFirst;  // if the user wants to prioritise the shorter tasks first
    private int dailyProdHours; // the number of hours a user want to be productive (1-10 hours)

    public Questionnaire(int currentMode, int peakProdTime, boolean prefQuickFirst, int dailyProdHours) {   // constructor

        this.currentMode = currentMode;
        this.peakProdTime = peakProdTime;
        this.prefQuickFirst = prefQuickFirst;
        this.dailyProdHours = dailyProdHours;

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

}
