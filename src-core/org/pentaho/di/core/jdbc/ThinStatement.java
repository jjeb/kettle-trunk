package org.pentaho.di.core.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public class ThinStatement implements Statement {

  private ThinConnection connection;
  private ThinResultSet resultSet;
  
  private int maxRows;
  
  public ThinStatement(ThinConnection connection, int resultSetType, int resultSetConcurrency) {
    this(connection);
  }

  public ThinStatement(ThinConnection connection, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    this(connection);
  }

  public ThinStatement(ThinConnection connection) {
    this.connection = connection;
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return null;
  }

  @Override
  public void addBatch(String arg0) throws SQLException {
  }

  @Override
  public void cancel() throws SQLException {
    if (resultSet!=null) {
      resultSet.cancel();
    }
  }

  @Override
  public void clearBatch() throws SQLException {
    throw new SQLException("Batch update statements are not supported by the thin Kettle JDBC driver");
  }

  @Override
  public void clearWarnings() throws SQLException {
  }

  @Override
  public void close() throws SQLException {
    // TODO

  }

  @Override
  public boolean execute(String sql) throws SQLException {
    return execute(sql, 0);
  }

  @Override
  public boolean execute(String sql, int arg1) throws SQLException {
    throw new SQLException("Executing statements is not supported by the thin Kettle JDBC driver");
  }

  @Override
  public boolean execute(String sql, int[] arg1) throws SQLException {
    return execute(sql);
  }

  @Override
  public boolean execute(String sql, String[] arg1) throws SQLException {
    return execute(sql);
  }

  @Override
  public int[] executeBatch() throws SQLException {
    throw new SQLException("Batch update statements are not supported by the thin Kettle JDBC driver");
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    resultSet = new ThinResultSet(this, connection.getSlaveBaseAddress()+"/sql/", connection.getUsername(), connection.getPassword(), sql);
    return resultSet;
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    throw new SQLException("The thin Kettle JDBC driver is read-only");
  }

  @Override
  public int executeUpdate(String sql, int arg1) throws SQLException {
    return executeUpdate(sql);
  }

  @Override
  public int executeUpdate(String sql, int[] arg1) throws SQLException {
    return executeUpdate(sql);
  }

  @Override
  public int executeUpdate(String sql, String[] arg1) throws SQLException {
    return executeUpdate(sql);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return connection;
  }

  @Override
  public int getFetchDirection() throws SQLException {
    return ResultSet.FETCH_FORWARD;
  }

  @Override
  public int getFetchSize() throws SQLException {
    return 1;
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    throw new SQLException("The thin Kettle JDBC driver is read-only");
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    return 0;
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    resultSet.close();
    return true;
  }

  @Override
  public boolean getMoreResults(int arg0) throws SQLException {
    resultSet.close();
    return true;
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    return 0;
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    return resultSet;
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    return resultSet.getConcurrency();
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    return resultSet.getHoldability();
  }

  @Override
  public int getResultSetType() throws SQLException {
    return resultSet.getType();
  }

  @Override
  public int getUpdateCount() throws SQLException {
    return 0;
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return null;
  }

  @Override
  public boolean isClosed() throws SQLException {
    return resultSet.isClosed();
  }

  @Override
  public boolean isPoolable() throws SQLException {
    return false;
  }

  @Override
  public void setCursorName(String arg0) throws SQLException {
  }

  @Override
  public void setEscapeProcessing(boolean arg0) throws SQLException {
  }

  @Override
  public void setFetchDirection(int arg0) throws SQLException {
  }

  @Override
  public void setFetchSize(int arg0) throws SQLException {
  }

  @Override
  public void setMaxFieldSize(int arg0) throws SQLException {
  }

  @Override
  public void setPoolable(boolean arg0) throws SQLException {
  }

  @Override
  public void setQueryTimeout(int arg0) throws SQLException {
  }

  /**
   * @return the maxRows
   */
  public int getMaxRows() {
    return maxRows;
  }

  /**
   * @param maxRows the maxRows to set
   */
  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
  }

}
