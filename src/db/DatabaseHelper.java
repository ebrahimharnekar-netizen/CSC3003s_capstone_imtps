package db;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Class contains all the CRUD (Create, Read, update, delete) methods and some other useful SQL queries for users, tasks, and preferences. 
 */

public class DatabaseHelper {

    private static final String URL = "jdbc:sqlite:imtps_db.db";

    // Get a fresh connection. Caller is responsible for closing it. 
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // USERS
    
    public static int addUser(String username, String email, String password) {
        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)"; 

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.executeUpdate();

            System.out.println("User added: " + username);

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
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
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getUserId(String username) {
        String sql = "SELECT user_id FROM users WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void printAllUsers() {
        String sql = "SELECT * FROM users";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.println(rs.getInt("user_id") + " | " +
                                    rs.getString("username"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TASKS

    public static void addTask(int userId, String taskName, int category,
                                int importance, double duration,
                                LocalDateTime dueDate, LocalDateTime startTime,
                                LocalDate startDate) {
        String sql = "INSERT INTO tasks (user_id, task_name, category, importance, " +
                     "duration, due_date, start_time, start_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, taskName);
            pstmt.setInt(3, category);
            pstmt.setInt(4, importance);
            pstmt.setDouble(5, duration);
            pstmt.setObject(6, dueDate);
            pstmt.setObject(7, startTime);
            pstmt.setObject(8, startDate);
            pstmt.executeUpdate();

            System.out.println("Task added: " + taskName);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void printTasksForUser(int userId) {
        String sql = "SELECT * FROM tasks WHERE user_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getInt("task_id") + " | " +
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

    public static void getTasksForTheDay(int userId, String date) {
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND due_date = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, date);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getInt("task_id") + " | " +
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

    public static void updateTaskImportance(int taskId, int newImportance) {
        String sql = "UPDATE tasks SET importance = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newImportance);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void updateTaskName(int taskId, String newTaskName) {
        String sql = "UPDATE tasks SET task_name = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newTaskName);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskCategory(int taskId, int newCategory) {
        String sql = "UPDATE tasks SET category = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newCategory);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskDuration(int taskId, double newDuration) {
        String sql = "UPDATE tasks SET duration = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newDuration);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskDueDate(int taskId, LocalDateTime newDueDate) {
        String sql = "UPDATE tasks SET due_date = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, newDueDate);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskStartTime(int taskId, LocalDateTime newStartTime) {
        String sql = "UPDATE tasks SET start_time = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, newStartTime);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskStartDate(int taskId, LocalDate newStartDate) {
        String sql = "UPDATE tasks SET start_date = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, newStartDate);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskEndTime(int taskId, LocalDateTime newEndTime) {
        String sql = "UPDATE tasks SET end_time = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, newEndTime);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteTask(int taskId) {
        String sql = "DELETE FROM tasks WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ... (rest of the class remains the same)

    /**
     * Checks if the given time slot overlaps with any existing pinned task for the user.
     * A pinned task with both a startDate and a dueDate will block that time on every day
     * in that date range. Otherwise, it only blocks the day of its startTime.
     *
     * @param userId         the ID of the user
     * @param newStart       the proposed start time (must not be null to check)
     * @param newDuration    the duration in hours
     * @param excludeTaskId  the ID of a task to ignore; pass -1 or 0 to ignore none
     * @return true if there is an overlap with any other task, false otherwise
     */
    public static boolean hasTimeOverlap(int userId, LocalDateTime newStart, double newDuration, int excludeTaskId) {
        if (newStart == null) return false;

        LocalDateTime newEnd = newStart.plusMinutes((long) (newDuration * 60));

        String sql = "SELECT task_id, start_time, duration, due_date, start_date FROM tasks WHERE user_id = ? AND start_time IS NOT NULL";
        if (excludeTaskId > 0) {
            sql += " AND task_id != ?";
        }

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            if (excludeTaskId > 0) {
                pstmt.setInt(2, excludeTaskId);
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
    // PREFERENCES

    public static void setPreferences(int userId, int peakProdTime, int dailyProdHours,
                                       boolean preferQuickFirst) {
        String updateSql = "UPDATE preferences SET peak_prodtime = ?, daily_prod_hours = ?, " +
                            "preferred_quick_first = ? WHERE user_id = ?";
        String insertSql = "INSERT INTO preferences (user_id, peak_prodtime, daily_prod_hours, " +
                            "preferred_quick_first) VALUES (?, ?, ?, ?)";

        try (Connection conn = connect()) {

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, peakProdTime);
                pstmt.setInt(2, dailyProdHours);
                pstmt.setBoolean(3, preferQuickFirst);
                pstmt.setInt(4, userId);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected == 0) {
                    try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                        insertPstmt.setInt(1, userId);
                        insertPstmt.setInt(2, peakProdTime);
                        insertPstmt.setInt(3, dailyProdHours);
                        insertPstmt.setBoolean(4, preferQuickFirst);
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

        public Preferences(int peakProdTime, int dailyProdHours, boolean preferQuickFirst) {
            this.peakProdTime = peakProdTime;
            this.dailyProdHours = dailyProdHours;
            this.preferQuickFirst = preferQuickFirst;
        }
    }

    public static Preferences getPreferences(int userId) {
        String sql = "SELECT peak_prodtime, daily_prod_hours, preferred_quick_first " +
                     "FROM preferences WHERE user_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Preferences(rs.getInt("peak_prodtime"),
                                        rs.getInt("daily_prod_hours"),
                                        rs.getBoolean("preferred_quick_first"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void printPreferences(int userId) {
        String sql = "SELECT * FROM preferences WHERE user_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("peak_prodtime=" + rs.getInt("peak_prodtime") +
                                    " | daily_prod_hours=" + rs.getInt("daily_prod_hours") +
                                    " | preferred_quick_first=" + rs.getBoolean("preferred_quick_first"));
            } else {
                System.out.println("No preferences set for user " + userId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int userId = addUser("nadim","nadim@example.com", "password_here");
        System.out.println("Created user with id: " + userId);

        addTask(userId, "Finish capstone report", 1, 5, 3.5, 
                LocalDateTime.of(2026, 8, 20, 17, 0), null, null);
        addTask(userId, "Gym session", 2, 2, 1.0, null, 
                LocalDateTime.of(2026, 8, 14, 7, 0), null);

        System.out.println("\nTasks for user " + userId + ":");
        printTasksForUser(userId);

        setPreferences(userId, 1, 6, true);
        System.out.println("\nPreferences for user " + userId + ":");
        printPreferences(userId);
    }
}