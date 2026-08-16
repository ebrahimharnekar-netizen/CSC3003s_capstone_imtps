import java.sql.*;

/**
 * Centralized database access for the IMTPS capstone project.
 * Handles connections to the SQLite database and provides
 * CRUD methods for users, tasks, and preferences.
 */
public class DatabaseHelper {

    private static final String URL = "jdbc:sqlite:imtps_db.db";

    // Get a fresh connection. Caller is responsible for closing it
    // (use try-with-resources).
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // ---------------------------------------------------------
    // USERS
    // ---------------------------------------------------------

    public static int addUser(String username, String email, String passwordHash) {
        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, passwordHash);
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1); // newly created user_id
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
                                    rs.getString("username") + " | " +
                                    rs.getString("email"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------
    // TASKS
    // ---------------------------------------------------------

    public static void addTask(int userId, String taskName, String category,
                                int importance, double duration,
                                String dueDate, String startTime) {
        String sql = "INSERT INTO tasks (user_id, task_name, category, importance, " +
                     "duration, due_date, start_time) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, taskName);
            pstmt.setString(3, category);
            pstmt.setInt(4, importance);
            pstmt.setDouble(5, duration);
            pstmt.setString(6, dueDate);   // pass null if not set
            pstmt.setString(7, startTime); // pass null if not set
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

    // ---------------------------------------------------------
    // PREFERENCES
    // ---------------------------------------------------------

    public static void setPreferences(int userId, int peakProdTime, int dailyProdHours,
                                       boolean preferQuickFirst) {
        // Try update first; if no row exists yet, insert one.
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

    // ---------------------------------------------------------
    // Simple manual test
    // ---------------------------------------------------------

    public static void main(String[] args) {
        int userId = addUser("nadim", "nadim@example.com", "hashed_password_here");
        System.out.println("Created user with id: " + userId);

        addTask(userId, "Finish capstone report", "Work", 5, 3.5, "2026-08-20 17:00:00", null);
        addTask(userId, "Gym session", "Health", 2, 1.0, null, "2026-08-14 07:00:00");

        System.out.println("\nTasks for user " + userId + ":");
        printTasksForUser(userId);

        setPreferences(userId, 1, 6, true);
        System.out.println("\nPreferences for user " + userId + ":");
        printPreferences(userId);
    }
}