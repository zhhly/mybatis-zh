package com.zh;

import java.sql.*;

public class App {
    public static void main(String[] args) {

        MySqlSessionFactory mySqlSessionFactory = new MySqlSessionFactory();
        UserMapper mapper = mySqlSessionFactory.getMapper(UserMapper.class);

        // 查询
        System.out.println(mapper.selectUserById(2));
        System.out.println(mapper.selectUserByNameAndAge("张三", 18));

        // 新增
        User user = new User();
        user.setAge(65);
        // user.setBirthday(new Date(2025, 12, 13));
        user.setName("测试新增");
        mapper.insertUser(user);

        mapper.selectAllUser().stream().forEach(System.out::println);
        // System.out.println(getUser(1));

    }

    private static User getUser(int id){
        String JDBCURL = "jdbc:mysql://localhost:3306/test";
        String USERNAME = "root";
        String PASSWROD = "root";

        String sql = "select * from user where id = ?";

        try (Connection conn = DriverManager.getConnection(JDBCURL, USERNAME, PASSWROD);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setInt(1,id);
            ResultSet rs = preparedStatement.executeQuery();
            if(rs.next()){
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setAge(rs.getInt("age"));
                user.setBirthday(rs.getDate("birthday"));
                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
