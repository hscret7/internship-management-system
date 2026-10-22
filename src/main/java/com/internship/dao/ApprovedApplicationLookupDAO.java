package com.internship.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Read-only choices for the existing approved-application enrollment workflow. */
public class ApprovedApplicationLookupDAO {
    public record Choice(int applicationId, String internName, String email) { }

    public List<Choice> find(String email, Integer applicationId, Connection connection) throws SQLException {
        if ((email == null) == (applicationId == null))
            throw new IllegalArgumentException("Specify exactly one application search key");
        String sql = """
                SELECT a.application_id, ip.full_name, COALESCE(u.email, ip.email) AS email
                FROM applications a
                JOIN intern_profiles ip ON ip.profile_id=a.intern_id
                LEFT JOIN users u ON u.user_id=ip.user_id
                WHERE a.status='APPROVED'
                  AND NOT EXISTS (SELECT 1 FROM program_enrollments used WHERE used.application_id=a.application_id)
                  AND NOT EXISTS (SELECT 1 FROM program_enrollments active_enrollment
                                  WHERE active_enrollment.intern_profile_id=ip.profile_id AND active_enrollment.status='ACTIVE')
                """ + (applicationId == null
                ? " AND (LOWER(TRIM(u.email))=LOWER(?) OR LOWER(TRIM(ip.email))=LOWER(?))"
                : " AND a.application_id=?") + " ORDER BY a.application_id DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (applicationId == null) { statement.setString(1,email);statement.setString(2,email); }
            else statement.setInt(1,applicationId);
            try (ResultSet rows=statement.executeQuery()) {
                List<Choice> result=new ArrayList<>();
                while(rows.next()) result.add(new Choice(rows.getInt("application_id"),rows.getString("full_name"),rows.getString("email")));
                return List.copyOf(result);
            }
        }
    }
}
