package com.mingchu.downloadprogressbutton.download;

import android.content.Context;


import com.jakewharton.rxbinding2.view.RxView;
import com.mingchu.downloadprogressbutton.utils.ACache;
import com.mingchu.downloadprogressbutton.utils.AppUtils;
import com.mingchu.downloadprogressbutton.bean.BaseBean;
import com.mingchu.downloadprogressbutton.constant.Constant;
import com.mingchu.downloadprogressbutton.utils.PermissionUtil;
import com.mingchu.downloadprogressbutton.R;
import com.mingchu.downloadprogressbutton.rx.RxHttpResponseCompat;
import com.mingchu.downloadprogressbutton.rx.RxSchedulers;
import com.mingchu.downloadprogressbutton.bean.AppDownloadInfo;
import com.mingchu.downloadprogressbutton.bean.AppInfo;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import retrofit2.http.GET;
import retrofit2.http.Path;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadBean;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadRecord;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * 下载按钮控制类  和主业务分离  降级耦合度
 */

public class DownloadButtonController {


    private RxDownload mRxDownload;

    private Api mApi;


    /**
     * 构造方法
     * @param downloader  RxDownload
     */
    public DownloadButtonController(RxDownload downloader) {

        this.mRxDownload = downloader;

        if (mRxDownload != null) {
            mApi = mRxDownload.getRetrofit().create(Api.class);
        }

    }


    /**
     * 处理点击事件
     *
     * @param btn    DownloadProgressButton
     * @param record DownloadRecord
     */
    public void handClick(final DownloadProgressButton btn, DownloadRecord record) {

        AppInfo info = downloadRecord2AppInfo(record);

        receiveDownloadStatus(record.getUrl()).subscribe(new DownloadConsumer(btn, info));

    }


    /**
     * 处理点击事件
     * @param btn  DownloadProgressButton
     * @param appInfo  AppInfo
     */
    public void handClick(final DownloadProgressButton btn, final AppInfo appInfo) {


        if (mApi == null) {

            return;
        }

        isAppInstalled(btn.getContext(), appInfo)

                .flatMap(new Function<DownloadEvent, ObservableSource<DownloadEvent>>() {
                    @Override
                    public ObservableSource<DownloadEvent> apply(@NonNull DownloadEvent event)
                            throws Exception {

                        if (DownloadFlag.UN_INSTALL == event.getFlag()) {

                            return isApkFileExsit(btn.getContext(), appInfo);

                        }
                        return Observable.just(event);


                    }
                })
                .flatMap(new Function<DownloadEvent, ObservableSource<DownloadEvent>>() {
                    @Override
                    public ObservableSource<DownloadEvent> apply(@NonNull DownloadEvent event) throws Exception {


                        if (DownloadFlag.FILE_EXIST == event.getFlag()) {

                            return getAppDownloadInfo(appInfo)
                                    .flatMap(new Function<AppDownloadInfo, ObservableSource<DownloadEvent>>() {
                                        @Override
                                        public ObservableSource<DownloadEvent> apply(@NonNull AppDownloadInfo appDownloadInfo) throws Exception {

                                            appInfo.setAppDownloadInfo(appDownloadInfo);

                                            return receiveDownloadStatus(appDownloadInfo.getDownloadUrl());
                                        }
                                    });
                        }
                        return Observable.just(event);
                    }
                })
                .compose(RxSchedulers.<DownloadEvent>io_main())

                .subscribe(new DownloadConsumer(btn, appInfo));


    }

    /**
     * 绑定事件
     * @param btn  DownloadProgressButton
     * @param appInfo  AppInfo
     */
    private void bindClick(final DownloadProgressButton btn, final AppInfo appInfo) {

        RxView.clicks(btn).subscribe(new Consumer<Object>() {


            @Override
            public void accept(@NonNull Object o) throws Exception {


                int flag = (int) btn.getTag(R.id.tag_apk_flag);


                switch (flag) {

                    case DownloadFlag.INSTALLED:

                        runApp(btn.getContext(), appInfo.getPackageName());
                        break;

                    case DownloadFlag.STARTED:
                        pausedDownload(appInfo.getAppDownloadInfo().getDownloadUrl());
                        break;


                    case DownloadFlag.NORMAL:
                    case DownloadFlag.PAUSED:
                        startDownload(btn, appInfo);

                        break;

                    case DownloadFlag.COMPLETED:
                        installApp(btn.getContext(), appInfo);

                        break;

                }


            }
        });
    }

    /**
     * 安装app
     * @param context 上下文
     * @param appInfo AppInfo
     */
    private void installApp(Context context, AppInfo appInfo) {

        String path = ACache.get(context).getAsString(Constant.APK_DOWNLOAD_DIR) + File.separator + appInfo.getReleaseKeyHash();

        AppUtils.installApk(context, path);
    }

