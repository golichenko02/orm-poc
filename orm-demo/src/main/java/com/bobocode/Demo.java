package com.bobocode;

import org.postgresql.ds.PGSimpleDataSource;

public class Demo {
    public static void main(String[] args) {
        EntityManager entityManager = createEntityManager();
        Person person = entityManager.find(Person.class, 1L);
        System.out.println(person);
        Note note1 = entityManager.find(Note.class, "2e9faa1d-dec5-471c-984e-02fe3aafee95");
        System.out.println(note1);
        Note note2 = entityManager.find(Note.class, "e36240cd-c320-41f4-b84f-4bf4ee9c7a9a");
        System.out.println(note2);
    }
    private static EntityManager createEntityManager() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("admin");
        return new EntityManager(dataSource);
    }
}
