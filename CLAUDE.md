# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Run tests
mvn test

# Compile
mvn compile

# Package
mvn package
```

## Architecture

This is a lightweight MyBatis-like ORM framework implementing the Mapper pattern.

### Core Components

- **MySqlSessionFactory** — Central factory that creates mapper proxies via `getMapper(Class<T> mapper)`. Uses Java's `Proxy.newProxyInstance()` to intercept method calls on mapper interfaces.
- **MyInvocationHandler** (inner class of MySqlSessionFactory) — Handles CRUD operations by method name prefix (`select*`, `insert*`, `update*`, `delete*`).
- **SqlBuilderUtils** — Builds SQL statements dynamically from method signatures and entity annotations.
- **JdbcUtils** — Manages MySQL connections via JDBC (hardcoded to `jdbc:mysql://localhost:3306/test` with `root/root`).

### Annotations

| Annotation | Target | Purpose |
|-----------|--------|---------|
| `@Table` | Class | Maps entity to table name |
| `@TableId` | Field | Marks primary key |
| `@Param` | Parameter | Maps method parameter to SQL column condition |

### Mapper Interface Pattern

Mapper interfaces (e.g., `UserMapper`) define query methods. Method naming convention drives SQL generation:
- `select*` → SELECT with WHERE clause from `@Param` annotations
- `insert*` → INSERT with non-null field values
- `update*` / `delete*` → Placeholder implementations

### Data Flow

1. `MySqlSessionFactory.getMapper()` returns a dynamic proxy
2. Method call is intercepted → `MyInvocationHandler.invoke()` checks method prefix
3. For `select*`: `SqlBuilderUtils.selectStatement()` builds SQL from `@Table`, `@Param`, entity fields
4. `parameterHandler()` binds args to PreparedStatement placeholders
5. `parseResultSet()` maps ResultSet back to entity objects

### Key Limitations

- Database credentials are hardcoded in `JdbcUtils.java`
- `update*` and `delete*` operations are stub implementations
- Type support in `parameterHandler()` and `parseResultSet()` is limited to common types
