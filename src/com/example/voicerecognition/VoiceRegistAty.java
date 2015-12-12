package com.example.voicerecognition;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicerecognition.utils.AccountInfo;
import com.example.voicerecognition.utils.AnimUtils;
import com.example.voicerecognition.utils.AudioRecordUtil;
import com.example.voicerecognition.utils.BaseLoadingView;
import com.example.voicerecognition.utils.DConfig;
import com.example.voicerecognition.utils.ErrorCode;
import com.example.voicerecognition.utils.HciCloudFuncHelper;
import com.example.voicerecognition.utils.ToastUtil;
import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.api.vpr.HciCloudVpr;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.InitParam;
import com.sinovoice.hcicloudsdk.common.Session;
import com.sinovoice.hcicloudsdk.common.vpr.VprConfig;
import com.sinovoice.hcicloudsdk.common.vpr.VprInitParam;

public class VoiceRegistAty extends Activity {

	/** �����û���Ϣ������ */
	private AccountInfo mAccountInfo;
	private final static int FLAG_WAV = 0;
	private int mState = -1; // -1:û��¼�ƣ�0��¼��wav
	private UIHandler uiHandler;
	private UIThread uiThread;
	private boolean isRegisted = false;
	public static final String REGIST_VPR_KEY = "vpr_regist";

