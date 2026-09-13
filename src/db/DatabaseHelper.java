package db;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Class contains all the CRUD (Create, Read, update, delete) methods and some other useful SQL queries for users, tasks, and preferences. 
 */
public class DatabaseHelper {

    private static final String URL = "jdbc:sqlite:imtps_db.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * Helper method to generate an ID consisting of the first 3 letters 
     * of the base string (capitalized) followed by a random 3-digit number.
     */
    private static String generateCustomId(String base) {
        String prefix;
        if (base == null || base.trim().isEmpty()) {
            prefix = "XXX";
        } else {
            // Remove spaces/special characters just in case, then take first 3 chars
            String cleanBase = base.trim().replaceAll("[^a-zA-Z0-9]", "");
            if (cleanBase.length() < 3) {
                prefix = (cleanBase + "XXX").substring(0, 3).toUpperCase();
            } else {
                prefix = cleanBase.substring(0, 3).toUpperCase();
            }
        }
        int randomDigits = (int) (Math.random() * 1000); // Generates 0 to 999
        return String.format("%s%03d", prefix, randomDigits);
    }

    // PASSWORD HASHING
    // Passwords are never stored in plain text - each one is hashed with
    // PBKDF2WithHmacSHA1 using a random per-user salt, so two users with the
    // same password get completely different stored values (and rainbow
    // tables don't work against them). The salt travels alongside the hash
    // in the same `password` column: "base64(salt):base64(hash)".

    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 128;

    private static String hashPassword(String password) {

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);

