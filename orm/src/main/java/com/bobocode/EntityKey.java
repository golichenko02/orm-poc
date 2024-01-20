package com.bobocode;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@EqualsAndHashCode(of = {"entityClass", "id"})
@Getter
@ToString
public class EntityKey<T> {

    private final Class<T> entityClass;
    private final Object id;
}
