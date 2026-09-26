package com.internship.dao;

import com.internship.model.Role;

import java.util.List;

public class RoleDAOTest {

    public static void main(String[] args) {
        RoleDAO roleDAO = new RoleDAO();

        try {
            System.out.println("=== FIND ALL ROLES ===");

            List<Role> roles = roleDAO.findAll();

            for (Role role : roles) {
                System.out.println(
                        role.getRoleId()
                                + " | "
                                + role.getRoleName()
                                + " | "
                                + role.getDescription()
                );
            }

            System.out.println();
            System.out.println("=== FIND ROLE BY NAME ===");

            Role internRole = roleDAO.findByName("INTERN");

            if (internRole != null) {
                System.out.println("Found: " + internRole);
            } else {
                System.out.println("INTERN role not found.");
            }

            System.out.println();
            System.out.println("=== FIND ROLE BY ID ===");

            Role role = roleDAO.findById(1);

            if (role != null) {
                System.out.println("Found: " + role);
            } else {
                System.out.println("Role ID 1 not found.");
            }

            System.out.println();
            System.out.println("=== ROLE DAO TEST SUCCESS ===");

        } catch (Exception e) {
            System.err.println("=== ROLE DAO TEST FAILED ===");
            e.printStackTrace();
        }
    }
}