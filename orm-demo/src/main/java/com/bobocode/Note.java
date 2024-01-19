package com.bobocode;

import com.bobocode.annotation.Column;
import com.bobocode.annotation.Id;
import com.bobocode.annotation.Table;
import lombok.ToString;

import java.util.UUID;

@Table(name = "notes")
@ToString
public class Note {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "title")
    private String title;

    @Column(name = "body")
    private String body;

    @Column(name = "person_id")
    private Long personId;
}
