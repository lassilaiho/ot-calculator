package com.lassilaiho.calculator.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Implements {@link SessionDao} using a SQL database.
 */
public final class SqlSessionDao implements SessionDao {
    private SqlHistoryDao historyDao;
    private SqlNamedValueDao namedValueDao;

    /**
     * Constructs a new {@link SqlSessionDao} backed by a SQL database.
     * 
     * @param connection the database connection used for the operations
     */
    public SqlSessionDao(Connection connection) {
        historyDao = new SqlHistoryDao(connection);
        namedValueDao = new SqlNamedValueDao(connection);
    }

    /**
     * Initializes the database. For example, creates tables. It is safe to call this method multiple
     * times.
     * 
     * @throws SQLException thrown if the initialization fails
     */
    public void initializeDatabase() throws SQLException {
        historyDao.initializeDatabase();
        namedValueDao.initializeDatabase();
    }

    /**
     * Switches the database used to persist the session. The current session is copied to the new
     * database.
     * 
     * @param  newConnection the new database connection used for persistence
     * @throws SQLException  thrown if the operation fails
     */
    public void switchDatabase(Connection newConnection) throws SQLException {
        historyDao.switchDatabase(newConnection);
        namedValueDao.switchDatabase(newConnection);
    }

    @Override
    public HistoryDao history() {
        return historyDao;
    }

    @Override
    public NamedValueDao namedValues() {
        return namedValueDao;
    }
}
