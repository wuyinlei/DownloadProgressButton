package com.mingchu.downloadprogressbutton.utils;

import android.content.Context;

import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.Observable;


/**
 * 权限工具类
 */

public class PermissionUtil {





    public static Observable<Boolean> requestPermisson(Context activity, String permission){


        RxPermissions rxPermissions =  RxPermissions.getInstance(activity);


        return rxPermissions.request(permission);
    }





}
