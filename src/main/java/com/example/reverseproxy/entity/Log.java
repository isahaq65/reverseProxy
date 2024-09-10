package com.example.reverseproxy.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "LOG_TBL")
public class Log {

    @Id
    @GeneratedValue
    private int id;

    @ManyToOne
    @JoinColumn(name = "context_object_id")
    private Context ctx;

    private String url;

    private int urlBodySize;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private int httpStatus;

    private String responseErrorMessage;

}
