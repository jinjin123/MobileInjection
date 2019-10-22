package com.karl.openkarlandroid_update.bean;

import java.io.Serializable;
import java.util.Date;

/**
 * 服务端返回的当前应用的最新版本信息
 */
public class AppApkVersionInfoResponse implements Serializable {

    private int  apkInfoId;

    private String apkInfoName;

    private int versionId;

    private String versionDesc;

    private Date versionUpdateDate;

    private String url;

    private int versionCode;

    public int getApkInfoId() {
        return apkInfoId;
    }

    public void setApkInfoId(int apkInfoId) {
        this.apkInfoId = apkInfoId;
    }

    public String getApkInfoName() {
        return apkInfoName;
    }

    public void setApkInfoName(String apkInfoName) {
        this.apkInfoName = apkInfoName;
    }

    public int getVersionId() {
        return versionId;
    }

    public void setVersionId(int versionId) {
        this.versionId = versionId;
    }

    public String getVersionDesc() {
        return versionDesc;
    }

    public void setVersionDesc(String versionDesc) {
        this.versionDesc = versionDesc;
    }

    public Date getVersionUpdateDate() {
        return versionUpdateDate;
    }

    public void setVersionUpdateDate(Date versionUpdateDate) {
        this.versionUpdateDate = versionUpdateDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }
}