    /**
     * 开始下载
     * @param btn DownloadProgressButton
     * @param appInfo AppInfo
     */
    private void startDownload(final DownloadProgressButton btn, final AppInfo appInfo) {


        //判断是否有权限  权限处理
        PermissionUtil.requestPermisson(btn.getContext(), WRITE_EXTERNAL_STORAGE)


                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(@NonNull Boolean aBoolean) throws Exception {


                        if (aBoolean) {

                            final AppDownloadInfo downloadInfo = appInfo.getAppDownloadInfo();

                            if (downloadInfo == null) {

                                getAppDownloadInfo(appInfo).subscribe(new Consumer<AppDownloadInfo>() {
                                    @Override
                                    public void accept(@NonNull AppDownloadInfo appDownloadInfo) throws Exception {

                                        appInfo.setAppDownloadInfo(appDownloadInfo);

                                        download(btn, appInfo);

                                    }
                                });
                            } else {

                                download(btn, appInfo);
                            }
                        }

                    }
                });


    }

    /**
     * 下载
     * @param btn DownloadProgressButton
     * @param info AppInfo
     */
    private void download(DownloadProgressButton btn, AppInfo info) {

        //.subscribe()   要订阅
        mRxDownload.serviceDownload(appInfo2DownloadBean(info)).subscribe();

        mRxDownload.receiveDownloadStatus(info.getAppDownloadInfo().getDownloadUrl())
                .subscribe(new DownloadConsumer(btn, info));

    }


    /**
     * AppInfo转换成下载bean
     * @param info AppInfo
     * @return DownloadBean
     */
    private DownloadBean appInfo2DownloadBean(AppInfo info) {

        DownloadBean downloadBean = new DownloadBean();

        downloadBean.setUrl(info.getAppDownloadInfo().getDownloadUrl());
        downloadBean.setSaveName(info.getReleaseKeyHash() + ".apk");


        downloadBean.setExtra1(info.getId() + "");
        downloadBean.setExtra2(info.getIcon());
        downloadBean.setExtra3(info.getDisplayName());
        downloadBean.setExtra4(info.getPackageName());
        downloadBean.setExtra5(info.getReleaseKeyHash());

        return downloadBean;
    }

    /**
     * @param bean 下载记录转换成AppInfo
     * @return AppInfo
     */
    public AppInfo downloadRecord2AppInfo(DownloadRecord bean) {


        AppInfo info = new AppInfo();

        info.setId(Integer.parseInt(bean.getExtra1()));
        info.setIcon(bean.getExtra2());
        info.setDisplayName(bean.getExtra3());
        info.setPackageName(bean.getExtra4());
        info.setReleaseKeyHash(bean.getExtra5());


        AppDownloadInfo downloadInfo = new AppDownloadInfo();

        downloadInfo.setDowanloadUrl(bean.getUrl());

        info.setAppDownloadInfo(downloadInfo);

        return info;


    }

    /**
     * 暂停下载
     *
     * @param url 下载地址
     */
    private void pausedDownload(String url) {

        mRxDownload.pauseServiceDownload(url).subscribe();
    }

    /**
     * 如果已经安装有app  那就变成可运行状态  点击运行app
     */
    private void runApp(Context context, String packageName) {

        AppUtils.runApp(context, packageName);
    }


    /**
     * 判断该app是否已经安装
     *
     * @param context 上下文
     * @param appInfo AppInfo
     * @return Observable<DownloadEvent></>
     */
    public Observable<DownloadEvent> isAppInstalled(Context context, AppInfo appInfo) {


        DownloadEvent event = new DownloadEvent();

        event.setFlag(AppUtils.isInstalled(context, appInfo.getPackageName()) ? DownloadFlag.INSTALLED : DownloadFlag.UN_INSTALL);

        return Observable.just(event);

    }


    /**
     * 判断apk文件是否存在
     * @param context 上下文
     * @param appInfo AppInfo
     * @return Observable<DownloadEvent>
     */
    public Observable<DownloadEvent> isApkFileExsit(Context context, AppInfo appInfo) {


        String path = ACache.get(context).getAsString(Constant.APK_DOWNLOAD_DIR) + File.separator + appInfo.getReleaseKeyHash();
        File file = new File(path);


        DownloadEvent event = new DownloadEvent();

        event.setFlag(file.exists() ? DownloadFlag.FILE_EXIST : DownloadFlag.NORMAL);

        return Observable.just(event);


    }


    /**
     * 回复下载状态
     * @param url  下载地址
     * @return Observable<DownloadEvent>
     */
    public Observable<DownloadEvent> receiveDownloadStatus(String url) {

        return mRxDownload.receiveDownloadStatus(url);
    }


    /**
     * 获取到app的下载信息
     * @param appInfo AppInfo
     * @return Observable<AppDownloadInfo>
     */
    public Observable<AppDownloadInfo> getAppDownloadInfo(AppInfo appInfo) {

        return mApi.getAppDownloadInfo(appInfo.getId()).compose((ObservableTransformer<? super BaseBean<AppDownloadInfo>, ? extends AppDownloadInfo>) RxHttpResponseCompat.<AppDownloadInfo>compatResult());
    }


    class DownloadConsumer implements Consumer<DownloadEvent> {


        DownloadProgressButton btn;

        AppInfo mAppInfo;

        public DownloadConsumer(DownloadProgressButton b, AppInfo appInfo) {

            this.btn = b;
            this.mAppInfo = appInfo;
        }


        @Override
        public void accept(@NonNull DownloadEvent event) throws Exception {


            int flag = event.getFlag();

            btn.setTag(R.id.tag_apk_flag, flag);

            bindClick(btn, mAppInfo);

            switch (flag) {

                case DownloadFlag.INSTALLED:
                    btn.setText("运行");
                    break;


                case DownloadFlag.NORMAL:
                    btn.download();
                    break;


                case DownloadFlag.STARTED:
                    btn.setProgress((int) event.getDownloadStatus().getPercentNumber());
                    break;

                case DownloadFlag.PAUSED:
                    btn.setProgress((int) event.getDownloadStatus().getPercentNumber());
                    btn.paused();
                    break;


                case DownloadFlag.COMPLETED: //已完成
                    btn.setText("安装");
                    //installApp(btn.getContext(),mAppInfo);
                    break;
                case DownloadFlag.FAILED://下载失败
                    btn.setText("失败");
                    break;
                case DownloadFlag.DELETED: //已删除

                    break;


            }


        }
    }


    interface Api {

        @GET("download/{id}")
        Observable<BaseBean<AppDownloadInfo>> getAppDownloadInfo(@Path("id") int id);
    }
}
