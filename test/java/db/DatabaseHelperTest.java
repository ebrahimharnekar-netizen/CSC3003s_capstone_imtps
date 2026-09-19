package db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import db.DatabaseHelper.Preferences;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatabaseHelperTest {

    private List<String> createdUserIds;
    private List<String> createdTaskIds;

    @BeforeAll
    static void setUpDatabase() {
        // Makes sure users/tasks/preferences tables exist before any test runs
        DatabaseSetup.main(new String[0]);
    }

    @BeforeEach
    void setUp() {
        createdUserIds = new ArrayList<>();
        createdTaskIds = new ArrayList<>();
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement()) {

            for (String id : createdUserIds) {
                // Delete child rows first to avoid foreign key violations
                stmt.executeUpdate("DELETE FROM tasks WHERE user_id = '" + id + "'");
                stmt.executeUpdate("DELETE FROM preferences WHERE user_id = '" + id + "'");
                stmt.executeUpdate("DELETE FROM users WHERE user_id = '" + id + "'");
            }

            // Clean up any tasks created in tests by task_id (independent of user)
            for (String taskId : createdTaskIds) {
                stmt.executeUpdate("DELETE FROM tasks WHERE task_id = '" + taskId + "'");
            }
        }
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    // ============================================================
    //  ORIGINAL TESTS
    // ============================================================

    @Test
    public void testOverlapLogicWithNullStart() {
        String mockUserId = "999";
        LocalDateTime nullStartTime = null;
        double duration = 2.0;

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

        String userId = DatabaseHelper.addUser(username, username + "@example.com", password);
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

        String userId = DatabaseHelper.addUser(username, username + "@example.com", "realPassword");
        createdUserIds.add(userId);

        assertFalse(DatabaseHelper.validateUser(username, "' OR '1'='1"),
                "a SQL injection payload in the password must not bypass login");
    }

    @Test
    void login_sqlInjectionCommentPayload_doesNotBypassAuthentication() {
        String username = uniqueUsername("login");

        String userId = DatabaseHelper.addUser(username, username + "@example.com", "realPassword");
        createdUserIds.add(userId);

        String injection = username + "' --";

        assertFalse(DatabaseHelper.validateUser(injection, "wrongOrAnyPassword"),
                "a comment-based injection payload must not bypass the password check");
    }

    @Test
    void signUp_sqlInjectionPayloadInUsername_isStoredLiterally_tableSurvives() throws SQLException {
        String maliciousUsername = "hacker'); DROP TABLE users; --";

        String userId = DatabaseHelper.addUser(maliciousUsername, "hacker@example.com", "pw");
        createdUserIds.add(userId);

        assertNotNull(userId, "sign-up should succeed, returning a valid UUID string");
        assertTrue(DatabaseHelper.userExists(maliciousUsername),
                "the malicious string should be stored (and matched) literally, not executed as SQL");

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            assertTrue(rs.next(), "the users table must still exist after the injection attempt");

        }
    }

    // ============================================================
    //  NEW: USER CRUD TESTS
    // ============================================================

    @Test
    void addUser_thenUserExists_returnsTrue() {
        String username = uniqueUsername("exists");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        assertNotNull(userId, "addUser should return a non-null userId");
        assertTrue(DatabaseHelper.userExists(username),
                "userExists should return true for a user that was just added");
    }

    @Test
    void userExists_unknownUser_returnsFalse() {
        assertFalse(DatabaseHelper.userExists("definitelyNotARealUser_xyz"),
                "userExists should return false for a username that was never added");
    }

    @Test
    void getUserId_returnsCorrectIdForExistingUser() {
        String username = uniqueUsername("getid");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        assertEquals(userId, DatabaseHelper.getUserId(username),
                "getUserId should return the exact userId assigned at sign-up");
    }

    @Test
    void getUserId_unknownUser_returnsNull() {
        assertNull(DatabaseHelper.getUserId("definitelyNotARealUser_xyz"),
                "getUserId should return null when the username does not exist");
    }

    @Test
    void validateUser_emptyPassword_returnsFalse() {
        String username = uniqueUsername("emptypw");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        assertFalse(DatabaseHelper.validateUser(username, ""),
                "empty password must not validate against a real password");
    }

    @Test
    void validateUser_caseSensitivePassword_failsOnWrongCase() {
        String username = uniqueUsername("casesens");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "PasswordCase");
        createdUserIds.add(userId);

        assertFalse(DatabaseHelper.validateUser(username, "passwordcase"),
                "password comparison must be case-sensitive");
    }

    // ============================================================
    //  NEW: PASSWORD HASHING TESTS
    // ============================================================

    @Test
    void addUser_passwordIsNotStoredInPlaintext() throws SQLException {
        String username = uniqueUsername("plaintext");
        String plainPassword = "SuperSecret123";
        String userId = DatabaseHelper.addUser(username, username + "@example.com", plainPassword);
        createdUserIds.add(userId);

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT password FROM users WHERE user_id = '" + userId + "'")) {

            assertTrue(rs.next(), "user row should exist");
            String stored = rs.getString("password");

            assertNotEquals(plainPassword, stored,
                    "the password column must NOT contain the plaintext password");
            assertTrue(stored.contains(":"),
                    "the stored password should be in the format 'base64(salt):base64(hash)'");
        }
    }

    // ============================================================
    //  NEW: TASK CRUD TESTS
    // ============================================================

    @Test
    void addTask_thenRetrieveByUser_containsTask() throws SQLException {
        String username = uniqueUsername("taskadd");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        DatabaseHelper.addTask(userId, "Study Algorithms", 1, 4, 2.0,
                LocalDateTime.now().plusDays(3), null, LocalDate.now());

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT task_name, category, importance, duration FROM tasks WHERE user_id = '" + userId + "'")) {

            assertTrue(rs.next(), "the task row should exist for the user");
            assertEquals("Study Algorithms", rs.getString("task_name"));
            assertEquals(1, rs.getInt("category"));
            assertEquals(4, rs.getInt("importance"));
            assertEquals(2.0, rs.getDouble("duration"), 0.001);
        }
    }

    @Test
    void updateTaskName_changesStoredValue() throws SQLException {
        String username = uniqueUsername("updatename");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        DatabaseHelper.addTask(userId, "Old Name", 1, 3, 1.0,
                LocalDateTime.now().plusDays(1), null, LocalDate.now());

        String taskId = getOnlyTaskIdForUser(userId);
        DatabaseHelper.updateTaskName(taskId, "New Name");

        assertEquals("New Name", getTaskName(taskId));
    }

    @Test
    void updateTaskImportance_changesStoredValue() throws SQLException {
        String username = uniqueUsername("updateimp");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        DatabaseHelper.addTask(userId, "Imp Task", 1, 2, 1.0,
                LocalDateTime.now().plusDays(1), null, LocalDate.now());

        String taskId = getOnlyTaskIdForUser(userId);
        DatabaseHelper.updateTaskImportance(taskId, 5);

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT importance FROM tasks WHERE task_id = '" + taskId + "'")) {

            assertTrue(rs.next());
            assertEquals(5, rs.getInt("importance"));
        }
    }

    @Test
    void markTaskCompleted_setsCompletedFlag() throws SQLException {
        String username = uniqueUsername("complete");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        DatabaseHelper.addTask(userId, "Task to Complete", 1, 3, 1.0,
                LocalDateTime.now().plusDays(1), null, LocalDate.now());

        String taskId = getOnlyTaskIdForUser(userId);
        DatabaseHelper.markTaskCompleted(taskId);

        assertTrue(isTaskCompleted(taskId));
    }

    @Test
    void restoreTask_clearsCompletedFlag() throws SQLException {
        String username = uniqueUsername("restore");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        DatabaseHelper.addTask(userId, "Task to Restore", 1, 3, 1.0,
                LocalDateTime.now().plusDays(1), null, LocalDate.now());

        String taskId = getOnlyTaskIdForUser(userId);
        DatabaseHelper.markTaskCompleted(taskId);
        DatabaseHelper.restoreTask(taskId);

        assertFalse(isTaskCompleted(taskId));
    }

    @Test
    void clearCompletedTasks_removesOnlyCompletedOnes() throws SQLException {
        String username = uniqueUsername("clearc");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        DatabaseHelper.addTask(userId, "Done 1", 1, 3, 1.0,
                LocalDateTime.now().plusDays(1), null, LocalDate.now());
        DatabaseHelper.addTask(userId, "Done 2", 1, 3, 1.0,
                LocalDateTime.now().plusDays(1), null, LocalDate.now());
        DatabaseHelper.addTask(userId, "Still Active", 1, 3, 1.0,
                LocalDateTime.now().plusDays(1), null, LocalDate.now());

        // Step 1: collect the task ids to complete - close the connection BEFORE
        // calling markTaskCompleted, otherwise SQLite will throw SQLITE_BUSY.
        List<String> idsToComplete = new ArrayList<>();

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT task_id, task_name FROM tasks WHERE user_id = '" + userId + "'")) {

            while (rs.next()) {
                String name = rs.getString("task_name");
                if (name.startsWith("Done")) {
                    idsToComplete.add(rs.getString("task_id"));
                }
            }
        }

        // Step 2: now that the connection is closed, mark them completed
        for (String taskId : idsToComplete) {
            DatabaseHelper.markTaskCompleted(taskId);
        }

        // Step 3: clear completed tasks
        DatabaseHelper.clearCompletedTasks(userId);

        // Step 4: verify only the non-completed task remains
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) AS c FROM tasks WHERE user_id = '" + userId + "'")) {

            assertTrue(rs.next());
            assertEquals(1, rs.getInt("c"),
                    "only the non-completed task should remain");
        }
    }

    @Test
    void deleteTask_removesRowEntirely() throws SQLException {
        String username = uniqueUsername("delete");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        DatabaseHelper.addTask(userId, "Delete Me", 1, 3, 1.0,
                LocalDateTime.now().plusDays(1), null, LocalDate.now());

        String taskId = getOnlyTaskIdForUser(userId);
        DatabaseHelper.deleteTask(taskId);

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) AS c FROM tasks WHERE task_id = '" + taskId + "'")) {

            assertTrue(rs.next());
            assertEquals(0, rs.getInt("c"));
        }
    }

    // ============================================================
    //  NEW: PREFERENCES TESTS
    // ============================================================

    @Test
    void setPreferences_thenGetPreferences_returnsSavedValues() {
        String username = uniqueUsername("prefs");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        DatabaseHelper.setPreferences(userId, 2, 6, true, "08:00", "18:00");

        Preferences prefs = DatabaseHelper.getPreferences(userId);
        assertNotNull(prefs, "getPreferences should return a Preferences object after a save");
        assertEquals(2, prefs.peakProdTime);
        assertEquals(6, prefs.dailyProdHours);
        assertTrue(prefs.preferQuickFirst);
        assertEquals("08:00", prefs.startTime);
        assertEquals("18:00", prefs.endTime);
    }

    @Test
    void setPreferences_calledTwice_updatesExistingRow() {
        String username = uniqueUsername("prefsupd");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        DatabaseHelper.setPreferences(userId, 1, 4, false, "07:00", "22:00");
        DatabaseHelper.setPreferences(userId, 3, 8, true, "09:00", "17:00");

        Preferences prefs = DatabaseHelper.getPreferences(userId);
        assertNotNull(prefs);
        assertEquals(3, prefs.peakProdTime,
                "second save should overwrite the first, not create a new row");
        assertEquals(8, prefs.dailyProdHours);
        assertTrue(prefs.preferQuickFirst);
        assertEquals("09:00", prefs.startTime);
        assertEquals("17:00", prefs.endTime);
    }

    @Test
    void getPreferences_userWithNoPreferences_returnsNull() {
        String username = uniqueUsername("noprefs");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        assertNull(DatabaseHelper.getPreferences(userId),
                "a user who never saved preferences should have no Preferences row");
    }

    // ============================================================
    //  NEW: TIME OVERLAP TESTS
    // ============================================================

    @Test
    void hasTimeOverlap_noExistingTasks_returnsFalse() {
        String username = uniqueUsername("overlapempty");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        boolean overlap = DatabaseHelper.hasTimeOverlap(
                userId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
                1.0, null);

        assertFalse(overlap, "no existing tasks means no overlap");
    }

    @Test
    void hasTimeOverlap_identicalSlot_returnsTrue() throws SQLException {
        String username = uniqueUsername("overlaptrue");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        LocalDate taskDate = LocalDate.now().plusDays(2);
        LocalDateTime start = taskDate.atTime(10, 0);
        DatabaseHelper.addTask(userId, "Pinned Task", 1, 3, 2.0,
                taskDate.atTime(23, 59), start, taskDate);

        // Same start time, same duration => definitely overlaps
        boolean overlap = DatabaseHelper.hasTimeOverlap(userId, start, 2.0, null);
        assertTrue(overlap, "two tasks occupying the exact same slot must overlap");
    }

    @Test
    void hasTimeOverlap_adjacentSlot_returnsFalse() throws SQLException {
        String username = uniqueUsername("overlapadj");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        LocalDate taskDate = LocalDate.now().plusDays(2);
        LocalDateTime existingStart = taskDate.atTime(10, 0); // 10:00 - 12:00
        DatabaseHelper.addTask(userId, "Morning Task", 1, 3, 2.0,
                taskDate.atTime(23, 59), existingStart, taskDate);

        // New task starts exactly at 12:00 (existing ends at 12:00) - should NOT overlap
        boolean overlap = DatabaseHelper.hasTimeOverlap(
                userId, taskDate.atTime(12, 0), 1.0, null);
        assertFalse(overlap, "back-to-back tasks (end == start) should not be treated as overlapping");
    }

    @Test
    void hasTimeOverlap_excludeTaskId_ignoresItself() throws SQLException {
        String username = uniqueUsername("overlapexcl");
        String userId = DatabaseHelper.addUser(username, username + "@example.com", "Password123");
        createdUserIds.add(userId);

        LocalDate taskDate = LocalDate.now().plusDays(2);
        LocalDateTime start = taskDate.atTime(14, 0);
        DatabaseHelper.addTask(userId, "Excluded", 1, 3, 1.0,
                taskDate.atTime(23, 59), start, taskDate);

        String taskId = getOnlyTaskIdForUser(userId);

        // Query for the same slot, but exclude this task's own id
        boolean overlap = DatabaseHelper.hasTimeOverlap(userId, start, 1.0, taskId);
        assertFalse(overlap,
                "a task should not overlap with itself when its own id is excluded");
    }

    // ============================================================
    //  PRIVATE HELPERS
    // ============================================================

    private String getOnlyTaskIdForUser(String userId) throws SQLException {
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT task_id FROM tasks WHERE user_id = '" + userId + "'")) {
            assertTrue(rs.next(), "expected at least one task for user " + userId);
            return rs.getString("task_id");
        }
    }

    private String getTaskName(String taskId) throws SQLException {
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT task_name FROM tasks WHERE task_id = '" + taskId + "'")) {
            assertTrue(rs.next());
            return rs.getString("task_name");
        }
    }

    private boolean isTaskCompleted(String taskId) throws SQLException {
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT completed FROM tasks WHERE task_id = '" + taskId + "'")) {
            assertTrue(rs.next());
            return rs.getInt("completed") == 1;
        }
    }
}