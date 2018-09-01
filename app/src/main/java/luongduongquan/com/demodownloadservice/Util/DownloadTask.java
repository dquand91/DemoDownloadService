package luongduongquan.com.demodownloadservice.Util;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class DownloadTask extends AsyncTask<String,Integer,Integer> {
    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSED=2;
    public static final int TYPE_CANCLED=3;


    /*CallBack để trả kết quả về cho DownloadService*/
    private DownloadListener listener;
    private boolean isCanceled=false;
    private boolean isPaused=false;
    private int lastProgress;
    public DownloadTask(DownloadListener listener){
        this.listener=listener;
    }
    /*Xử lý việc download bằng AsynTask*/
    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;  //input stream để xử lý file download

        File file = null; //File để xử lý việc download

        //Tạo ra 1 file, mà file đó có thể đọc/ghi ở bất kỳ vị trí trên file đó. Giúp cho việc download xong pause giữa chừng rồi download tiếp.
        //Lúc này file của chúng ta sẽ như 1 cái Array vậy, mình có thể truy xuất vào bất kỳ vị trí nào của file.
        // Từ FILE ở trên, sẽ đưa vào file dạng này để xử lý việc download/pause
        RandomAccessFile savedFile = null;

        try {
            long downloadedLength = 0;  // chứa kích thước file đã download được.
            String downloadUrl = params[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();//保存路径
            file = new File(directory + fileName);
            if (file.exists()) {
                // Nếu file đã tồn tại, hoặc download dang dở, thì lấy ra được kích thước file đang có trên device.
                downloadedLength = file.length();
            }
        /*Total file byte*/
        // Để lấy ra độ dài của file cần download
            long contentLength = getContentLehgth(downloadUrl);
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    //header RANGE để báo hiệu đã download được tới đâu, sẽ tiếp tục download tiếp phần còn lại
                    // nếu downloadedLength =0 => download lại từ đầu
                    // nếu downloadedLenght = kích thước file cần downloaf => file đã download xong rồi => ko down nữa
                    .addHeader("RANGE","bytes="+downloadedLength+"-")
                    .url(downloadUrl).build();
            Response response=client.newCall(request).execute();
            if(request!=null){
                is=response.body().byteStream();
                savedFile=new RandomAccessFile(file,"rw");
                savedFile.seek(downloadedLength);
                byte [] b=new byte[1024];
                int total =0;
                int len;
                while ((len=is.read(b))!=-1){
                    if(isCanceled) {
                        return TYPE_CANCLED;
                    }else  if(isPaused){
                        return TYPE_PAUSED;
                    }else {
                        //total là tổng byte của file hiện tại đang download
                        total+=len;
                        savedFile.write(b,0,len);
                        int progress =(int) ((total+downloadedLength)*100/contentLength);
                        // cập nhật % progress lên UI
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(is!=null){
                    is.close();
                }
                if(savedFile!=null){
                    savedFile.close();
                }
                if(isCanceled&&file!=null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {

        int progress=values[0];
        if(progress>lastProgress){
            listener.onProgress(progress);
            lastProgress=progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_CANCLED:
                listener.onCancle();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            default:
                break;
        }
    }
    public void pauseDownload(){
        isPaused=true;
    }
    public void cancleDownload(){
        isCanceled=true;
    }

    // Để lấy được kích thước của file cần download (dựa vào link URL dùng OKHttp gửi lên server)
    private long getContentLehgth(String downloadUrl) throws IOException {

        OkHttpClient client=new OkHttpClient();
        Request request= new Request.Builder().url(downloadUrl).build();

        Response response= client.newCall(request).execute();
        if(response!=null&&response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}
