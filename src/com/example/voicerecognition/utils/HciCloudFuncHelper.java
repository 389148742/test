package com.example.voicerecognition.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.api.HciCloudUser;
import com.sinovoice.hcicloudsdk.api.vpr.HciCloudVpr;
import com.sinovoice.hcicloudsdk.common.AuthExpireTime;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.InitParam;
import com.sinovoice.hcicloudsdk.common.Session;
import com.sinovoice.hcicloudsdk.common.vpr.VprConfig;
import com.sinovoice.hcicloudsdk.common.vpr.VprEnrollResult;
import com.sinovoice.hcicloudsdk.common.vpr.VprEnrollVoiceData;
import com.sinovoice.hcicloudsdk.common.vpr.VprEnrollVoiceDataItem;
import com.sinovoice.hcicloudsdk.common.vpr.VprIdentifyResult;
import com.sinovoice.hcicloudsdk.common.vpr.VprInitParam;
import com.sinovoice.hcicloudsdk.common.vpr.VprVerifyResult;

public class HciCloudFuncHelper {
	private static final String TAG = HciCloudFuncHelper.class.getSimpleName();

	/**
	 * ���س�ʼ����Ϣ
	 * 
	 * @param context
	 *            �������ﾳ
	 * @return ϵͳ��ʼ������
	 */
	public static InitParam getInitParam(Context context) {
		String authDirPath = context.getFilesDir().getAbsolutePath();

		// ǰ����������
		InitParam initparam = new InitParam();
		// ��Ȩ�ļ�����·�����������
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_AUTH_PATH, authDirPath);
		// �����Ʒ���Ľӿڵ�ַ���������
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_CLOUD_URL, AccountInfo.getInstance().getCloudUrl());
		// ������Key���������ɽ�ͨ�����ṩ
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_DEVELOPER_KEY, AccountInfo.getInstance().getDeveloperKey());
		// Ӧ��Key���������ɽ�ͨ�����ṩ
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_APP_KEY, AccountInfo.getInstance().getAppKey());

		// ������־����
		String sdcardState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(sdcardState)) {
			String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
			String packageName = context.getPackageName();

			String logPath = sdPath + File.separator + "sinovoice" + File.separator + packageName + File.separator + "log" + File.separator;

			// ��־�ļ���ַ
			File fileDir = new File(logPath);
			if (!fileDir.exists()) {
				fileDir.mkdirs();
			}

			// ��־��·������ѡ�������������Ϊ����������־
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_PATH, logPath);
			// ��־��Ŀ��Ĭ�ϱ������ٸ���־�ļ��������򸲸���ɵ���־
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_COUNT, "5");
			// ��־��С��Ĭ��һ����־�ļ�д��󣬵�λΪK
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_SIZE, "1024");
			// ��־�ȼ���0=�ޣ�1=����2=���棬3=��Ϣ��4=ϸ�ڣ�5=���ԣ�SDK�����С�ڵ���logLevel����־��Ϣ
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_LEVEL, "5");
		}

		return initparam;
	}

	/**
	 * ��ȡ��Ȩ
	 * 
	 * @return true �ɹ�
	 */
	public static int checkAuthAndUpdateAuth() {

		// ��ȡϵͳ��Ȩ����ʱ��
		int initResult;
		AuthExpireTime objExpireTime = new AuthExpireTime();
		initResult = HciCloudSys.hciGetAuthExpireTime(objExpireTime);
		if (initResult == HciErrorCode.HCI_ERR_NONE) {
			// ��ʾ��Ȩ����,���û�����Ҫ��ע��ֵ,�˴�����ɺ���
			Date date = new Date(objExpireTime.getExpireTime() * 1000);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
			Log.i(TAG, "expire time: " + sdf.format(date));

			if (objExpireTime.getExpireTime() * 1000 > System.currentTimeMillis()) {
				// �Ѿ��ɹ���ȡ����Ȩ,���Ҿ�����Ȩ�����г����ʱ��(>7��)
				Log.i(TAG, "checkAuth success");
				return initResult;
			}

		}

		// ��ȡ����ʱ��ʧ�ܻ����Ѿ�����
		initResult = HciCloudSys.hciCheckAuth();
		if (initResult == HciErrorCode.HCI_ERR_NONE) {
			Log.i(TAG, "checkAuth success");
			return initResult;
		} else {
			Log.e(TAG, "checkAuth failed: " + initResult);
			return initResult;
		}
	}

	/*
	 * VPRע���ѵ��
	 */
	public static boolean Enroll(String capkey, VprConfig enrollConfig) {
		// ��װ��Ƶ������һ�δ�������Ƶ
		int nEnrollDataCount = 1;
		int nIndex = 0;
		ArrayList<VprEnrollVoiceDataItem> enrollVoiceDataList = new ArrayList<VprEnrollVoiceDataItem>();

		for (; nIndex < nEnrollDataCount; nIndex++) {
			byte[] voiceData = getPcmFileData();
			if (null == voiceData) {
//				ShowMessage("Open input voice file" + voiceDataName + "error!");
				break;
			}
			VprEnrollVoiceDataItem voiceDataItem = new VprEnrollVoiceDataItem();
			voiceDataItem.setVoiceData(voiceData);
			enrollVoiceDataList.add(voiceDataItem);
		}
		if (nIndex <= 0) {
//			ShowMessage("no enroll data found in assets folder!");
			return false;
		}

		// ���� VPR Session
		VprConfig sessionConfig = new VprConfig();
		sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);
		// ����ʵ�����ָ����Դǰ׺
		if (capkey.contains("local")) {
			sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_RES_PREFIX, "16k_");
		}

		Session session = new Session();
		int errCode = HciCloudVpr.hciVprSessionStart(sessionConfig.getStringConfig(), session);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("hciVprSessionStart return " + errCode);
			return false;
		}

		// VPR ע��
		VprEnrollVoiceData enrollVoiceData = new VprEnrollVoiceData();
		enrollVoiceData.setEnrollVoiceDataCount(nEnrollDataCount);
		enrollVoiceData.setEnrollVoiceDataList(enrollVoiceDataList);
		VprEnrollResult enrollResult = new VprEnrollResult();
		errCode = HciCloudVpr.hciVprEnroll(session, enrollVoiceData, enrollConfig.getStringConfig(), enrollResult);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
			// ����ʧ��
			HciCloudVpr.hciVprSessionStop(session);
