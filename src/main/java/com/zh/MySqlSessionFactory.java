package com.zh;

import java.lang.reflect.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhy
 * @version 1.0
 */
public class MySqlSessionFactory {

    Connection connection = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;

    /**
     * 获取方法返回值的泛型类型
     */
    public static Class<?> getMethodGenericReturnType(Method method) {
        // 获取泛型类型
        Type returnType = method.getGenericReturnType();

        if (returnType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) returnType;
            Type[] actualTypeArguments = type.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                Type actualType = actualTypeArguments[0];
                // 处理多层泛型
                if (actualType instanceof Class) {
                    return (Class<?>) actualType;
                } else if (actualType instanceof ParameterizedType) {
                    return (Class<?>) ((ParameterizedType) actualType).getRawType();
                }
            }
        }

        return null;
    }

    @SuppressWarnings("all")
    public <T> T getMapper(Class<T> mapper) {
        // 1、使用代理对象获取 mapper 对象,
        // 本代理对象需要传入三个参数，一个是类加载器，一个是获取哪些对象，一个是真正处理操作对象
        return (T) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{mapper},
                new MyInvocationHandler());
    }

    /**
     * 返回值类型赋值
     */
    private Object parseResultSet(ResultSet resultSet, Class<?> returnType) throws Exception {
        Constructor<?> constructor = returnType.getConstructor();
        Object result = constructor.newInstance();
        // 获取返回值对象的所有的字段
        Field[] fields = returnType.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String name = field.getName();
            Object o = null;
            // 判断字段的不同类型然后分别赋值  <这里没有完全写完整.>
            if (field.getType() == String.class) {
                o = resultSet.getString(name);
            } else if (field.getType() == Integer.class || field.getType() == int.class) {
                o = resultSet.getInt(name);
            } else if (field.getType() == LocalDate.class || field.getType() == Date.class) {
                o = resultSet.getDate(name);
            } else if (field.getType() == Long.class) {
                o = resultSet.getLong(name);
            }
            field.setAccessible(true);
            field.set(result, o);
        }
        return result;
    }

    /**
     * 参数的填充 id = ? 给问号部分填充指定类型的值
     */
    public void parameterHandler(PreparedStatement pstmt, int index, Object arg) throws Exception {
        // 使用instanceof链式判断
        if (arg instanceof String) {
            pstmt.setString(index, (String) arg);
        } else if (arg instanceof Integer) {
            pstmt.setInt(index, (Integer) arg);
        } else if (arg instanceof Long) {
            pstmt.setLong(index, (Long) arg);
        } else if (arg instanceof Double) {
            pstmt.setDouble(index, (Double) arg);
        } else if (arg instanceof Float) {
            pstmt.setFloat(index, (Float) arg);
        } else if (arg instanceof Short) {
            pstmt.setShort(index, (Short) arg);
        } else if (arg instanceof Byte) {
            pstmt.setByte(index, (Byte) arg);
        } else if (arg instanceof Boolean) {
            pstmt.setBoolean(index, (Boolean) arg);
        } else if (arg instanceof java.util.Date) {
            pstmt.setTimestamp(index, new Timestamp(((java.util.Date) arg).getTime()));
        } else if (arg instanceof LocalDate) {
            pstmt.setDate(index, java.sql.Date.valueOf((LocalDate) arg));
        } else if (arg instanceof LocalDateTime) {
            pstmt.setTimestamp(index, Timestamp.valueOf((LocalDateTime) arg));
        } else if (arg instanceof Object[]) {
            // 处理数组转JSON或特殊处理
            pstmt.setObject(index, arg);
        } else {
            // 默认处理
            pstmt.setObject(index, arg);
        }
    }

    @SuppressWarnings("all")
    class MyInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 2、判断对象中哪些东西需要代理（我们只需要对代理对象中的实现方法进行代理，例如：select,update,delete,insert 等方法进行代理）

            // 2.1. 过滤Object类的方法（避免代理toString、hashCode等）
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            String methodName = method.getName();
            // 3、判断是什么操作
            if (methodName.startsWith("select")) {
                System.out.println("执行查询操作");
                return invokeSelect(method, args);
            } else if (methodName.startsWith("update")) {
                System.out.println("执行更新操作");
                // return invokeUpdate(method, args);
            } else if (methodName.startsWith("delete")) {
                System.out.println("执行删除操作");
                // return invokeDelete(method, args);
            } else if (methodName.startsWith("insert")) {
                System.out.println("执行插入操作");
                return invokeInsert(method, args);
            } else {
                // 不是CRUD方法，调用真实对象的方法
                if (proxy != null) {
                    return method.invoke(proxy, args);
                }
                return null;
            }
            return null;
        }
        // 获取参数的非空值的字段值进行问号填充
        public static List<Object> getInsertNonNullValues(Method method, Object[] args) throws IllegalAccessException {
            Class<?> paramType = method.getParameters()[0].getType();
            List<Object> nonNullValues = new ArrayList<>();
            for (Field field : paramType.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(args[0]);
                if (value != null) {
                    nonNullValues.add(value);
                }
            }
            return nonNullValues;
        }

        // 查询操作
        private Object invokeSelect(Method method, Object[] args) throws Exception {
            try {
                // 0、判断返回值类型到底是什么？ 是实体类  还是 List？
                Class<?> returnType = getMethodGenericReturnType(method);
                if (returnType == null) {
                    returnType = method.getReturnType();
                }

                // 1、获取mysql连接
                connection = JdbcUtils.getConnection();

                // 2、生成sql
                String sql = SqlBuilderUtils.selectStatement(returnType, method, args);
                System.out.println("执行的SQL: " + sql);

                // 3、构造sql
                preparedStatement = connection.prepareStatement(sql);

                // 4、填充参数
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        parameterHandler(preparedStatement, i + 1, args[i]);
                    }
                }

                // 5、执行查询
                resultSet = preparedStatement.executeQuery();

                // 6、处理结果集
                if (method.getReturnType() == List.class) {
                    // 返回List集合
                    List<Object> resultList = new ArrayList<>();
                    while (resultSet.next()) {
                        Object entity = parseResultSet(resultSet, returnType);
                        resultList.add(entity);
                    }
                    return resultList;
                } else {
                    // 返回单个对象
                    if (resultSet.next()) {
                        return parseResultSet(resultSet, returnType);
                    }
                    return null;
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("查询执行失败: " + e.getMessage(), e);
            } finally {
                // 在这里统一关闭连接，无论是否发生异常
                JdbcUtils.close(resultSet, preparedStatement, connection);
            }
        }

        // 新增操作
        private Object invokeInsert(Method method, Object[] args) throws Exception {
            connection = JdbcUtils.getConnection();
            try {
                String sql = SqlBuilderUtils.insertStatement(method, args);
                System.out.println("执行的SQL: " + sql);

                preparedStatement = connection.prepareStatement(sql);

                // 获取非空字段值并填充参数
                List<Object> nonNullValues = getInsertNonNullValues(method, args);
                for (int i = 0; i < nonNullValues.size(); i++) {
                    parameterHandler(preparedStatement, i + 1, nonNullValues.get(i));
                }

                return preparedStatement.executeUpdate();
            } finally {
                JdbcUtils.close(resultSet, preparedStatement, connection);
            }
        }
    }
}
