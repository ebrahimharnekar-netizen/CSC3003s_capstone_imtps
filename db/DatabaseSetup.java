import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * This class sets up the SQLite database and creates the necessary tables for our IMTPS application.
 * It creates a database file named "imtps_db.db" in the current working directory if it doesn't already exist.
 * The tables created are:
 * users: to store user information
 * tasks: to store tasks associated with users
 * preferences: to store user preferences after answering the questionnaire
 */

public class DatabaseSetup {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:imtps_db.db";  //creates the file if it doesn't exist using the SQLite JDBC driver located in the directory.

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "username VARCHAR(100) NOT NULL, " +
                         "password VARCHAR(255) NOT NULL" +
                         ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                         "task_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "user_id INTEGER NOT NULL, " +
                         "task_name VARCHAR(255) NOT NULL, " +
                         "category VARCHAR(100), " +
                         "importance INTEGER CHECK (importance BETWEEN 1 AND 5), " +
                         "duration DECIMAL(5,2), " +
                         "due_date DATETIME NULL, " +
                         "start_time DATETIME NULL, " +
                         "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                         ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS preferences (" +
                         "preference_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "user_id INTEGER NOT NULL, " +
                         "peak_prodtime INTEGER CHECK (peak_prodtime BETWEEN 1 AND 3), " +
                         "daily_prod_hours INTEGER CHECK (daily_prod_hours BETWEEN 1 AND 10), " +
                         "preferred_quick_first BOOLEAN, " +
                         "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                         ");");

            System.out.println("Database and tables created successfully!");

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        }
    }
}