	private Button mAudioRegistBtn;
	private Button mAudioVerifyBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addStep1Layout();
	}

	// ��һ��,��ʼ��
	private void addStep1Layout() {
		setContentView(R.layout.voice_step_1_init);
		TextView cancelTxt = (TextView) findViewById(R.id.voice_step1_cancel_txt);
		cancelTxt.setOnClickListener(cancelClickListener);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				init();
			}
		}, 1000);
	}

	private TextView mStep2SayTxt;
	private TextView mStep2ErrorTxt;
	private BaseLoadingView mStep2Loading;
	private ImageView mStep2Tip;
	private RelativeLayout mStep2SayHalo;

	// �ڶ�����ע������
	private void addStep2Layout() {
		setContentView(R.layout.voice_step_2_say);
		TextView cancelTxt = (TextView) findViewById(R.id.voice_step2_cancel_txt);
		mStep2SayTxt = (TextView) findViewById(R.id.voice_step2_say_txt);
		mStep2ErrorTxt = (TextView) findViewById(R.id.voice_step2_error_txt);
		mStep2Loading = (BaseLoadingView) findViewById(R.id.voice_step2_loading);
		mStep2Tip = (ImageView) findViewById(R.id.voice_step2_tip);
		mStep2SayHalo = (RelativeLayout) findViewById(R.id.voice_step2_say_btn_halo);
		mAudioRegistBtn = (Button) findViewById(R.id.voice_step2_say_btn);
		cancelTxt.setOnClickListener(cancelClickListener);
		mAudioRegistBtn.setOnTouchListener(audioClickListener);
	}

	// ����������һ����ʾ
	private void addStep3Layout() {
		setContentView(R.layout.voice_step_3_next);
		TextView cancelTxt = (TextView) findViewById(R.id.voice_step3_cancel_txt);
		Button nextBtn = (Button) findViewById(R.id.voice_step3_next_btn);
		cancelTxt.setOnClickListener(cancelClickListener);
		nextBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addStep4Layout();
			}
		});
	}

	// ���Ĳ�����֤����(�͵ڶ���ҳ����ͬ��ֻ��¼������ע�������֤)
	private void addStep4Layout() {
		setContentView(R.layout.voice_step_2_say);
		TextView cancelTxt = (TextView) findViewById(R.id.voice_step2_cancel_txt);
		mStep2SayTxt = (TextView) findViewById(R.id.voice_step2_say_txt);
		mStep2ErrorTxt = (TextView) findViewById(R.id.voice_step2_error_txt);
		mStep2Loading = (BaseLoadingView) findViewById(R.id.voice_step2_loading);
		mStep2Tip = (ImageView) findViewById(R.id.voice_step2_tip);
		mStep2SayHalo = (RelativeLayout) findViewById(R.id.voice_step2_say_btn_halo);
		mAudioVerifyBtn = (Button) findViewById(R.id.voice_step2_say_btn);
		cancelTxt.setOnClickListener(cancelClickListener);
		mAudioVerifyBtn.setOnTouchListener(audioClickListener);
		mStep2Tip.setVisibility(View.INVISIBLE);
	}

	// ���岽��ע�����
	private void addStep5Layout() {
		setContentView(R.layout.voice_step_4_finsh);
	}

	private void init() {
		initAudio();
		initVpr();
	}

	private void initAudio() {
		uiHandler = new UIHandler();
	}

	/**
	 * ��ʼ¼��
	 * 
	 * @param mFlag
	 *            ��0��¼��wav��ʽ��1��¼��amr��ʽ
	 */
	private void record(int mFlag) {
		if (mState != -1) {
			Message msg = new Message();
			Bundle b = new Bundle();// �������
			b.putInt("cmd", CMD_RECORDFAIL);
			b.putInt("msg", ErrorCode.E_STATE_RECODING);
			msg.setData(b);

			uiHandler.sendMessage(msg); // ��Handler������Ϣ,����UI
			return;
		}
		int mResult = -1;
		switch (mFlag) {
		case FLAG_WAV:
			AudioRecordUtil mRecord_1 = AudioRecordUtil.getInstance();
			mResult = mRecord_1.startRecordAndFile();
			break;
		}
		if (mResult == ErrorCode.SUCCESS) {
			uiThread = new UIThread();
			new Thread(uiThread).start();
			mState = mFlag;
		} else {
			Message msg = new Message();
			Bundle b = new Bundle();// �������
			b.putInt("cmd", CMD_RECORDFAIL);
			b.putInt("msg", mResult);
			msg.setData(b);

			uiHandler.sendMessage(msg); // ��Handler������Ϣ,����UI
		}
	}

	/**
	 * ֹͣ¼��
	 */
	private void stopRecord() {
		if (mState != -1) {
			switch (mState) {
			case FLAG_WAV:
				AudioRecordUtil mRecord_1 = AudioRecordUtil.getInstance();
				mRecord_1.stopRecordAndFile();
				break;
			}
			if (uiThread != null) {
				uiThread.stopThread();
			}
			if (uiHandler != null)
				uiHandler.removeCallbacks(uiThread);

			mState = -1;
		}
	}

	private final static int CMD_RECORDING_TIME = 2000;
	private final static int CMD_RECORDFAIL = 2001;
	private final static int CMD_STOP = 2002;

	private int recordTime;

	class UIHandler extends Handler {
		public UIHandler() {
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle b = msg.getData();
			int vCmd = b.getInt("cmd");
			switch (vCmd) {
			case CMD_RECORDING_TIME:
				recordTime = b.getInt("msg");
				// record_txt.setText("����¼���У���¼�ƣ�" + vTime + " s");
				break;
			case CMD_RECORDFAIL:
				int vErrorCode = b.getInt("msg");
				String vMsg = ErrorCode.getErrorInfo(VoiceRegistAty.this, vErrorCode);
				// record_txt.setText("¼��ʧ�ܣ�" + vMsg);
				setLoading(false);
				break;
			default:
				break;
			}
		}
	};

	class UIThread implements Runnable {
		int mTimeMill = 0;
		boolean vRun = true;

		public void stopThread() {
			vRun = false;
		}

		public void run() {
			while (vRun) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mTimeMill++;
				Log.d("thread", "mThread........" + mTimeMill);
				Message msg = new Message();
				Bundle b = new Bundle();// �������
				b.putInt("cmd", CMD_RECORDING_TIME);
				b.putInt("msg", mTimeMill);
				msg.setData(b);

				VoiceRegistAty.this.uiHandler.sendMessage(msg); // ��Handler������Ϣ,����UI
			}

		}
	}

	private void initVpr() {

		mAccountInfo = AccountInfo.getInstance();
		boolean loadResult = mAccountInfo.loadAccountInfo(this);
		if (loadResult) {
		} else {
			ToastUtil.showToast(this, "���������˺�ʧ�ܣ�����assets/AccountInfo.txt�ļ�����д��ȷ�������˻���Ϣ���˻���Ҫ��www.hcicloud.com������������ע�����롣", Toast.LENGTH_SHORT);
			return;
		}

		// ������Ϣ,����InitParam, ������ò������ַ���
		InitParam initParam = HciCloudFuncHelper.getInitParam(this);
		String strConfig = initParam.getStringConfig();

		// ��ʼ��
		int errCode = HciCloudSys.hciInit(strConfig, this);
		if (errCode != HciErrorCode.HCI_ERR_NONE && errCode != HciErrorCode.HCI_ERR_SYS_ALREADY_INIT) {
			ToastUtil.showToast(this, "\nhciInit error: " + HciCloudSys.hciGetErrorInfo(errCode), Toast.LENGTH_SHORT);
			Log.i("aaaaaaaaaaaaaaaaaaaaaa", "������"+ errCode);
			return;
		} else {
//			ToastUtil.showToast(this, "\nhciInit error: " + HciCloudSys.hciGetErrorInfo(errCode), Toast.LENGTH_SHORT);
			Log.i("aaaaaaaaaaaaaaaaaaaaaa", "������"+ errCode);
		}

		// ��ȡ��Ȩ/������Ȩ�ļ� :
		errCode = HciCloudFuncHelper.checkAuthAndUpdateAuth();
		if (errCode != HciErrorCode.HCI_ERR_NONE) {
			// ����ϵͳ�Ѿ���ʼ���ɹ�,�ڽ���ǰ��Ҫ���÷���hciRelease()����ϵͳ�ķ���ʼ��
			 HciCloudSys.hciRelease();
			return;
		}else{
			addStep2Layout();
		}
		// HciCloudFuncHelper.Func(this, mAccountInfo.getCapKey(), mLogView);
		
		
		// ��ʼ��VPR
		// ����VPR��ʼ���İ������ʵ��
		VprInitParam vprInitParam = new VprInitParam();
		// ��ȡAppӦ���е�lib��·��,��������������Դ�ļ������ʹ��/data/data/packagename/libĿ¼,��Ҫ���android_so�ı��
//		String dataPath = context.getFilesDir().getAbsolutePath().replace("files", "lib");
//		initParam.addParam(VprInitParam.PARAM_KEY_DATA_PATH, dataPath);
		initParam.addParam(VprInitParam.PARAM_KEY_FILE_FLAG, VprInitParam.VALUE_OF_PARAM_FILE_FLAG_ANDROID_SO);
		initParam.addParam(VprInitParam.PARAM_KEY_INIT_CAP_KEYS, "vpr.cloud.verify");
//		ShowMessage("HciVprInit config :" + initParam.getStringConfig());
		int vprErrCode = HciCloudVpr.hciVprInit(initParam.getStringConfig());
		if (vprErrCode != HciErrorCode.HCI_ERR_NONE) {
//			ShowMessage("HciVprInit error:"	+ HciCloudSys.hciGetErrorInfo(errCode));
			Log.i("aaaaaaaaaaaaaaaaaaaa", "�����룺"+vprErrCode);
			return;
		} else {
			Log.i("aaaaaaaaaaaaaaaaaaa", "HciVprInit Success");
		}
		
		
	}

	/** ע������ */
	private void registVoice() {
		String capKey = mAccountInfo.getCapKey();
		final StringBuffer userId = new StringBuffer("wangyue8");

		boolean enrollResult = false;
		VprConfig enrollConfig = new VprConfig();
		enrollConfig.addParam(VprConfig.UserConfig.PARAM_KEY_USER_ID, "wangyue8");
		enrollConfig.addParam(VprConfig.AudioConfig.PARAM_KEY_AUDIO_FORMAT, VprConfig.AudioConfig.VALUE_OF_PARAM_AUDIO_FORMAT_PCM_16K16BIT);
		
//		VprConfig sessionConfig = new VprConfig();
//		sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_CAP_KEY,
//				"vpr.cloud.verify");
//		Session session = new Session();
//		int errCode = HciCloudVpr.hciVprSessionStart(sessionConfig.getStringConfig(), session);
//		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			Log.i("aaaaaaaaaaaaaaaaaaaa", "�����룺"+errCode);
//			return ;
//		}
		
		enrollResult = HciCloudFuncHelper.Enroll(capKey, enrollConfig);
		
		
		if (enrollResult) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					isRegisted = true;
					setLoading(false);
					addStep3Layout();
					// Toast.makeText(getApplicationContext(), "ע��ɹ���userid:" +
					// userId.toString(), 1).show();
				}
			});
			AudioRecordUtil.getInstance().removePcmFile();
			DConfig.Preference.setStringPref(this, REGIST_VPR_KEY, userId.toString());
		} else {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					isRegisted = false;
					setLoading(false);
					Toast.makeText(getApplicationContext(), "ע��ʧ��", 1).show();
				}
			});
			return;
		}
