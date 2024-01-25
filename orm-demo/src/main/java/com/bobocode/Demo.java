package com.bobocode;

import org.postgresql.ds.PGSimpleDataSource;

public class Demo {
    public static void main(String[] args) {
        try (EntityManager entityManager = createEntityManager()) {
            Person person = entityManager.find(Person.class, 3022L);
            System.out.println(person);

            // cache
            Person personFromCache = entityManager.find(Person.class, 3022L);
            personFromCache.setFirstName("Michael");

            Note note = entityManager.find(Note.class, "80caefce-17c5-4656-a8dc-d01332690cf5");
            System.out.println(note);
            note.setBody("new body");
        }

    }

    private static EntityManager createEntityManager() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("admin");
        return new EntityManager(dataSource);
    }
}
