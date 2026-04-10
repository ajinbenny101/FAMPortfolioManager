package com.training.FAMPortfolioManager.dto;

// PerformanceDataPointDto - data point for portfolio performance chart
// CLASS ANNOTATIONS: (none, this is a plain POJO)
// FIELD ANNOTATIONS:
//   @Getter, @Setter - Lombok annotations for automatic getters/setters
//   @NoArgsConstructor - Lombok creates no-arg constructor
//   @AllArgsConstructor - Lombok creates constructor with all fields
// Two fields only: date (LocalDate) and totalValue (double)
// One object per data point for the line chart visualization
// Returned as a list by GET /api/portfolio/performance endpoint
//
// FIELD LAYOUT:
// private LocalDate date;       // Purchase date or aggregation date
// private double totalValue;    // Total portfolio value at this date
//
// IMPORTS NEEDED:
// import java.time.LocalDate;
// import lombok.Getter;
// import lombok.Setter;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
public class PerformanceDataPointDto {

}

