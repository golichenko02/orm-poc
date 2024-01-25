package com.bobocode.utils;

import com.bobocode.annotation.Column;
import com.bobocode.annotation.Id;
import com.bobocode.annotation.Table;
import com.bobocode.exception.DaoOperationException;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class ReflectionUtils {


    public static boolean isUUIDField(Class<?> clazz, String fieldName) {
        return UUID.class.isAssignableFrom(getFieldType(clazz, fieldName));
    }

    public static boolean isDefaultConstructorPresent(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == 0);
    }

    public static Class<?> getFieldType(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName).getType();
        } catch (NoSuchFieldException e) {
            throw new DaoOperationException("Failed to get field by name [%s]".formatted(fieldName), e);
        }
    }

    public <T> List<Field> getEntityColumns(Class<? extends T> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .collect(Collectors.toList());
    }

    public <T> List<String> getEntityColumnNamesWithoutId(Class<? extends T> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .map(field -> field.getAnnotation(Column.class).name())
                .collect(Collectors.toList());
    }

    public <T> Object[] getEntityColumnValues(T entity) {
        return getEntityColumns(entity.getClass()).stream()
                .map(field -> getFieldValue(field, entity))
                .toArray();
    }

    public <T> Object[] getEntityColumnValuesWithoutId(T entity) {
        return getEntityColumns(entity.getClass()).stream()
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .map(field -> getFieldValue(field, entity))
                .toArray();
    }


    public static <T> Object getFieldValue(Field field, T currentEntityState) {
        try {
            field.setAccessible(true);
            return field.get(currentEntityState);
        } catch (IllegalAccessException e) {
            throw new DaoOperationException("Failed to get field value for field [%s]".formatted(field.getName()), e);
        }
    }


    public <T> String findPrimaryKeyName(Class<? extends T> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class) && field.isAnnotationPresent(Column.class))
                .findFirst()
                .map(field -> field.getAnnotation(Column.class).name())
                .orElseThrow(() -> new DaoOperationException("Primary key column is not specified"));
    }


    public static <T> String getTableName(Class<? extends T> entityClass) {
        return entityClass.getAnnotation(Table.class).name();
    }
}
