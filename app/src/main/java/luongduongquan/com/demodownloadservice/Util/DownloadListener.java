package luongduongquan.com.demodownloadservice.Util;

public interface DownloadListener {
    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCancle();
}
