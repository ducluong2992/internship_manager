package com.qlnv.modules.overtime.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qlnv.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "overtime_requests", indexes = {
    @Index(name = "idx_ot_user_date", columnList = "user_id, work_date"),
    @Index(name = "idx_ot_status_date", columnList = "status, work_date"),
    @Index(name = "idx_ot_weighted_hours", columnList = "weighted_hours")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    private String project;

    @Column(name = "work_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    private String startTime;

    @Column(name = "end_time", nullable = false)
    private String endTime;

    @Column(name = "raw_hours", nullable = false)
    private Double rawHours;

    @Column(nullable = false)
    private Double factor;

    @Column(name = "weighted_hours", nullable = false)
    private Double weightedHours;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Column(nullable = false)
    private String status = "Pending"; // Pending / Approved / Rejected

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", insertable = false, updatable = false)
    private User approver;

    @Column(name = "approved_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
