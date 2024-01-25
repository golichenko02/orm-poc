package com.bobocode.utils;

import com.bobocode.EntityKey;
import com.bobocode.annotation.Column;
import com.bobocode.annotation.Id;
import com.bobocode.annotation.Table;
import com.bobocode.exception.DaoOperationException;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

@UtilityClass
public class SqlBuilder {
    private static final String SELECT_SQL = "SELECT * FROM %s WHERE %s = ?";
    private static final String SELECT_UUID_SQL = "SELECT * FROM %s WHERE %s = uuid(?)";
    private static final String UPDATE_SQL = "UPDATE %s %s WHERE %s = ?;";
    private static final String UPDATE_UUID_SQL = "UPDATE %s %s WHERE %s = uuid(?);";

    public <T> String prepareUpdateQuery(EntityKey<T> entityKey) {
        Class<T> entityClass = entityKey.getEntityClass();
        String setUpdatedColumnsSql = ReflectionUtils.getEntityColumnNamesWithoutId(entityClass).stream()
                .map("%s = ?"::formatted)
                .collect(Collectors.joining(", "));
        String tableName = ReflectionUtils.getTableName(entityClass);
        String primaryKeyName = ReflectionUtils.findPrimaryKeyName(entityClass);

        return ReflectionUtils.isUUIDField(entityClass, primaryKeyName) ?
                UPDATE_UUID_SQL.formatted(tableName, "SET %s".formatted(setUpdatedColumnsSql), primaryKeyName) :
                UPDATE_SQL.formatted(tableName, "SET %s".formatted(setUpdatedColumnsSql), primaryKeyName);
    }

    public <T> String prepareSelectQuery(Class<? extends T> entityClass) {
        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new RuntimeException();
        }
        String tableName = ReflectionUtils.getTableName(entityClass);
        String pk = ReflectionUtils.findPrimaryKeyName(entityClass);
        return ReflectionUtils.isUUIDField(entityClass, pk) ?
                SELECT_UUID_SQL.formatted(tableName, pk) :
                SELECT_SQL.formatted(tableName, pk);
    }

    public <T> void setParameters(PreparedStatement preparedStatement, Object pk, Object... parameters) throws SQLException {
        int i = 1;
        for (Object parameter : parameters) {
            preparedStatement.setObject(i++, parameter);
        }

        if (pk != null) {
            preparedStatement.setObject(i, pk);
        }
    }
}