//		HciCloudVpr.hciVprSessionStop(session);
	}

	private void verifyVoice() {
		boolean verifyResult = false;
		VprConfig verifyConfig = new VprConfig();
		verifyConfig.addParam(VprConfig.UserConfig.PARAM_KEY_USER_ID, "wangyue8");
//		HciCloudFuncHelper.Verify(mAccountInfo.getAppKey(), verifyConfig);
		verifyResult = HciCloudFuncHelper.Verify("vpr.cloud.verify", verifyConfig);
		setLoading(false);
		if (verifyResult) {
			AudioRecordUtil.getInstance().removePcmFile();
			ToastUtil.showToast(this, "����ʶ��ɹ�", 1);
			addStep5Layout();
		} else {
			AudioRecordUtil.getInstance().removePcmFile();
			ToastUtil.showToast(this, "����ʶ��ʧ��", 1);
		}

	}

	private OnClickListener cancelClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			finish();
		}
	};

	private OnTouchListener audioClickListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				recordStart();
				record(FLAG_WAV);
				break;
			case MotionEvent.ACTION_UP:
				recordFinish();
				stopRecord();
				if (recordTime <= 1) {
					mStep2ErrorTxt.setVisibility(View.VISIBLE);
					mStep2ErrorTxt.setText("¼��ʱ��̫��");
					setLoading(false);
				} else {
					mStep2ErrorTxt.setVisibility(View.INVISIBLE);
					if (isRegisted) {
						verifyVoice();
					} else {
						registVoice();
					}
				}

				break;

			case MotionEvent.ACTION_CANCEL:
				recordFinish();
				setLoading(false);
				mStep2ErrorTxt.setVisibility(View.VISIBLE);
				mStep2ErrorTxt.setText("¼��ʱ��̫��");
				stopRecord();
				break;
			default:
				break;
			}
			return false;
		}
	};

	private void recordStart() {
		mStep2ErrorTxt.setVisibility(View.INVISIBLE);
		mStep2Tip.setVisibility(View.INVISIBLE);
		mStep2SayHalo.setVisibility(View.VISIBLE);
		setLoading(false);
		AnimUtils.animVoiceBtnScale(mStep2SayHalo);
	}

	private void recordFinish() {
		setLoading(true);
		mStep2SayHalo.setVisibility(View.INVISIBLE);
	}

	private void setLoading(boolean isVisible) {
		if (isVisible) {
			mStep2Loading.setVisibility(View.VISIBLE);
		} else {
			mStep2Loading.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected void onDestroy() {
		// �ͷ�HciCloudSys������������ȫ���ͷ���Ϻ󣬲��ܵ���HciCloudSys���ͷŷ���
		HciCloudVpr.hciVprRelease();
		HciCloudSys.hciRelease();
		super.onDestroy();
	}

}
