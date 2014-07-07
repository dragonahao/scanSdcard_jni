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
	//���ֲ���
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
	
	//���ؿ�
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
	
	//��ʼ��UI
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
	
	//��ʼ������
	private void initLocalVarible(){
		currentScannedFile = "";
		allScannedFile = "";
		count = 0;
		endTime = 0;
		startTime = 0;
	}

	//����
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
			currentScannedFileTv.setText("��ɨ�赽 " + count + "���ļ�����ʱ " + seconds +" ��  " + milSeconds + " ����" );
			initLocalVarible();
		}
	};

	private Handler showTipsHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			if(msg.what == 0){
				new AlertDialog.Builder(ScanSdcardMainActivity.this).setTitle("��ܰС��ʾ").setMessage("������ɨ�谡���Ҳ��ˣ�����ë��").show();
			}
			if(msg.what == 1){
				Toast.makeText(ScanSdcardMainActivity.this, "·��������",Toast.LENGTH_LONG).show();;
			}
		}
	};
	
	//��¼ɨ��ʱ��
	private void recordStartTime(){
		startTime = System.currentTimeMillis();
	}

	private void recordEndTime(){
		endTime = System.currentTimeMillis();
	}
		
	//����û�����
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
	
	//*****ҵ����*****
	//JNI�ص�Java����
	private void updateResult(String currentScannedFileName){
		if(LocalDebug) Log.d(TAG,"JNI�ص�JAVA����updateResult()");
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
				//�Ƿ�����
				if(invalidateInput()){
					showInvalidatePathTips();
					scanning = false;
					return;
				}
				if(LocalDebug) Log.d(TAG,"��ʼɨ��");
				//�û�������
				if(TextUtils.isEmpty(pathEt.getText().toString())){
					Log.d(TAG,"ɨ��Ĭ��·��");
					if(sdcardError()){
						showInvalidatePathTips();
						scanning = false;
					}
					startScan(defaultScanPath);
				//ɨ��ָ��·��
				}else{
					if(LocalDebug) Log.d(TAG,"ɨ��ָ��·��");
					startScan(pathEt.getText().toString());
				}
				//��ʾ����ɨ����
				displayResult();
				if(LocalDebug) Log.d(TAG,"ɨ�����");
				scanning = false;
			}
		}.start();
	}
	
	//����
	//��ʼ������ɨ�����
	//��¼��ʼɨ��ʱ��
	//��¼ɨ�����ʱ��
	private void startScan(final String path){
		clearScreen();
		initLocalVarible();
		recordStartTime();
		scanFile(path);
		recordEndTime();		
	}
	
	//Native����
	private native final void scanFile(String defaultScanPath);
}