//			ShowMessage("hciVprEnroll return " + errCode);
			return false;
		}

		// �ر�session
		HciCloudVpr.hciVprSessionStop(session);
		return true;
	}

	/*
	 * VPR ȷ�ϣ�Verify��
	 */
	public static boolean Verify(String capkey, VprConfig verifyConfig) {
		byte[] voiceDataVerify = getPcmFileData();
		if (null == voiceDataVerify) {
//			ShowMessage("Open input voice file " + voiceDataName + " error!");
			return false;
		}

		// ���� VPR Session
		VprConfig sessionConfig = new VprConfig();
		sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);
		// ����ʵ�����ָ����Դǰ׺
		if (capkey.contains("local")) {
			sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_RES_PREFIX, "16k_");
		}

		Session session = new Session();
		int errCode = HciCloudVpr.hciVprSessionStart(sessionConfig.getStringConfig(), session);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("hciVprSessionStart return " + errCode);
			return false;
		}

		// ��ʼУ��
		VprVerifyResult verifyResult = new VprVerifyResult();
		errCode = HciCloudVpr.hciVprVerify(session, voiceDataVerify, verifyConfig.getStringConfig(), verifyResult);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("Hcivpr hciVprVerify return " + errCode);
			HciCloudVpr.hciVprSessionStop(session);
			return false;
		}

		if (verifyResult.getStatus() == VprVerifyResult.VPR_VERIFY_STATUS_MATCH) {
			
			
		} else {
//			ShowMessage("voice data doesn't match with user_id !");
		}

		HciCloudVpr.hciVprSessionStop(session);
		return true;
	}

	/*
	 * VPR ��ʶ��Identify��
	 */
	public static boolean Identify(String capkey, VprConfig identifyConfig) {

		byte[] voiceDataVerify = getPcmFileData();
		if (null == voiceDataVerify) {
//			ShowMessage("Open input voice file " + voiceDataName + " error!");
			return false;
		}

		// ���� VPR Session
		VprConfig sessionConfig = new VprConfig();
		sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);
		// ����ʵ�����ָ����Դǰ׺
		if (capkey.contains("local")) {
			sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_RES_PREFIX, "16k_");
		}

		Session session = new Session();
		int errCode = HciCloudVpr.hciVprSessionStart(sessionConfig.getStringConfig(), session);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("Hcivpr hciVprSessionStart return " + errCode);
			return false;
		}

		// ��ʶ
		VprIdentifyResult identifyResult = new VprIdentifyResult();
		errCode = HciCloudVpr.hciVprIdentify(session, voiceDataVerify, identifyConfig.getStringConfig(), identifyResult);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("Hcivpr hciVprIdentify return " + errCode);
			HciCloudVpr.hciVprSessionStop(session);
			return false;
		}

		HciCloudVpr.hciVprSessionStop(session);
		return true;
	}
	
	
	
	
	
	public static byte[] getPcmFileData() {
		InputStream in = null;
		int size = 0;
		try {
			in = new FileInputStream(AudioRecordUtil.getInstance().getAudioPcmFile());
			size = in.available();
			byte[] data = new byte[size];
			in.read(data, 0, size);

			return data;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	
}
