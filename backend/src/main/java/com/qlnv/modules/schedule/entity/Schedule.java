package com.qlnv.modules.schedule.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qlnv.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedules", indexes = {
    @Index(name = "idx_schedules_period_user", columnList = "period_id, user_id"),
    @Index(name = "idx_schedules_work_day", columnList = "work_day")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "period_id", nullable = false)
    private Integer periodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", insertable = false, updatable = false)
    private SchedulePeriod period;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "work_day", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDay;

    private String shift; // S / C / SC / None

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
