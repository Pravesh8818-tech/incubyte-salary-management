package com.incubyte.salarymanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SqliteConnectionTest {

    @Test
    public void testSqliteConnection() throws SQLException {
        // Load the SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }

        // Connect to an in-memory database for testing
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        assertNotNull(connection, "Connection should not be null");
        assertTrue(!connection.isClosed(), "Connection should be open");

        // Optionally, run a simple query to verify the database works
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS test (id INTEGER PRIMARY KEY, name TEXT)");
            statement.execute("INSERT INTO test (name) VALUES ('test')");
            var resultSet = statement.executeQuery("SELECT COUNT(*) FROM test");
            assertTrue(resultSet.next(), "Result set should have a row");
            int count = resultSet.getInt(1);
            assertTrue(count == 1, "Count should be 1");
        }

        connection.close();
    }
}
