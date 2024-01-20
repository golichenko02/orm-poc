package com.bobocode;

import org.postgresql.ds.PGSimpleDataSource;

public class Demo {
    public static void main(String[] args) {
        EntityManager entityManager = createEntityManager();
        Person person = entityManager.find(Person.class, 1L);
        System.out.println(person);
        Note note1 = entityManager.find(Note.class, "d0d3e59b-a985-4d35-8fd8-8fa0ec640f6c");
        System.out.println(note1);
        Note note2 = entityManager.find(Note.class, "4e79b80b-5173-4281-834c-91b9f1cf0945");
        System.out.println(note2);

        // cache
        Person personFromCache = entityManager.find(Person.class, 1L);
        System.out.println(person == personFromCache);
        Note note1FromCache = entityManager.find(Note.class, "d0d3e59b-a985-4d35-8fd8-8fa0ec640f6c");
        System.out.println(note1 == note1FromCache);
        Note note2FromCache = entityManager.find(Note.class, "4e79b80b-5173-4281-834c-91b9f1cf0945");
        System.out.println(note2 == note2FromCache);
    }

    private static EntityManager createEntityManager() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("admin");
        return new EntityManager(dataSource);
    }
}
