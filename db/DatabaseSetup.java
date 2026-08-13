Cimport java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseSetup {
    public static void main(String[] args) {
        // 1. Notice the URL changes: No database name is specified, and allowMultiQueries is set to true.
        String url = "jdbc:mysql://localhost:3306/?allowMultiQueries=true";
        String user = "your_username";
        String password = "your_password";

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();

            // 2. Your exact SQL script formatted as a Java String
            String sql = "CREATE DATABASE IF NOT EXISTS imtps_db;\n" +
                         "USE imtps_db;\n" +
                         "\n" +
                         "CREATE TABLE users (\n" +
                         "    user_id INT AUTO_INCREMENT PRIMARY KEY,\n" +
                         "    username VARCHAR(100) NOT NULL, \n" +
                         "    email VARCHAR(100) NOT NULL UNIQUE,\n" +
                         "    password_hash VARCHAR(255) NOT NULL\n" +
                         ");\n" +
                         "\n" +
                         "CREATE TABLE tasks (\n" +
                         "    task_id      INT AUTO_INCREMENT PRIMARY KEY,\n" +
                         "    user_id      INT NOT NULL,\n" +
                         "    task_name    VARCHAR(255) NOT NULL,\n" +
                         "    category     VARCHAR(100),\n" +
                         "    importance   INT CHECK (importance BETWEEN 1 AND 5),\n" +
                         "    duration     DECIMAL(5,2),\n" +
                         "    due_date     DATETIME NULL,\n" +
                         "    start_time   DATETIME NULL,\n" +
                         "    FOREIGN KEY (user_id) REFERENCES users(user_id)\n" +
                         ");\n" +
                         "\n" +
                         "CREATE TABLE preferences (\n" +
                         "    preference_id INT AUTO_INCREMENT PRIMARY KEY,\n" +
                         "    user_id       INT NOT NULL,\n" +
                         "    peak_prodtime  INT CHECK (peak_prodtime BETWEEN 1 AND 3),\n" +
                         "    daily_prod_hours    INT CHECK (daily_prod_hours BETWEEN 1 AND 10),\n" +
                         "    preferred_quick_first BOOLEAN,\n" +
                         "    FOREIGN KEY (user_id) REFERENCES users(user_id)\n" +
                         ");";

            // 3. Execute the full script at once
            stmt.executeUpdate(sql);
            System.out.println("Database and tables created successfully!");

            // 4. Clean up
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        }
    }
}