package com.aplana.iask.mus.test.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.aplana.iask.mus.test.Settings.*;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
public class JDBCConnector {

    private Connection connection;

    public Connection open() {
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.setProperty("jdbc.driver", get(DRIVER));

        try {
            connection = DriverManager.getConnection(
                    get(URL),
                    get(USERNAME),
                    get(PASSWORD)
            );
            connection.createStatement().execute("SET NOCOUNT ON;");
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось подключиться к базе", e);
        }

        return connection;
    }

}