        byte[] hash = pbkdf2(password, salt);

        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);

    }

    private static boolean verifyPassword(String password, String storedValue) {

        if (storedValue == null) {

            return false;

        }

        String[] parts = storedValue.split(":");

        if (parts.length != 2) {

            // covers old plaintext rows from before hashing was added - they simply
            // won't verify anymore, rather than throwing, so login fails cleanly
            return false;

        }

        try {

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            byte[] actualHash = pbkdf2(password, salt);

            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (IllegalArgumentException notBase64) {

            return false;

        }

    }

    private static byte[] pbkdf2(String password, byte[] salt) {

        try {

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return factory.generateSecret(spec).getEncoded();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {

            throw new RuntimeException("Password hashing failed", e);

        }

    }

    // USERS
    
    public static String addUser(String username, String email, String password) {
        String userId = generateCustomId(username); // Generates custom ID
        String passwordHash = hashPassword(password);
        String sql = "INSERT INTO users (user_id, username, email, password) VALUES (?, ?, ?, ?)"; 

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, username);
            pstmt.setString(3, email);
            pstmt.setString(4, passwordHash);
            pstmt.executeUpdate();

            System.out.println("User added: " + username + " (ID: " + userId + ")");
            return userId;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean validateUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return verifyPassword(password, rs.getString("password"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getUserId(String username) {
        String sql = "SELECT user_id FROM users WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("user_id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void printAllUsers() {
        String sql = "SELECT * FROM users";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.println(rs.getString("user_id") + " | " +
                                    rs.getString("username"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TASKS

    public static void addTask(String userId, String taskName, int category,
                                int importance, double duration,
                                LocalDateTime dueDate, LocalDateTime startTime,
                                LocalDate startDate) {
        String taskId = generateCustomId(taskName); // Generates custom ID
        String sql = "INSERT INTO tasks (task_id, user_id, task_name, category, importance, " +
                     "duration, due_date, start_time, start_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, taskId);
            pstmt.setString(2, userId);
            pstmt.setString(3, taskName);
            pstmt.setInt(4, category);
            pstmt.setInt(5, importance);
            pstmt.setDouble(6, duration);
            pstmt.setObject(7, dueDate);
            pstmt.setObject(8, startTime);
            pstmt.setObject(9, startDate);
            pstmt.executeUpdate();

            System.out.println("Task added: " + taskName + " (ID: " + taskId + ")");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void printTasksForUser(String userId) {
        String sql = "SELECT * FROM tasks WHERE user_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString("task_id") + " | " +
                                    rs.getString("task_name") + " | " +
                                    rs.getString("category") + " | " +
                                    "importance=" + rs.getInt("importance") + " | " +
                                    "duration=" + rs.getDouble("duration") + "h | " +
                                    "due=" + rs.getString("due_date") + " | " +
                                    "start_time=" + rs.getString("start_time") + " | " +
                                    "start_date=" + rs.getString("start_date"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void getTasksForTheDay(String userId, String date) {
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND due_date = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, date);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString("task_id") + " | " +
                                    rs.getString("task_name") + " | " +
                                    rs.getString("category") + " | " +
                                    "importance=" + rs.getInt("importance") + " | " +
                                    "duration=" + rs.getDouble("duration") + "h | " +
                                    "due=" + rs.getString("due_date"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskImportance(String taskId, int newImportance) {
        String sql = "UPDATE tasks SET importance = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newImportance);
            pstmt.setString(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void updateTaskName(String taskId, String newTaskName) {
        String sql = "UPDATE tasks SET task_name = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newTaskName);
            pstmt.setString(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskCategory(String taskId, int newCategory) {
        String sql = "UPDATE tasks SET category = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newCategory);
            pstmt.setString(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskDuration(String taskId, double newDuration) {
        String sql = "UPDATE tasks SET duration = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newDuration);
            pstmt.setString(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskDueDate(String taskId, LocalDateTime newDueDate) {
        String sql = "UPDATE tasks SET due_date = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, newDueDate);
            pstmt.setString(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskStartTime(String taskId, LocalDateTime newStartTime) {
        String sql = "UPDATE tasks SET start_time = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, newStartTime);
            pstmt.setString(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskStartDate(String taskId, LocalDate newStartDate) {
        String sql = "UPDATE tasks SET start_date = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, newStartDate);
            pstmt.setString(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskEndTime(String taskId, LocalDateTime newEndTime) {
        String sql = "UPDATE tasks SET end_time = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, newEndTime);
            pstmt.setString(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteTask(String taskId) {
        String sql = "DELETE FROM tasks WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Marks a task as completed instead of deleting it, so it can still be
    // viewed/restored later. Used when the user ticks a task off in View tasks.
    public static void markTaskCompleted(String taskId) {
        String sql = "UPDATE tasks SET completed = 1 WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Moves a completed task back onto the active schedule.
    public static void restoreTask(String taskId) {
        String sql = "UPDATE tasks SET completed = 0 WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Permanently removes every completed task for a user. Only call this after
    // the user has explicitly confirmed - there's no undo.
    public static void clearCompletedTasks(String userId) {
        String sql = "DELETE FROM tasks WHERE user_id = ? AND completed = 1";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasTimeOverlap(String userId, LocalDateTime newStart, double newDuration, String excludeTaskId) {
        if (newStart == null) return false;

        LocalDateTime newEnd = newStart.plusMinutes((long) (newDuration * 60));

        String sql = "SELECT task_id, start_time, duration, due_date, start_date FROM tasks WHERE user_id = ? AND start_time IS NOT NULL";
        if (excludeTaskId != null && !excludeTaskId.isEmpty()) {
            sql += " AND task_id != ?";
        }

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            if (excludeTaskId != null && !excludeTaskId.isEmpty()) {
                pstmt.setString(2, excludeTaskId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Object startObj = rs.getObject("start_time");
                    if (startObj == null) continue;

                    LocalDateTime existingStart = (startObj instanceof LocalDateTime)
                            ? (LocalDateTime) startObj
                            : LocalDateTime.parse(startObj.toString().replace(" ", "T"));

                    Object startDateObj = rs.getObject("start_date");
                    LocalDate existingStartDate = null;
                    if (startDateObj != null) {
                        existingStartDate = (startDateObj instanceof LocalDate)
                                ? (LocalDate) startDateObj
                                : LocalDate.parse(startDateObj.toString());
                    }

                    Object dueObj = rs.getObject("due_date");
                    LocalDateTime existingDue = null;
                    if (dueObj != null) {
                        existingDue = (dueObj instanceof LocalDateTime)
                                ? (LocalDateTime) dueObj
                                : LocalDateTime.parse(dueObj.toString().replace(" ", "T"));
                    }

                    LocalDate rangeStart;
                    LocalDate rangeEnd;

                    if (existingStartDate != null && existingDue != null) {
                        rangeStart = existingStartDate;
                        rangeEnd = existingDue.toLocalDate();
                    } else {
                        rangeStart = existingStart.toLocalDate();
                        rangeEnd = rangeStart;
                    }

                    LocalDate newDate = newStart.toLocalDate();
                    if (newDate.isBefore(rangeStart) || newDate.isAfter(rangeEnd)) {
                        continue;
                    }

                    LocalDateTime existingStartOfDay = newDate.atTime(existingStart.toLocalTime());
                    double existingDur = rs.getDouble("duration");
                    LocalDateTime existingEnd = existingStartOfDay.plusMinutes((long) (existingDur * 60));

                    if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStartOfDay)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error checking overlap: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static void setPreferences(String userId, int peakProdTime, int dailyProdHours,
                                       boolean preferQuickFirst, String startTime, String endTime) {
        String prefId = generateCustomId("PREF"); // Generates custom ID starting with PRE
        String updateSql = "UPDATE preferences SET peak_prodtime = ?, daily_prod_hours = ?, " +
                            "preferred_quick_first = ?, start_time = ?, end_time = ? WHERE user_id = ?";
        String insertSql = "INSERT INTO preferences (preference_id, user_id, peak_prodtime, daily_prod_hours, " +
                            "preferred_quick_first, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect()) {

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, peakProdTime);
                pstmt.setInt(2, dailyProdHours);
                pstmt.setBoolean(3, preferQuickFirst);
                pstmt.setString(4, startTime);
                pstmt.setString(5, endTime);
                pstmt.setString(6, userId);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected == 0) {
                    try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                        insertPstmt.setString(1, prefId);
                        insertPstmt.setString(2, userId);
                        insertPstmt.setInt(3, peakProdTime);
                        insertPstmt.setInt(4, dailyProdHours);
                        insertPstmt.setBoolean(5, preferQuickFirst);
                        insertPstmt.setString(6, startTime);
                        insertPstmt.setString(7, endTime);
                        insertPstmt.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class Preferences {
        public final int peakProdTime;
        public final int dailyProdHours;
        public final boolean preferQuickFirst;
        public final String startTime;
        public final String endTime;

        public Preferences(int peakProdTime, int dailyProdHours, boolean preferQuickFirst, String startTime, String endTime) {
            this.peakProdTime = peakProdTime;
            this.dailyProdHours = dailyProdHours;
            this.preferQuickFirst = preferQuickFirst;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static Preferences getPreferences(String userId) {
        String sql = "SELECT peak_prodtime, daily_prod_hours, preferred_quick_first, start_time, end_time " +
                     "FROM preferences WHERE user_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Preferences(rs.getInt("peak_prodtime"),
                                        rs.getInt("daily_prod_hours"),
                                        rs.getBoolean("preferred_quick_first"),
                                        rs.getString("start_time"),
                                        rs.getString("end_time"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}