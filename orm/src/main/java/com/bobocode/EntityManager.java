package com.bobocode;

import com.bobocode.annotation.Column;
import com.bobocode.exception.DaoOperationException;
import com.bobocode.utils.ReflectionUtils;
import com.bobocode.utils.SqlBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class EntityManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(EntityManager.class);

    private final DataSource dataSource;
    private final Map<EntityKey<?>, Object> entityByEntityKeyCache = new ConcurrentHashMap<>();
    private final Map<EntityKey<?>, Object[]> entityInitialStateByEntityKey = new ConcurrentHashMap<>();

    public <T> T find(Class<? extends T> entityClass, Object primaryKey) {
        EntityKey<? extends T> entityKey = new EntityKey<>(entityClass, primaryKey);
        return entityClass.cast(entityByEntityKeyCache.computeIfAbsent(entityKey, this::findEntity));
    }

    private <T> T findEntity(EntityKey<? extends T> entityKey) {
        log.trace("Not found entity in the cache by key [{}]", entityKey);
        Class<? extends T> entityClass = entityKey.getEntityClass();
        Object primaryKey = entityKey.getId();
        String sql = SqlBuilder.prepareSelectQuery(entityClass);
        log.trace("Query: [{}]", sql);
        T result = executeQuery(entityKey, sql, primaryKey);
        saveEntitySnapshot(entityKey, result);
        return result;
    }

    private <T> void saveEntitySnapshot(EntityKey<? extends T> entityKey, T entity) {
        Object[] values = ReflectionUtils.getEntityColumnValues(entity);
        entityInitialStateByEntityKey.put(entityKey, values);
    }

    @Override
    public void close() {
        executeDirtyCheck();
        entityByEntityKeyCache.clear();
        entityInitialStateByEntityKey.clear();
    }

    private void executeDirtyCheck() {
        entityByEntityKeyCache.keySet().stream()
                .filter(this::hasChanged)
                .forEach(this::flushChanges);
    }

    private <T> boolean hasChanged(EntityKey<T> entityKey) {
        Object[] currentEntityState = ReflectionUtils.getEntityColumnValues(entityByEntityKeyCache.get(entityKey));
        Object[] initialEntityState = entityInitialStateByEntityKey.get(entityKey);
        return !Arrays.equals(currentEntityState, initialEntityState);
    }

    private <T> void flushChanges(EntityKey<T> entityKey) {
        log.trace("Found not flushed changes in the cache");
        Class<T> entityClass = entityKey.getEntityClass();
        T updatedEntity = entityClass.cast(entityByEntityKeyCache.get(entityKey));
        String updateSql = SqlBuilder.prepareUpdateQuery(entityKey);
        log.trace("Update entity: [{}]", updateSql);
        executeUpdateQuery(updateSql, entityKey.getId(), ReflectionUtils.getEntityColumnValuesWithoutId(updatedEntity));
    }

    private <T> T executeQuery(EntityKey<T> entityKey, String sql, Object... parameters) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            SqlBuilder.setParameters(preparedStatement, entityKey.getId());
            return parseResultSet(entityKey, preparedStatement.executeQuery());
        } catch (SQLException e) {
            throw new DaoOperationException("Failed to execute query: [%s] with parameters [%s]"
                    .formatted(sql, Arrays.toString(parameters)), e);
        }
    }

    private long executeUpdateQuery(String sql, Object pk, Object... parameters) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            SqlBuilder.setParameters(preparedStatement, pk, parameters);
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DaoOperationException("Failed to execute update query: [%s] with parameters %s"
                    .formatted(sql, Arrays.toString(parameters)), e);
        }
    }

    private <T> T parseResultSet(EntityKey<? extends T> entityKey, ResultSet resultSet) {
        moveResultSetCursor(resultSet);

        Class<? extends T> entityClass = entityKey.getEntityClass();
        checkDefaultConstructor(entityClass);
        T entity = createEntity(entityClass);

        List<Field> fields = ReflectionUtils.getEntityColumns(entityClass);
        fields.forEach(field -> fillEntityField(resultSet, field, entity));


        return entity;
    }

    private static void moveResultSetCursor(ResultSet resultSet) {
        try {
            if (!resultSet.next()) {
                throw new DaoOperationException("No data found");
            }
        } catch (SQLException e) {
            throw new DaoOperationException("Failed to move cursor for ResultSet", e);
        }
    }

    private static <T> void fillEntityField(ResultSet resultSet, Field field, T entity) {
        try {
            String columnName = field.getAnnotation(Column.class).name();
            Object value = resultSet.getObject(columnName);
            setFieldValue(entity, field, value);
        } catch (SQLException e) {
            throw new DaoOperationException("Failed to parse ResultSet", e);
        }
    }

    private static <T> void setFieldValue(T entity, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new DaoOperationException("Failed to set value [%s] for entity column [%s]".formatted(value, field.getName()), e);
        }
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
        if (!ReflectionUtils.isDefaultConstructorPresent(entityClass)) {
            throw new DaoOperationException("Could instantiate entity %s. No default constructor declared".formatted(entityClass.getSimpleName()));
        }
    }

    private Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DaoOperationException("Failed to acquire connection", e);
        }
    }

}
