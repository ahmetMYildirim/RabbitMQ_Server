package org.example.SQlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class sqconnection {
    private static final String DB_URL = "jdbc:sqlite:rabbitmq.db";

    public static Connection connection() {
        try{
            return DriverManager.getConnection(DB_URL);
        }catch (SQLException e) {
            System.out.println("Bağlantı koptu...");
            return null;
        }
    }
}
