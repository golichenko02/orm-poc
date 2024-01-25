package com.bobocode;

import com.bobocode.annotation.Column;
import com.bobocode.annotation.Id;
import com.bobocode.annotation.Table;
import lombok.Setter;
import lombok.ToString;

@Table(name = "persons")
@ToString
public class Person {

    @Id
    @Column(name = "id")
    private Long id;

    @Setter
    @Column(name = "first_name")
    private String firstName;

    @Setter
    @Column(name = "last_name")
    private String lastName;

}
