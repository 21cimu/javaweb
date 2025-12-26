package com.carrental.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vehicle entity representing a rental car
 */
public class Vehicle implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String vin;
    private String plateNumber;
    private String brand;
    private String model;
    private String series;
    private Integer year;
    private String color;
    private Integer seats;
    private String fuelType; // gasoline, diesel, electric, hybrid
    private String transmission; // auto, manual
    private String category; // economy, compact, midsize, suv, luxury, minivan
    private Long storeId;
    private String storeName;
    private BigDecimal dailyPrice;
    private BigDecimal weeklyPrice;
    private BigDecimal monthlyPrice;
    private BigDecimal deposit;
    private Integer mileage;
    private Integer status; // 0-下架, 1-可租, 2-预订中, 3-出租中, 4-维修中, 5-清洁中, 6-事故
    private String mainImage;
    private String images;
    private String features; // JSON array of features
    private String description;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private LocalDate insuranceExpiry;
    private LocalDate inspectionExpiry;
    private LocalDate registrationExpiry;
    private LocalDate lastMaintenanceDate;
    private Integer viewCount;
    private Integer orderCount;
    private BigDecimal rating;
    private Boolean isHot;
    private Boolean isNew;
    private Boolean noDeposit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Vehicle() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public BigDecimal getDailyPrice() { return dailyPrice; }
    public void setDailyPrice(BigDecimal dailyPrice) { this.dailyPrice = dailyPrice; }

    public BigDecimal getWeeklyPrice() { return weeklyPrice; }
    public void setWeeklyPrice(BigDecimal weeklyPrice) { this.weeklyPrice = weeklyPrice; }

    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }

    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }

    public Integer getMileage() { return mileage; }
    public void setMileage(Integer mileage) { this.mileage = mileage; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public LocalDate getInsuranceExpiry() { return insuranceExpiry; }
    public void setInsuranceExpiry(LocalDate insuranceExpiry) { this.insuranceExpiry = insuranceExpiry; }

    public LocalDate getInspectionExpiry() { return inspectionExpiry; }
    public void setInspectionExpiry(LocalDate inspectionExpiry) { this.inspectionExpiry = inspectionExpiry; }

    public LocalDate getRegistrationExpiry() { return registrationExpiry; }
    public void setRegistrationExpiry(LocalDate registrationExpiry) { this.registrationExpiry = registrationExpiry; }

    public LocalDate getLastMaintenanceDate() { return lastMaintenanceDate; }
    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Boolean getIsHot() { return isHot; }
    public void setIsHot(Boolean isHot) { this.isHot = isHot; }

    public Boolean getIsNew() { return isNew; }
    public void setIsNew(Boolean isNew) { this.isNew = isNew; }

    public Boolean getNoDeposit() { return noDeposit; }
    public void setNoDeposit(Boolean noDeposit) { this.noDeposit = noDeposit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
