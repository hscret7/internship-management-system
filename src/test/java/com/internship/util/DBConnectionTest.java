package com.internship.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBConnectionTest {

    public static void main(String[] args) {

        String sql = """
                SELECT role_id, role_name, description
                FROM roles
                ORDER BY role_id
                """;

        try (
            Connection connection = DBConnection.getConnection();
            PreparedStatement statement =
                    connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {

            System.out.println("=== DATABASE CONNECTION SUCCESS ===");

            while (resultSet.next()) {
                System.out.println(
                        resultSet.getLong("role_id")
                        + " | "
                        + resultSet.getString("role_name")
                        + " | "
                        + resultSet.getString("description")
                );
            }

        } catch (Exception e) {
            System.err.println("=== DATABASE CONNECTION FAILED ===");
            e.printStackTrace();
        }
    }
}