package luongduongquan.com.demodownloadservice;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

	private Button start;
	private Button pause;
	private Button cancel;
	private DownloadService.DownloadBinder downloadBinder; // Custom download binder để giao tiếp với BindService.
	private ServiceConnection connection=new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			downloadBinder=(DownloadService.DownloadBinder)service;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {


		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		start=(Button)findViewById(R.id.start_download);
		pause=(Button)findViewById(R.id.pause_download);
		cancel=(Button)findViewById(R.id.cancel_download);

		myOnClickListener myOnClickListener=new myOnClickListener();
		start.setOnClickListener(myOnClickListener);
		pause.setOnClickListener(myOnClickListener);
		cancel.setOnClickListener(myOnClickListener);

		/*Sử dụng Bound Service (trong bound service có chứa AsyncTask) để quản lý việc download*/
		Intent intent = new Intent(MainActivity.this,DownloadService.class);
//        startService(intent);
		bindService(intent,connection,BIND_AUTO_CREATE);// start Bind Service.
		/*Kiểm tra quyền đọc ghi dữ liệu*/
		if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
			ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
		}
	}
	class myOnClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			if(downloadBinder==null){
				return;
			}
			switch (v.getId()){
				case R.id.start_download:
					// Truyền Link cần download vào đây.
					String url="http://www.shadedrelief.com/world/Data/map1_type.jpg";
					downloadBinder.startDownload(url);
					break;
				case R.id.pause_download:
					downloadBinder.pauseDownload();
					break;
				case R.id.cancel_download:
					downloadBinder.cancelDownload();
					break;
				default:
					break;
			}

		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		// Callback trả về sau khi check permission do cái hàm ContextCompat.checkSelfPermission ở trên
		switch (requestCode){
			case 1:
				if(grantResults.length>0&&grantResults[0]!=PackageManager.PERMISSION_GRANTED){
					Toast.makeText(MainActivity.this,"Permission is denied!",Toast.LENGTH_SHORT).show();
					finish();
				}
				break;
			default:
				break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
	}
}
