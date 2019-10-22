package com.karl.openkarlandroid_update.bean;

import java.io.Serializable;

/**
 * App的返回实体
 */
public class AppCommonResponse implements Serializable {

    private boolean success;

    private AppApkVersionInfoResponse data;

    private String msg;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public AppApkVersionInfoResponse getData() {
        return data;
    }

    public void setData(AppApkVersionInfoResponse data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
