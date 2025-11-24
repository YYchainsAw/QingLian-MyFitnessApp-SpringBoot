package com.yychainsaw.pojo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @NotNull
    @Id
    private Long id;
    @NotEmpty
    private String username;
    @NotEmpty
    private String passwordHash;
    @NotEmpty
    private String nickname;
    private String avatarUrl;
    private String gender;
    private Integer height;
    private BigDecimal weight;
    private LocalDate createAt;
}
