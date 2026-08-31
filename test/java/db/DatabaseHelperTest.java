package db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatabaseHelperTest {

    private List<String> createdUserIds; // Updated to String

    @BeforeAll
    static void setUpDatabase() {
        // Makes sure users/tasks/preferences tables exist before any test runs
        DatabaseSetup.main(new String[0]);
    }

    @BeforeEach
    void setUp() {
        createdUserIds = new ArrayList<>();
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement()) {

            for (String id : createdUserIds) { // Updated to String
                // Enclosed the string ID in single quotes for the SQL query
                stmt.executeUpdate("DELETE FROM users WHERE user_id = '" + id + "'");
            }

        }
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    @Test
    public void testOverlapLogicWithNullStart() {
        String mockUserId = "999"; // Updated to String
        LocalDateTime nullStartTime = null;
        double duration = 2.0;

        // Passed null instead of -1 for the excludeTaskId
        boolean doesOverlap = DatabaseHelper.hasTimeOverlap(mockUserId, nullStartTime, duration, null);

        assertFalse(doesOverlap, "A task with a null start time should immediately return false for overlaps");
    }

    @Test
    public void testUserValidationFailsForBadCredentials() {

        String fakeUser = "TestUserThatDoesNotExist";
        String fakePass = "WrongPassword123";

        boolean isValid = DatabaseHelper.validateUser(fakeUser, fakePass);

        assertFalse(isValid, "Database should return false for non-existent users");
    }

    @Test
    void login_correctCredentials_succeeds() {
        String username = uniqueUsername("login");
        String password = "CorrectHorseBatteryStaple1";

        String userId = DatabaseHelper.addUser(username, username + "@example.com", password); // Updated to String
        createdUserIds.add(userId);

        assertTrue(DatabaseHelper.validateUser(username, password));
    }

    @Test
    void login_sqlInjectionInUsername_doesNotBypassAuthentication() {

        String injection = "' OR '1'='1";

        assertFalse(DatabaseHelper.validateUser(injection, "anything"),
                "a SQL injection payload in the username must not bypass login");
    }

    @Test
    void login_sqlInjectionInPassword_doesNotBypassAuthentication() {
        String username = uniqueUsername("login");

        String userId = DatabaseHelper.addUser(username, username + "@example.com", "realPassword"); // Updated to String
        createdUserIds.add(userId);

        assertFalse(DatabaseHelper.validateUser(username, "' OR '1'='1"),
                "a SQL injection payload in the password must not bypass login");
    }

    @Test
    void login_sqlInjectionCommentPayload_doesNotBypassAuthentication() {
        String username = uniqueUsername("login");

        String userId = DatabaseHelper.addUser(username, username + "@example.com", "realPassword"); // Updated to String
        createdUserIds.add(userId);

        String injection = username + "' --";

        assertFalse(DatabaseHelper.validateUser(injection, "wrongOrAnyPassword"),
                "a comment-based injection payload must not bypass the password check");
    }

    @Test
    void signUp_sqlInjectionPayloadInUsername_isStoredLiterally_tableSurvives() throws SQLException {
        String maliciousUsername = "hacker'); DROP TABLE users; --";

        String userId = DatabaseHelper.addUser(maliciousUsername, "hacker@example.com", "pw"); // Updated to String
        createdUserIds.add(userId);

        assertNotNull(userId, "sign-up should succeed, returning a valid UUID string"); // Updated assertion
        assertTrue(DatabaseHelper.userExists(maliciousUsername),
                "the malicious string should be stored (and matched) literally, not executed as SQL");

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            assertTrue(rs.next(), "the users table must still exist after the injection attempt");

        }
    }
}