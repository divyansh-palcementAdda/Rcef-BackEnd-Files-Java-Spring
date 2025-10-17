package com.renaissance.app.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "departments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString(exclude = {"tasks", "users"})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;

    @NotBlank
    @Size(min = 1, max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @PastOrPresent
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Lazy loaded users to prevent serialization issues
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<User> users;

    @ManyToMany(mappedBy = "departments")
    private Set<Task> tasks = new HashSet<>();

    // ===============================
    // Equals and HashCode (exclude collections)
    // ===============================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department)) return false;
        Department that = (Department) o;
        return Objects.equals(departmentId, that.departmentId) &&
               Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departmentId, name);
    }
}
