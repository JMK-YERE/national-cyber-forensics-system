package com.tz.forensics.dto;

public class EvidenceDto {
    private Long caseId;
    public Long getCaseId(){return caseId;} public void setCaseId(Long v){caseId=v;}
    private String description, sourceDevice, acquisitionMethod, acquisitionType;
    private String deviceMake, deviceModel, deviceSerial, sourceIdentifier;
    private String acquisitionTool, acquisitionToolVersion, acquisitionNotes;
    private Boolean writeBlockerUsed;

    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getSourceDevice(){return sourceDevice;} public void setSourceDevice(String v){sourceDevice=v;}
    public String getAcquisitionMethod(){return acquisitionMethod;} public void setAcquisitionMethod(String v){acquisitionMethod=v;}
    public String getAcquisitionType(){return acquisitionType;} public void setAcquisitionType(String v){acquisitionType=v;}
    public String getDeviceMake(){return deviceMake;} public void setDeviceMake(String v){deviceMake=v;}
    public String getDeviceModel(){return deviceModel;} public void setDeviceModel(String v){deviceModel=v;}
    public String getDeviceSerial(){return deviceSerial;} public void setDeviceSerial(String v){deviceSerial=v;}
    public String getSourceIdentifier(){return sourceIdentifier;} public void setSourceIdentifier(String v){sourceIdentifier=v;}
    public String getAcquisitionTool(){return acquisitionTool;} public void setAcquisitionTool(String v){acquisitionTool=v;}
    public String getAcquisitionToolVersion(){return acquisitionToolVersion;} public void setAcquisitionToolVersion(String v){acquisitionToolVersion=v;}
    public Boolean getWriteBlockerUsed(){return writeBlockerUsed;} public void setWriteBlockerUsed(Boolean v){writeBlockerUsed=v;}
    public String getAcquisitionNotes(){return acquisitionNotes;} public void setAcquisitionNotes(String v){acquisitionNotes=v;}
}