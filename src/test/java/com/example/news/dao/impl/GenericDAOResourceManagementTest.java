package com.example.news.dao.impl;

import com.example.news.exception.DatabaseException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenericDAOResourceManagementTest {

    @Test
    public void readOperationsShouldAlwaysCloseTheirJdbcResources() {
        assertReadResourcesClosed(dao -> dao.query("SELECT value", resultSet -> resultSet.getLong(1)));
        assertReadResourcesClosed(dao -> dao.queryOne("SELECT value", resultSet -> resultSet.getLong(1)));
        assertReadResourcesClosed(dao -> dao.count("SELECT COUNT(*)"));
    }

    @Test
    public void writeOperationsShouldAlwaysCloseTheirJdbcResources() {
        TrackingJdbc insertJdbc = new TrackingJdbc();
        GenericDAO insertDAO = new GenericDAO(insertJdbc::openConnection);
        assertEquals(7L, insertDAO.insert("INSERT INTO sample(value) VALUES (?)", "value"));
        assertEquals(1, insertJdbc.connectionCloseCount);
        assertEquals(1, insertJdbc.statementCloseCount);
        assertEquals(1, insertJdbc.resultSetCloseCount);

        TrackingJdbc updateJdbc = new TrackingJdbc();
        GenericDAO updateDAO = new GenericDAO(updateJdbc::openConnection);
        assertEquals(1, updateDAO.update("UPDATE sample SET value = ?", "value"));
        assertWriteResourcesClosed(updateJdbc);

        TrackingJdbc deleteJdbc = new TrackingJdbc();
        GenericDAO deleteDAO = new GenericDAO(deleteJdbc::openConnection);
        assertEquals(1, deleteDAO.delete("DELETE FROM sample WHERE id = ?", 1L));
        assertWriteResourcesClosed(deleteJdbc);
    }

    @Test
    public void queryShouldCloseResourcesWhenMappingFails() {
        TrackingJdbc jdbc = new TrackingJdbc();
        GenericDAO dao = new GenericDAO(jdbc::openConnection);

        assertThrows(DatabaseException.class,
                () -> dao.query("SELECT value", resultSet -> {
                    throw new SQLException("mapping failed");
                }));

        assertEquals(1, jdbc.connectionCloseCount);
        assertEquals(1, jdbc.statementCloseCount);
        assertEquals(1, jdbc.resultSetCloseCount);
    }

    @Test
    public void updateShouldCloseResourcesWhenExecutionFails() {
        TrackingJdbc jdbc = new TrackingJdbc();
        jdbc.failOnExecute = true;
        GenericDAO dao = new GenericDAO(jdbc::openConnection);

        assertThrows(DatabaseException.class,
                () -> dao.update("UPDATE sample SET value = ?", "value"));

        assertEquals(1, jdbc.connectionCloseCount);
        assertEquals(1, jdbc.statementCloseCount);
        assertEquals(0, jdbc.resultSetCloseCount);
    }

    @Test
    public void transactionShouldUseOneConnectionCommitAndCloseAllResources() {
        TrackingJdbc jdbc = new TrackingJdbc();
        GenericDAO dao = new GenericDAO(jdbc::openConnection);

        long generatedId = dao.executeInTransaction(transactionDAO -> {
            long id = transactionDAO.insert("INSERT INTO sample(value) VALUES (?)", "value");
            transactionDAO.update("UPDATE sample SET value = ? WHERE id = ?", "updated", id);
            return id;
        });

        assertEquals(7L, generatedId);
        assertEquals(1, jdbc.connectionOpenCount);
        assertEquals(1, jdbc.connectionCloseCount);
        assertEquals(2, jdbc.statementCloseCount);
        assertEquals(1, jdbc.resultSetCloseCount);
        assertTrue(jdbc.committed);
        assertFalse(jdbc.rolledBack);
    }

    @Test
    public void transactionShouldRollbackAndCloseResourcesWhenOperationFails() {
        TrackingJdbc jdbc = new TrackingJdbc();
        GenericDAO dao = new GenericDAO(jdbc::openConnection);

        assertThrows(DatabaseException.class, () -> dao.executeInTransaction(transactionDAO -> {
            transactionDAO.update("UPDATE sample SET value = ?", "value");
            throw new DatabaseException("force rollback");
        }));

        assertEquals(1, jdbc.connectionOpenCount);
        assertEquals(1, jdbc.connectionCloseCount);
        assertEquals(1, jdbc.statementCloseCount);
        assertTrue(jdbc.rolledBack);
        assertFalse(jdbc.committed);
    }

    private void assertReadResourcesClosed(DAOCall operation) {
        TrackingJdbc jdbc = new TrackingJdbc();
        GenericDAO dao = new GenericDAO(jdbc::openConnection);

        operation.execute(dao);

        assertEquals(1, jdbc.connectionOpenCount);
        assertEquals(1, jdbc.connectionCloseCount);
        assertEquals(1, jdbc.statementCloseCount);
        assertEquals(1, jdbc.resultSetCloseCount);
    }

    private void assertWriteResourcesClosed(TrackingJdbc jdbc) {
        assertEquals(1, jdbc.connectionOpenCount);
        assertEquals(1, jdbc.connectionCloseCount);
        assertEquals(1, jdbc.statementCloseCount);
        assertEquals(0, jdbc.resultSetCloseCount);
    }

    @FunctionalInterface
    private interface DAOCall {

        void execute(GenericDAO dao);
    }

    private static final class TrackingJdbc {

        private int connectionOpenCount;
        private int connectionCloseCount;
        private int statementCloseCount;
        private int resultSetCloseCount;
        private boolean failOnExecute;
        private boolean committed;
        private boolean rolledBack;
        private boolean connectionClosed;

        private Connection openConnection() {
            connectionOpenCount++;
            connectionClosed = false;
            InvocationHandler handler = (proxy, method, args) -> handleConnection(method, args);
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{Connection.class}, handler);
        }

        private Object handleConnection(Method method, Object[] args) throws SQLException {
            String name = method.getName();
            if ("prepareStatement".equals(name)) {
                return createStatement();
            }
            if ("setAutoCommit".equals(name)) {
                return null;
            }
            if ("getAutoCommit".equals(name)) {
                return true;
            }
            if ("commit".equals(name)) {
                committed = true;
                return null;
            }
            if ("rollback".equals(name)) {
                rolledBack = true;
                return null;
            }
            if ("close".equals(name)) {
                if (!connectionClosed) {
                    connectionClosed = true;
                    connectionCloseCount++;
                }
                return null;
            }
            if ("isClosed".equals(name)) {
                return connectionClosed;
            }
            return defaultValue(method.getReturnType());
        }

        private PreparedStatement createStatement() {
            InvocationHandler handler = new InvocationHandler() {
                private boolean closed;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
                    String name = method.getName();
                    if ("executeQuery".equals(name)) {
                        failIfRequested();
                        return createResultSet();
                    }
                    if ("executeUpdate".equals(name)) {
                        failIfRequested();
                        return 1;
                    }
                    if ("getGeneratedKeys".equals(name)) {
                        return createResultSet();
                    }
                    if ("close".equals(name)) {
                        if (!closed) {
                            closed = true;
                            statementCloseCount++;
                        }
                        return null;
                    }
                    if ("isClosed".equals(name)) {
                        return closed;
                    }
                    return defaultValue(method.getReturnType());
                }
            };
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{PreparedStatement.class}, handler);
        }

        private ResultSet createResultSet() {
            InvocationHandler handler = new InvocationHandler() {
                private boolean firstRow = true;
                private boolean closed;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("next".equals(name)) {
                        if (firstRow) {
                            firstRow = false;
                            return true;
                        }
                        return false;
                    }
                    if ("getLong".equals(name)) {
                        return 7L;
                    }
                    if ("close".equals(name)) {
                        if (!closed) {
                            closed = true;
                            resultSetCloseCount++;
                        }
                        return null;
                    }
                    if ("isClosed".equals(name)) {
                        return closed;
                    }
                    return defaultValue(method.getReturnType());
                }
            };
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{ResultSet.class}, handler);
        }

        private void failIfRequested() throws SQLException {
            if (failOnExecute) {
                throw new SQLException("execution failed");
            }
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }
}
