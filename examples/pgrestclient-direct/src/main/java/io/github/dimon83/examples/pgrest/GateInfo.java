package io.github.dimon83.examples.pgrest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GateInfo {
    private Long id;
    private String parkCode;
    private String equipName;
    private String equipType;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String equipCode;
    private String delFlag;
    private LocalDateTime createTime;
    private String createBy;
    private LocalDateTime updateTime;
    private String updateBy;
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getParkCode() { return parkCode; }
    public void setParkCode(String parkCode) { this.parkCode = parkCode; }
    public String getEquipName() { return equipName; }
    public void setEquipName(String equipName) { this.equipName = equipName; }
    public String getEquipType() { return equipType; }
    public void setEquipType(String equipType) { this.equipType = equipType; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public String getEquipCode() { return equipCode; }
    public void setEquipCode(String equipCode) { this.equipCode = equipCode; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String toString() {
        return "GateInfo{" +
            "id=" + id +
            ", parkCode='" + parkCode + '\'' +
            ", equipName='" + equipName + '\'' +
            ", equipType='" + equipType + '\'' +
            ", longitude=" + longitude +
            ", latitude=" + latitude +
            ", equipCode='" + equipCode + '\'' +
            ", delFlag='" + delFlag + '\'' +
            ", createTime=" + createTime +
            ", createBy='" + createBy + '\'' +
            ", updateTime=" + updateTime +
            ", updateBy='" + updateBy + '\'' +
            ", tenantId=" + tenantId +
            '}';
    }
}