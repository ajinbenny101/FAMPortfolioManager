package com.training.FAMPortfolioManager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


// PortfolioRequestDTO - DTO for creating/updating a portfolio
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class PortfolioRequestDTO {
    private String name;
    private String description;
}