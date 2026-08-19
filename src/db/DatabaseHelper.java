package db;
import java.sql.*;

/**
 * Class contains all the CRUD (Create, Read, update, delete) methods for users, tasks, and preferences. 
 */

public class DatabaseHelper {

    private static final String URL = "jdbc:sqlite:imtps_db.db"; //Uses the SQLite JDBC driver to connect to the database file in the current working directory. 

    // Get a fresh connection. Caller is responsible for closing it. 

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // USERS
    
    //Note: the ? in the SQL statements are placeholders for parameters that will be set later using 
    // PreparedStatement. This helps prevent SQL injection attacks and allows for dynamic query building. 
    // i applied this method for all the other methods that update the database

    public static int addUser(String username, String email, String password) {
        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)"; 

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username); // The first ? will be replaced with the username
            pstmt.setString(2, email);   // The second ? will be replaced with the email
            pstmt.setString(3, password); // The third ? will be replaced with the password
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1); // newly created user_id
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // indicates failure to add user, for eg if the username already exists (due to unique constraint)
    }

    public static boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0; // returns true if count > 0
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // indicates user does not exist
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
                                String dueDate, String startTime) {
        String sql = "INSERT INTO tasks (user_id, task_name, category, importance, " +
                     "duration, due_date, start_time) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, taskName);
            pstmt.setInt(3, category);
            pstmt.setInt(4, importance);
            pstmt.setDouble(5, duration);
            pstmt.setString(6, dueDate);   //pass null if not set
            pstmt.setString(7, startTime); //pass null if not set
            pstmt.executeUpdate();         //executes the insert statement

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

    public static void updateTaskDueDate(int taskId, String newDueDate) {
        String sql = "UPDATE tasks SET due_date = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newDueDate);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskStartTime(int taskId, String newStartTime) {
        String sql = "UPDATE tasks SET start_time = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStartTime);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTaskEndTime(int taskId, String newEndTime) {
        String sql = "UPDATE tasks SET end_time = ? WHERE task_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newEndTime);
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

    // PREFERENCES

    public static void setPreferences(int userId, int peakProdTime, int dailyProdHours,
                                       boolean preferQuickFirst) {
        //Tries to update first. if no row exists yet, inserts one.
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


    //Simple test of the DatabaseHelper methods
   

    public static void main(String[] args) {
        int userId = addUser("nadim","nadim@example.com", "password_here");
        System.out.println("Created user with id: " + userId);

        addTask(userId, "Finish capstone report", 1, 5, 3.5, "2026-08-20 17:00:00", null);
        addTask(userId, "Gym session", 2, 2, 1.0, null, "2026-08-14 07:00:00");

        System.out.println("\nTasks for user " + userId + ":");
        printTasksForUser(userId);

        setPreferences(userId, 1, 6, true);
        System.out.println("\nPreferences for user " + userId + ":");
        printPreferences(userId);
    }
}