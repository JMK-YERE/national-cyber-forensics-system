package com.tz.forensics.dto;

public class EvidenceDto {
    private String description;
    private String sourceDevice;
    private String acquisitionMethod;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceDevice() { return sourceDevice; }
    public void setSourceDevice(String sourceDevice) { this.sourceDevice = sourceDevice; }
    public String getAcquisitionMethod() { return acquisitionMethod; }
    public void setAcquisitionMethod(String acquisitionMethod) { this.acquisitionMethod = acquisitionMethod; }
}
