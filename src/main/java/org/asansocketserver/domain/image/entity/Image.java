package org.asansocketserver.domain.image.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Table(name = "image")
@Entity
public class Image {
    @Id
    @GeneratedValue
    private Long id;
    private String imageUrl;
    @OneToMany
    private List<Coordinate> coordinateList = new ArrayList<>();
}


