# mybatis-zh
手动实现mybatis的动态代理。

一个轻量级的 MyBatis-like ORM 框架，采用动态代理模式实现 Mapper 接口，支持基于方法名约定的 CRUD 操作。

## 技术栈

- Java 8+
- JDBC (MySQL Connector 8.0.26)
- Maven
- JUnit 3.8.1

## 核心组件

| 组件 | 职责 |
|------|------|
| `MySqlSessionFactory` | 核心工厂类，负责创建 Mapper 代理对象 |
| `MyInvocationHandler` | 动态代理处理器，拦截 Mapper 方法调用并分发到对应的 CRUD 实现 |
| `SqlBuilderUtils` | SQL 语句构建器，根据方法签名和注解动态生成 SQL |
| `JdbcUtils` | JDBC 工具类，管理数据库连接和资源释放 |

## 注解

| 注解 | 作用目标 | 说明 |
|------|----------|------|
| `@Table` | 类 | 映射实体类对应的数据库表名 |
| `@TableId` | 字段 | 标记主键字段 |
| `@Param` | 方法参数 | 将参数映射为 SQL WHERE 条件中的列名 |

## 开发思路

### 1. 目标

模拟 MyBatis 的核心功能：通过接口定义 Mapper，基于方法名约定自动生成 SQL，通过动态代理执行数据库操作。

### 2. 核心设计：动态代理

```
UserMapper mapper = sessionFactory.getMapper(UserMapper.class);
User user = mapper.selectUserById(1);  // 实际执行的是代理逻辑
```

`getMapper()` 方法返回一个动态代理对象，当调用 `selectUserById()` 时：

1. 代理拦截方法调用
2. 根据方法名前缀（`select`/`insert`/`update`/`delete`）判断操作类型
3. 反射解析方法签名、注解、泛型返回类型
4. 动态构建并执行 SQL
5. 将 ResultSet 反向映射为实体对象

### 3. SQL 构建流程

**SELECT 示例：**
```java
User selectUserById(@Param("id") int id);
// -> SELECT id,name,age,birthday FROM user WHERE id = ?
```

1. 从方法返回值类型获取 `@Table` 获取表名
2. 从返回值类型的字段列表获取所有列名
3. 从方法参数获取 `@Param` 构建 WHERE 条件

**INSERT 示例：**
```java
int insertUser(User user);
// -> INSERT INTO user (name,age) VALUES (?,?)
```

1. 从方法参数类型的 `@Table` 获取表名
2. 遍历参数对象的所有字段，只收集 **非 null** 的字段
3. 字段名作为列名，`?` 作为占位符

### 4. ResultSet 到实体对象的反向映射

`parseResultSet()` 通过反射实现：

1. 通过无参构造创建实体对象
2. 遍历实体类的所有字段
3. 根据字段类型调用对应的 `resultSet.getXxx()` 方法
4. 通过 `field.setAccessible(true)` 设置访问权限，写入字段值

### 5. PreparedStatement 参数填充

`parameterHandler()` 使用 `instanceof` 链式判断，将 Java 对象类型映射到对应的 `pstmt.setXxx()` 方法。

## 关键代码片段

### getMapper — 创建 Mapper 代理

```java
public <T> T getMapper(Class<T> mapper) {
    return (T) Proxy.newProxyInstance(
        this.getClass().getClassLoader(),
        new Class[]{mapper},
        new MyInvocationHandler());
}
```

### invokeSelect — 查询执行流程

```java
// 1. 获取连接
connection = JdbcUtils.getConnection();

// 2. 构建 SQL
String sql = SqlBuilderUtils.selectStatement(returnType, method, args);

// 3. 填充参数
for (int i = 0; i < args.length; i++) {
    parameterHandler(preparedStatement, i + 1, args[i]);
}

// 4. 执行查询
resultSet = preparedStatement.executeQuery();

// 5. 处理结果集
while (resultSet.next()) {
    Object entity = parseResultSet(resultSet, returnType);
    resultList.add(entity);
}
```

### invokeInsert — 插入执行流程

```java
// 1. 构建 SQL（只包含非 null 字段）
String sql = SqlBuilderUtils.insertStatement(method, args);

// 2. 获取非空字段值
List<Object> nonNullValues = getInsertNonNullValues(method, args);

// 3. 填充参数
for (int i = 0; i < nonNullValues.size(); i++) {
    parameterHandler(preparedStatement, i + 1, nonNullValues.get(i));
}

// 4. 执行
return preparedStatement.executeUpdate();
```

## 当前实现进度

| 操作 | 状态 | 说明 |
|------|------|------|
| SELECT | ✅ 已完成 | 支持单对象和 List 返回，WHERE 条件基于 `@Param` |
| INSERT | ✅ 已完成 | 只插入非 null 字段，支持自增主键处理 |
| UPDATE | 🔜 待完成 | `invokeUpdate` 为空实现 |
| DELETE | 🔜 待完成 | `invokeDelete` 为空实现 |

## 待完善项

1. **UPDATE/DELETE** — 方法桩已写好但未实现逻辑
2. **泛型类型解析** — `getMethodGenericReturnType` 对多层泛型支持有限
3. **类型覆盖** — `parseResultSet` 和 `parameterHandler` 仅支持常见类型
4. **连接管理** — JDBC 配置硬编码在 `JdbcUtils` 中
5. **异常处理** — 统一异常体系替代直接 `throw Exception`

## 运行

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.zh.App"
```

## 数据库

~~~mysql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姓名',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '张三', 18, '2026-05-03');
INSERT INTO `user` VALUES (2, '李四', 22, '2026-05-03');
INSERT INTO `user` VALUES (3, '王五', 25, '2026-05-03');
INSERT INTO `user` VALUES (5, '测试新增', 65, '2026-05-04');
INSERT INTO `user` VALUES (6, '测试新增', 65, NULL);

SET FOREIGN_KEY_CHECKS = 1;
~~~

