package com.training.FAMPortfolioManager.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// A single point on the performance chart: one month + the total portfolio value that month.
// Returned as a list by the /performance endpoints and consumed by the frontend chart.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDataPointDto {

    private LocalDate date;        // First day of the month this point represents
    private double totalValue;     // Combined market value of all relevant assets that month
}