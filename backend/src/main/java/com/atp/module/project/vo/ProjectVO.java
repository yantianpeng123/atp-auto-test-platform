package com.atp.module.project.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目出参
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVO {

    private Long id;

    private String name;

    private String team;

    private Long ownerId;
}
