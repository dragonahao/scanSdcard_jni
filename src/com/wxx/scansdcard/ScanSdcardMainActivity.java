package com.wxx.scansdcard;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ScanSdcardMainActivity extends Activity {
	//各种参数
	private static final String TAG = "ScanSdcardMainActivity";
	private static final String defaultScanPath = "/storage/sdcard1/";
	private EditText pathEt;
	private Button startScanBtn;
	private Button clearBtn;
	private TextView currentScannedFileTv;
	private TextView scannedResultTv;
	private boolean scanning;	
	private boolean LocalDebug = false;
	private String currentScannedFile = "";
	private String allScannedFile = "";
	private int count;
	private long endTime;
	private long startTime;
	
	//加载库
	static{
		System.loadLibrary("scan_jni");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan_sdcard_main);
		initViews();
		if(LocalDebug) Log.d(TAG,"onCreate");		
	}
	
	//初始化UI
	private void initViews(){
		pathEt = (EditText) this.findViewById(R.id.scanPathEt);
		startScanBtn = (Button) this.findViewById(R.id.scanBtn);
		clearBtn = (Button) this.findViewById(R.id.clearBtn);
		currentScannedFileTv = (TextView) this.findViewById(R.id.currentScannedFile);
		scannedResultTv = (TextView) this.findViewById(R.id.scanResultDetailTv);
		
		startScanBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(!scanning){
					scanning = true;
					startScan();
				}else{
					showScanningTips();
				}
			}
			
		});
		
		clearBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				clearScreen();
			}
			
		});
	}
	
	//初始化变量
	private void initLocalVarible(){
		currentScannedFile = "";
		allScannedFile = "";
		count = 0;
		endTime = 0;
		startTime = 0;
	}

	//清屏
	private void clearScreen(){
		clearScreenHandler.sendEmptyMessage(0);
	}
	
	//UI Handler
	private Handler clearScreenHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			currentScannedFileTv.setText("");
			scannedResultTv.setText("");
		}
	};
		
	private Handler updataCurrentScannedFileHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			currentScannedFileTv.setText(currentScannedFile);
		}
	};

	private Handler updataDetailScannedFileHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			scannedResultTv.setText(allScannedFile);
			int seconds = (int)( (endTime - startTime) / 1000 );
			int milSeconds = (int)( (endTime - startTime) % 1000 );
			currentScannedFileTv.setText("共扫描到 " + count + "个文件，耗时 " + seconds +" 秒  " + milSeconds + " 毫秒" );
			initLocalVarible();
		}
	};

	private Handler showTipsHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			if(msg.what == 0){
				new AlertDialog.Builder(ScanSdcardMainActivity.this).setTitle("温馨小提示").setMessage("我正在扫描啊，我草了，急个毛！").show();
			}
			if(msg.what == 1){
				Toast.makeText(ScanSdcardMainActivity.this, "路径有问题",Toast.LENGTH_LONG).show();;
			}
		}
	};
	
	//记录扫描时间
	private void recordStartTime(){
		startTime = System.currentTimeMillis();
	}

	private void recordEndTime(){
		endTime = System.currentTimeMillis();
	}
		
	//检测用户输入
	private boolean invalidateInput(){
		if( ! TextUtils.isEmpty(pathEt.getText().toString()) 
				&& !( new File( this.pathEt.getText().toString() ).exists() ) ){
			return true;
		}
		return false;
	}
	
	private boolean sdcardError(){
		if( !( new File( defaultScanPath ).exists() ) ){
			return true;
		}
		return false;
	}
		
	private void showScanningTips(){
		showTipsHandler.sendEmptyMessage(0);		
	}

	private void showInvalidatePathTips(){
		showTipsHandler.sendEmptyMessage(1);		
	}

	private void displayResult(){
		updataDetailScannedFileHandler.sendEmptyMessage(0);
	}
	
	//*****业务函数*****
	//JNI回调Java函数
	private void updateResult(String currentScannedFileName){
		if(LocalDebug) Log.d(TAG,"JNI回调JAVA函数updateResult()");
		currentScannedFile = currentScannedFileName;
		String tempFile = currentScannedFile + "\n";
		allScannedFile += tempFile;
		updataCurrentScannedFileHandler.sendEmptyMessage(0);
		count ++;
	}
	
	private void startScan(){
		new Thread(){
			@Override
			public void run(){
				//非法输入
				if(invalidateInput()){
					showInvalidatePathTips();
					scanning = false;
					return;
				}
				if(LocalDebug) Log.d(TAG,"开始扫描");
				//用户无输入
				if(TextUtils.isEmpty(pathEt.getText().toString())){
					Log.d(TAG,"扫描默认路径");
					if(sdcardError()){
						showInvalidatePathTips();
						scanning = false;
					}
					startScan(defaultScanPath);
				//扫描指定路径
				}else{
					if(LocalDebug) Log.d(TAG,"扫描指定路径");
					startScan(pathEt.getText().toString());
				}
				//显示所有扫描结果
				displayResult();
				if(LocalDebug) Log.d(TAG,"扫描结束");
				scanning = false;
			}
		}.start();
	}
	
	//清屏
	//初始化本次扫描变量
	//记录开始扫描时间
	//记录扫描结束时间
	private void startScan(final String path){
		clearScreen();
		initLocalVarible();
		recordStartTime();
		scanFile(path);
		recordEndTime();		
	}
	
	//Native函数
	private native final void scanFile(String defaultScanPath);
}
