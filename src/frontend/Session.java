package frontend;

/**
 * Session
 *
 * Holds the currently logged-in user's id/username so later screens (like
 * the onboarding questionnaire) know whose preferences to save, without
 * every FXMLLoader.load() call having to pass it around manually.
 * Set once in LoginController after a successful sign-in.
 */
public class Session {

    public static String currentUserId = "";
    public static String currentUsername;

}
