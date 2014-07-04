package com.huichongzi.download_android.download;

import java.io.File;
import android.util.Log;


/**
 * 下载主类
 * Created by cuihz on 2014/7/3.
 */
public class Downloader {
    private DownloaderListener downloadListener = null;
    protected DownloadInfo di;
    //下载中断是否重连
    private static boolean isReconnect = false;



    private Downloader(DownloadInfo di, DownloaderListener listener) {
        this.di = di;
        this.downloadListener = listener;
    }


    /**
     * 检查存储情况。通过不同的回调处理相应的事件。
     * 1、有异常或错误
     * 2、未下载或未下载完开启下载线程
     */
    private void tryStorage() {
        // 新建存储线程（存储可能需要3-5s，所以以线程方式）
        StorageHandleTask sh = new StorageHandleTask(di, new StorageListener() {
            public void onAlreadyDownload(String path) {
                Log.d("onAlreadyDownload", di.getName() + " already exists in "
                        + path);
                if (downloadListener != null) {
                    downloadListener.onDownloadRepeat(di.getName() + "文件已经下载过");
                }
            }
            public void onDownloadNotFinished(String path) {
                Log.e("onDownloadNotFinished", di.getName()
                        + " is download but not finished in " + path);
                download(false);
            }
            public void onNotDownload(String path) {
                Log.e("onNotDownload", di.getName()
                        + " is  not download,it will download in "
                        + path);
                download(true);
            }
            public void onStorageNotEnough(long softSize, long avilableSize) {
                Log.e("onStorageNotEnough", di.getName()
                        + "not enough size sdsize=" + avilableSize);
                String msg = "空间不足";
                if (downloadListener != null) {
                    downloadListener.onCreateFailed(msg);
                }
            }
            public void onStorageNotMount(String path) {
                Log.e("onStorageNotMount", di.getName() + "rom can't chmod");
                String msg = "sd卡不存在";
                if (downloadListener != null) {
                    downloadListener.onCreateFailed(msg);
                }
            }
            public void onDownloadPathConnectError(String msg) {
                Log.e("onDownloadPathConnectError", di.getName() + "无法连接到下载地址" + di.getUrl());
                if (downloadListener != null) {
                    downloadListener.onConnectFailed(di.getName() + msg);
                }
            }
            public void onFileSizeError() {
                Log.e("onDownloadPathConnectError", di.getName() + "文件大小与服务器不符" + di.getUrl());
                if (downloadListener != null) {
                    downloadListener.onCheckFailed(di.getName() + "文件大小与服务器不符");
                }
            }
        });
        sh.start();
    }


    /**
     * 开启下载线程
     * @param isNew 是否为新的下载
     */
    private void download(boolean isNew) {
        // 启动文件下载线程
        DownloadTask downloadTask = new DownloadTask(di, downloadListener, isNew);
        downloadTask.start();
    }

    /**
     * 取消下载
     * 删除文件
     */
    protected void stopDownload() {
        di.setState(DownloadOrder.STATE_STOP);
        DownloadUtils.removeFile(di.getPath());
    }




    /**
     * 暂停下载
     * @param id 下载唯一id
     */
    public static void pauseDownload(String id) {
        if(DownloadList.has(id)){
            Downloader down = DownloadList.getFromMap(id);
            if(downloaderIsUsable(down)){
                down.di.setState(DownloadOrder.STATE_PAUSE);
                return;
            }
            DownloadList.remove(id);
        }
    }


    /**
     * 恢复下载
     * @param id 下载唯一id
     * @throws DownloadNotExistException 下载任务不存在
     */
    public static void resumeDownload(String id) throws DownloadNotExistException {
        if(DownloadList.has(id)){
            Downloader down = DownloadList.getFromMap(id);
            if(downloaderIsUsable(down)){
                down.di.setState(DownloadOrder.STATE_WAIT);
                return;
            }
            DownloadList.remove(id);
        }
        throw new DownloadNotExistException();
    }





    /**
     * 取消下载
     * @param id
     */
    public static void cancelDownload(String id){
        if(DownloadList.has(id)){
            Downloader down = DownloadList.getFromMap(id);
            if(downloaderIsUsable(down)){
                down.stopDownload();
            }
            DownloadList.remove(id);
        }
    }


    /**
     * 添加下载任务
     * @param di 下载信息
     * @param listener 下载事件回调
     * @throws DownloadRepeatException 重复下载异常
     */
    public static void downloadEvent(DownloadInfo di, DownloaderListener listener) throws DownloadRepeatException{
        Downloader downloader = new Downloader(di, listener);
        //检查是否已下载完成
        File file = new File(di.getPath());
        if (file.exists() && file.isFile()) {
            di.setState(DownloadOrder.STATE_SUCCESS);
            DownloadList.add(downloader);
            throw new DownloadRepeatException("已经下载过了");
        }
        //检查是否已经在任务列表中
        if (DownloadList.has(di.getId()) && downloaderIsUsable(DownloadList.getFromMap(di.getId()))) {
                Log.d("download", di.getName() + " is downloading");
                throw new DownloadRepeatException("已经在任务列表中");
        }
        DownloadList.add(downloader);
        di.setState(DownloadOrder.STATE_WAIT);
    }


    public static void setReconnect(boolean flag) {
        isReconnect = flag;
    }


    /**
     *  判断一个下载者是否可用
     * @param down
     * @return
     */
    private static boolean downloaderIsUsable(Downloader down){
        if(down == null || down.di == null || down.downloadListener == null){
            return false;
        }
        int state = down.di.getState();
        if(state != DownloadOrder.STATE_PAUSE || state != DownloadOrder.STATE_FAILED || state != DownloadOrder.STATE_WAIT){
            return false;
        }
        return true;
    }



}
