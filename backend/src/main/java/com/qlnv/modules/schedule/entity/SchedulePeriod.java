package com.qlnv.modules.schedule.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_period")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulePeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "open_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime openDate;

    @Column(name = "close_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime closeDate;

    @Builder.Default
    @Column(nullable = false)
    private String status = "closed"; // open / closed
}
