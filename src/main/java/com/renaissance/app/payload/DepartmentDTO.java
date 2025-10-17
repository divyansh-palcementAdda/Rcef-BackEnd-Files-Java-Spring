package com.renaissance.app.payload;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentDTO {
    private Long departmentId;
    private String name;
    private String description;
    private List<UserDTO> users; // optional: include users
}