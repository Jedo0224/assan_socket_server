package org.asansocketserver.domain.watch.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "accessible_area")
public class AccessibleArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "watch_id")
    private Watch watch;


}
