package algorithm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuestionnaireTest {

    @Test
    public void testQuestionnaireInitialization() {

        int mode = Questionnaire.HealthMode; 
        int peakTime = Questionnaire.AfternoonPeriod;
        boolean quickFirst = true;
        int dailyHours = 5; 


        Questionnaire prefs = new Questionnaire(mode, peakTime, quickFirst, dailyHours);


        assertEquals(Questionnaire.HealthMode, prefs.getCurrentMode(), "Focus mode should be Health (2)");
        assertEquals(Questionnaire.AfternoonPeriod, prefs.getPeakProdTime(), "Peak time should be Afternoon (2)");
        assertTrue(prefs.isPrefQuickFirst(), "Prefer quick tasks first should be true");
        assertEquals(5, prefs.getDailyProdHours(), "Daily productive hours should be 5");
    }
}