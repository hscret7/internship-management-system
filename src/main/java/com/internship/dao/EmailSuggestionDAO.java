package com.internship.dao;

import java.sql.*;
import java.util.*;

/** Read-only, bounded email substring search. No profile or enrollment is created. */
public class EmailSuggestionDAO {
    public enum Scope { ENROLLMENT, APPLICATION, ACTOR }
    public record Suggestion(String email, String fullName) { }
    public List<Suggestion> search(Connection connection, Scope scope, String query) throws SQLException {
        String email = scope == Scope.ACTOR ? "u.email" : "COALESCE(u.email,ip.email)";
        String from = scope == Scope.ACTOR ? "users u" : "intern_profiles ip LEFT JOIN users u ON u.user_id=ip.user_id";
        String name = scope == Scope.ACTOR ? "u.full_name" : "ip.full_name";
        String eligible = switch (scope) {
            case ACTOR -> "";
            case ENROLLMENT -> " AND EXISTS (SELECT 1 FROM program_enrollments pe WHERE pe.intern_profile_id=ip.profile_id)";
            case APPLICATION -> """
                 AND EXISTS (SELECT 1 FROM applications a WHERE a.intern_id=ip.profile_id AND a.status='APPROVED'
                     AND NOT EXISTS (SELECT 1 FROM program_enrollments used WHERE used.application_id=a.application_id))
                 AND NOT EXISTS (SELECT 1 FROM program_enrollments active_enrollment WHERE active_enrollment.intern_profile_id=ip.profile_id AND active_enrollment.status='ACTIVE')
                """;
        };
        // LOCATE treats %, _ and backslashes as ordinary text, not LIKE wildcards.
        String sql = "SELECT DISTINCT TRIM(" + email + ") AS email," + name + " AS full_name FROM " + from
                + " WHERE LOCATE(LOWER(?),LOWER(TRIM(" + email + "))) > 0" + eligible + " ORDER BY email,full_name LIMIT 20";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, query);
            try (ResultSet rows = statement.executeQuery()) {
                List<Suggestion> result = new ArrayList<>();
                while (rows.next()) result.add(new Suggestion(rows.getString("email"),rows.getString("full_name")));
                return List.copyOf(result);
            }
        }
    }
}
