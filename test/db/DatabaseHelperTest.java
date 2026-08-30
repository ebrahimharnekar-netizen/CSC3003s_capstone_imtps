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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatabaseHelperTest {

    private List<Integer> createdUserIds;
 
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
 
            for (int id : createdUserIds) {
                stmt.executeUpdate("DELETE FROM users WHERE user_id = " + id);
            }
 
        }
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    @Test
    public void testOverlapLogicWithNullStart() {
        // Arrange: A new task that is floating (null start time)[cite: 27]
        int mockUserId = 999;
        LocalDateTime nullStartTime = null;
        double duration = 2.0;

        // Act: Run it through your overlap checker[cite: 27]
        boolean doesOverlap = DatabaseHelper.hasTimeOverlap(mockUserId, nullStartTime, duration, -1);

        // Assert: A floating task can never overlap because it isn't pinned yet[cite: 27]
        assertFalse(doesOverlap, "A task with a null start time should immediately return false for overlaps");
    }
    
    @Test
    public void testUserValidationFailsForBadCredentials() {
        // Arrange: A completely fake username and password[cite: 27]
        String fakeUser = "TestUserThatDoesNotExist";
        String fakePass = "WrongPassword123";

        // Act: Attempt to validate[cite: 27]
        boolean isValid = DatabaseHelper.validateUser(fakeUser, fakePass);

        // Assert: The database should reject it[cite: 27]
        assertFalse(isValid, "Database should return false for non-existent users");
    }

    @Test
    void login_correctCredentials_succeeds() {
        String username = uniqueUsername("login");
        String password = "CorrectHorseBatteryStaple1";
 
        int userId = DatabaseHelper.addUser(username, username + "@example.com", password);
        createdUserIds.add(userId);
 
        assertTrue(DatabaseHelper.validateUser(username, password));
    }

    @Test
    void login_sqlInjectionInUsername_doesNotBypassAuthentication() {
        // Classic auth-bypass payload. If validateUser() built its query with string
        // concatenation instead of a PreparedStatement, this would make the WHERE
        // clause always true and log in as the first user in the table.
        String injection = "' OR '1'='1";
 
        assertFalse(DatabaseHelper.validateUser(injection, "anything"),
                "a SQL injection payload in the username must not bypass login");
    }

    @Test
    void login_sqlInjectionInPassword_doesNotBypassAuthentication() {
        String username = uniqueUsername("login");
 
        int userId = DatabaseHelper.addUser(username, username + "@example.com", "realPassword");
        createdUserIds.add(userId);
 
        assertFalse(DatabaseHelper.validateUser(username, "' OR '1'='1"),
                "a SQL injection payload in the password must not bypass login");
    }

    @Test
    void login_sqlInjectionCommentPayload_doesNotBypassAuthentication() {
        String username = uniqueUsername("login");
 
        int userId = DatabaseHelper.addUser(username, username + "@example.com", "realPassword");
        createdUserIds.add(userId);
 
        // Payload that would comment out the rest of the query (including the password
        // check) in a naive, concatenated SQL statement.
        String injection = username + "' --";
 
        assertFalse(DatabaseHelper.validateUser(injection, "wrongOrAnyPassword"),
                "a comment-based injection payload must not bypass the password check");
    }

    @Test
    void signUp_sqlInjectionPayloadInUsername_isStoredLiterally_tableSurvives() throws SQLException {
        String maliciousUsername = "hacker'); DROP TABLE users; --";
 
        int userId = DatabaseHelper.addUser(maliciousUsername, "hacker@example.com", "pw");
        createdUserIds.add(userId);
 
        assertTrue(userId > 0, "sign-up should succeed, treating the payload as a literal string");
        assertTrue(DatabaseHelper.userExists(maliciousUsername),
                "the malicious string should be stored (and matched) literally, not executed as SQL");
 
        // If the payload had actually executed as SQL, the users table would be gone
        // and this query would throw instead of returning a row.
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement()) {
 
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            assertTrue(rs.next(), "the users table must still exist after the injection attempt");
 
        }
    }
}