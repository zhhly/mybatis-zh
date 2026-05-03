package com.zh;

import java.util.List;

/**
 * @author zhy
 * @version 1.0
 */
public interface UserMapper {

    User selectUserById(@Param("id")int id);

    User selectUserByNameAndAge(@Param("name")String name, @Param("age")int age);

    List<User> selectAllUser();

    int insertUser(User user);

    int updateUser(User user);

    int deleteUserById(@Param("id")int id);

}
