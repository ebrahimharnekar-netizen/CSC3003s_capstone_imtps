package algorithm;

public class Questionnaire {
    
    private String currentMode;  // this is the current focus mode the user has toggled (work, health, personal)
    private String peakProdTime;  // the widnow they are most productive (morning, afternoon, evening)
    private boolean prefQuickFirst;  // if the user wants to prioritise the shorter tasks first
    private int dailyProdHours; // the number of hours a user want to be productive (1-10 hours)

    public Questionnaire(String currentMode, String peakProdTime, boolean prefQuickFirst, int dailyProdHours) {   // constructor

        this.currentMode = currentMode;
        this.peakProdTime = peakProdTime;
        this.prefQuickFirst = prefQuickFirst;
        this.dailyProdHours = dailyProdHours;

    }
    // getters
    public String getCurrentMode() {

        return currentMode;

    }

    public String getPeakProdTime() {

        return peakProdTime;

    }

    public boolean isPrefQuickFirst() {

        return prefQuickFirst;

    }

    public int getDailyProdHours() {

        return dailyProdHours;

    }

}
