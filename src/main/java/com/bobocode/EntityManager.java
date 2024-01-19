package com.bobocode;

import com.bobocode.annotation.Column;
import com.bobocode.annotation.Id;
import com.bobocode.annotation.Table;
import com.bobocode.exception.DaoOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class EntityManager {
    private static final String SELECT_SQL = "SELECT * FROM %s WHERE %s = ?";
    private static final String SELECT_UUID_SQL = "SELECT * FROM %s WHERE %s = uuid(?)";

    private final DataSource dataSource;

    public <T> T find(Class<? extends T> entityClass, Object primaryKey) {
        return findEntity(entityClass, primaryKey);
    }

    private <T> T findEntity(Class<? extends T> entityClass, Object primaryKey) {
        String sql = prepareSqlQuery(entityClass, primaryKey);
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setObject(1, primaryKey);
            ResultSet resultSet = preparedStatement.executeQuery();
            return parseResultSet(entityClass, resultSet);
        } catch (SQLException e) {
            throw new DaoOperationException("Failed to retrieve entity by id: %s".formatted(primaryKey), e);
        }
    }

    private <T> T parseResultSet(Class<? extends T> entityClass, ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            throw new DaoOperationException("No data found");
        }

        checkDefaultConstructor(entityClass);
        T entity = createEntity(entityClass);

        Map<String, Field> fieldByColumnName = getEntityColumns(entityClass);
        for (Map.Entry<String, Field> fieldByColumnNameEntry : fieldByColumnName.entrySet()) {
            String columnName = fieldByColumnNameEntry.getKey();
            Field field = fieldByColumnNameEntry.getValue();
            Object value = resultSet.getObject(columnName);
            setFieldValue(entity, field, value);
        }

        return entity;
    }

    private static <T> void setFieldValue(T entity, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new DaoOperationException("Failed to set value [%s] for entity column [%s]".formatted(value, field.getName()), e);
        }
    }

    private <T> Map<String, Field> getEntityColumns(Class<? extends T> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .collect(Collectors.toMap(field -> field.getAnnotation(Column.class).name(), Function.identity()));
    }

    private <T> T createEntity(Class<? extends T> entityClass) {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new DaoOperationException("No default constructor found for entity %s".formatted(entityClass.getSimpleName()));
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new DaoOperationException("Failed to instantiate entity %s".formatted(entityClass.getSimpleName()));
        }
    }

    private <T> void checkDefaultConstructor(Class<? extends T> entityClass) {
        boolean isDefaultConstructorExists = Arrays.stream(entityClass.getDeclaredConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == 0);

        if (!isDefaultConstructorExists) {
            throw new DaoOperationException("Could instantiate entity %s. No default constructor declared".formatted(entityClass.getSimpleName()));
        }
    }

    private <T> String prepareSqlQuery(Class<? extends T> entityClass, Object primaryKey) {
        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new RuntimeException();
        }
        String tableName = entityClass.getAnnotation(Table.class).name();
        String pk = findPrimaryKeyName(entityClass);
        return UUID.class.isAssignableFrom(getFieldType(entityClass, pk)) ?
                SELECT_UUID_SQL.formatted(tableName, pk) :
                SELECT_SQL.formatted(tableName, pk);
    }

    private static <T> Class<?> getFieldType(Class<? extends T> entityClass, String fieldName) {
        try {
            return entityClass.getDeclaredField(fieldName).getType();
        } catch (NoSuchFieldException e) {
            throw new DaoOperationException("Failed to get field by name [%s]".formatted(fieldName), e);
        }
    }

    private <T> String findPrimaryKeyName(Class<? extends T> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class) && field.isAnnotationPresent(Column.class))
                .findFirst()
                .map(field -> field.getAnnotation(Column.class).name())
                .orElseThrow(() -> new DaoOperationException("Primary key column is not specified"));
    }

    private Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DaoOperationException("Failed to acquire connection", e);
        }
    }

}
