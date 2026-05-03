package com.zh;

import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhy
 * @version 1.0
 * @description: TODO
 * @date 2026/5/3 15:49
 */
public class SqlBuilderUtils {

    // 查询参数生成。目前只是简单的语句
    public static String selectStatement(Class<?> returnType, Method method, Object[] args) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        // 获取字段。method.getReturnType()方法的返回值类型。
        String cols = getCols(returnType);
        sql.append(cols);
        sql.append(" FROM ");
        // 数据表名（跟我们实体类一致）。
        String tableName = getTableName(returnType);
        sql.append(tableName);
        if (args != null) {
            sql.append(" WHERE ");
            // 查询条件, id = ? and name = ?
            String condition = getCondition(method);
            sql.append(condition);
        }
        return sql.toString();
    }

    // 查询参数
    private static String getCondition(Method method) {
        // 获取查询条件，首先获取方法的所有参数
        Parameter[] parameters = method.getParameters();
        // 这里我们用了一个在参数中生效的注解 @Param ，指定查询条件的字段是什么名称
        return Arrays.stream(parameters).map(parameter -> {
            Param param = parameter.getAnnotation(Param.class);
            return param.value() + " = ?";  // User selectUserById(@Param("id")int id); 直接获取@Param("id")中的 id
        }).collect(Collectors.joining(" and "));
    }

    // 获取数据表名
    private static String getTableName(Class<?> returnType) {
        // 这里我们获取实体类中的@Table注解中的值，表示我们指定的表名
        Table annotation = returnType.getAnnotation(Table.class);
        if (annotation == null) {
            throw new RuntimeException("未找到数据表名！");
        }
        return annotation.tableName();
    }

    // 获取返回值类型的所有字段（返回对象的字段）。这里只做了实体类的判断（String， Integer这种返回值还未做判断）
    private static String getCols(Class<?> returnType) {
        // returnType.getDeclaredFields()返回值对象的所有字段（属性）。
        List<String> list = Arrays.stream(returnType.getDeclaredFields()).map(Field::getName).toList();
        return String.join(",", list);
    }

    // 新增语句生成
    public static String insertStatement(Method method, Object[] args) throws IllegalAccessException {
        Class<?> paramType = method.getParameters()[0].getType();
        String tableName = getTableName(paramType);

        List<String> nonNullFields = new ArrayList<>();
        List<Object> nonNullValues = new ArrayList<>();
        for (Field field : paramType.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(args[0]);
            if (value != null) {
                nonNullFields.add(field.getName());
                nonNullValues.add(value);
            }
        }

        String columns = String.join(", ", nonNullFields);
        String placeholders = String.join(", ", nonNullFields.stream().map(f -> "?").toList());

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        // 构造包含字段名和字段值的结果对象返回
        return sql;
    }

}