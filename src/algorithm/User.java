package algorithm;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/*
  User Represents an account in the users table (user_id, username, email,password_hash) and handles sign-up,
  login and logout for backlog item 2.
  Passwords are never stored in plain text - each password is hashed with PBKDF2WithHmacSHA1 using a random per-user salt,
  and the salt + hash are both stored together in the password_hash column.
 */
public class User {

    // PBKDF2 tuning
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 128;

    private String userId;
    private String username;
    private String email;
    private boolean loggedIn;

    private User(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.loggedIn = false;
    }

    // getters
    public String getUserId() {

        return userId;

    }

    public String getUsername() {

        return username;

    }

    public String getEmail() {

        return email;

    }

    public boolean isLoggedIn() {

        return loggedIn;

    }

    /*
      Signs up a new user: hashes the password, inserts the row into the
      users table, and returns the new User object.
      Returns null if the username/email is already taken.
     */
    public static User signUp(Connection conn, String username, String email, String password) throws SQLException {

        String passwordHash = hashPassword(password);

        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {

                if (keys.next()) {

                    String newUserId = keys.getString(1);
                    User newUser = new User(newUserId, username, email);
                    newUser.loggedIn = true; // signing up logs the user in straight away
                    return newUser;

                }

                return null;

            }

        } catch (SQLIntegrityConstraintViolationException e) {

            // username/email already exists,The caller decides how to inform the user
            return null;

        }

    }

    /*
      Logs a user in by username and password.
      Returns the User object on success, or null if the username doesn't
      exist or the password doesn't match.
     */
    public static User login(Connection conn, String username, String password) throws SQLException {

        String sql = "SELECT user_id, email, password_hash FROM users WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {

                    return null; // no such username

                }

                String storedHash = rs.getString("password_hash");

                if (!verifyPassword(password, storedHash)) {

                    return null; // wrong password

                }

                User user = new User(rs.getString("user_id"), username, rs.getString("email"));
                user.loggedIn = true;
                return user;

            }

        }

    }

    public void logout() {

        this.loggedIn = false;

    }

    // password hashing

    private static String hashPassword(String password) {

        byte[] salt = new byte[SALT_LENGTH_BYTES]; //salt is generated randomly for each user to prevent rainbow table attacks
                                                   // (users with same passwords are encrypted differently)
        new SecureRandom().nextBytes(salt);

        byte[] hash = pbkdf2(password, salt);

        
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);

    }

    private static boolean verifyPassword(String password, String storedHash) {

        String[] parts = storedHash.split(":");

        if (parts.length != 2) {

            return false;

        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

        byte[] actualHash = pbkdf2(password, salt);

        return java.security.MessageDigest.isEqual(expectedHash, actualHash);

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

}