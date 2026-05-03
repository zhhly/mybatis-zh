package com.zh;

import java.sql.*;

/**
 * @author zhy
 * @version 1.0
 */
public class JdbcUtils {

    static String JDBCURL = "jdbc:mysql://localhost:3306/test";
    static String USERNAME = "root";
    static String PASSWROD = "root";

    // 获取连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                JDBCURL,
                USERNAME,
                PASSWROD
        );
    }

    // 关闭资源
    public static void close(ResultSet rs, PreparedStatement stmt, Connection conn) {
        close(rs);
        close(stmt);
        close(conn);
    }

    public static void close(PreparedStatement stmt, Connection conn) {
        close(null, stmt, conn);
    }

    public static void close(ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static void close(PreparedStatement stmt) {
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static void close(Connection conn) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

}
