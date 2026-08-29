package algorithm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuestionnaireTest {

    @Test
    public void testQuestionnaireInitialization() {
        // 1. Arrange: Create a questionnaire with specific preferences[cite: 22]
        int mode = Questionnaire.HealthMode; // 2[cite: 22]
        int peakTime = Questionnaire.AfternoonPeriod; // 2[cite: 22]
        boolean quickFirst = true; //[cite: 22]
        int dailyHours = 5; //[cite: 22]

        // 2. Act: Instantiate the object[cite: 22]
        Questionnaire prefs = new Questionnaire(mode, peakTime, quickFirst, dailyHours);

        // 3. Assert: Verify the getters return the exact data we passed in[cite: 22]
        assertEquals(Questionnaire.HealthMode, prefs.getCurrentMode(), "Focus mode should be Health (2)");
        assertEquals(Questionnaire.AfternoonPeriod, prefs.getPeakProdTime(), "Peak time should be Afternoon (2)");
        assertTrue(prefs.isPrefQuickFirst(), "Prefer quick tasks first should be true");
        assertEquals(5, prefs.getDailyProdHours(), "Daily productive hours should be 5");
    }
}