package com.lcinshu.demo_lcinshu;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.server.converter.ConverterWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.speech.RecognizerListener;
import com.iflytek.speech.RecognizerResult;
import com.iflytek.sunflower.FlowerCollector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,CompoundButton.OnCheckedChangeListener{

    public static final String TAG = MainActivity.class.getSimpleName();
    private int ONLINE_ORNOT = -1;
    private String resultJSON;

    private RecognizerDialog recognizerDialog;
    private SpeechRecognizer mIat;

    private Button button ;
    private EditText editText;
    private RadioButton radioButton;
    private TextView textView;
    private ProgressDialog pDialog;
    private AlertDialog.Builder alertbuilder;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);
        //初始化,并检测服务是否安装
        initDemo(this,mInitListener);
        checkService(this);

        editText = (EditText)findViewById(R.id.speech_content);
        textView = (TextView)findViewById(R.id.lat_category_sign);

        button = (Button)findViewById(R.id.speech_start);
        button.setOnClickListener(this);
        button = (Button)findViewById(R.id.clear_edittext);
        button.setOnClickListener(this);

        radioButton = (RadioButton)findViewById(R.id.radiobt_offline);
        radioButton.setOnCheckedChangeListener(this);
        radioButton = (RadioButton)findViewById(R.id.radiobt_online);
        radioButton.setOnCheckedChangeListener(this);

    }

    //一共有四个控件需要添加监听事件，两个按钮，两个单选按钮，采用实现多接口的方式
    /**
     * 设置开始语音和清空内容监听事件，语音识别在这里实现，辅助以网络及是否选择在线判断。
     * @param view
     */
    public void onClick(View view){
        switch (view.getId()){
            case R.id.speech_start: {
                //测试代码
                //editText.append("aaaa");
                if(ONLINE_ORNOT == 1) {
                    if(isNetworkConnected(this)) {
                        //在线识别且有网络连接
                        boolean isCheckServerFinished = checkService(this);
                        if (isCheckServerFinished)
                            startSpeechwithUI();
                    }else{
                        //在线识别但无网络连接
                        showTip("您的网络未连接，请连接网络后重试");
                    }
                }
                else if(ONLINE_ORNOT == 0){
                    //离线识别
                    boolean isCheckServerFinished = checkService(this);
                    if (isCheckServerFinished)
                        startSpeechwithUI();
                }
                else {
                    //未选择引擎
                    showTip("请先选择语音引擎类型，再录音");
                }
                break;
            }
            case R.id.clear_edittext:
                editText.setText(null);
                break;
            default:
                break;
        }
    }

    /**
     * 设置本地和离线单选按钮的监听事件，这里主要设置
     * @param group
     * @param checkedId
     */
    public void onCheckedChanged(CompoundButton group, boolean checkedId){

        switch (group.getId()){
            case R.id.radiobt_online: {
                ONLINE_ORNOT = 0;
                textView.setText("引擎需要下载讯飞语记");
                break;
            }
            case R.id.radiobt_offline: {
                ONLINE_ORNOT = 1;
                textView.setText("引擎需要连接网络，请检查网络是否连接");
                break;
            }
            default:
                break;
        }
    }

    /**
     * 初始化引擎
     * @param context
     * @param initListener
     */
    protected void initDemo(Context context,InitListener initListener){
        SpeechUtility.createUtility(MainActivity.this,
                SpeechConstant.APPID + "=584f6073");
        //1.创建SpeechRecognizer对象，第二个参数：本地听写时传InitListener
        mIat = SpeechRecognizer.createRecognizer(context, initListener);
        recognizerDialog = new RecognizerDialog(context,initListener);
        //speechRecognizer = SpeechRecognizer.createRecognizer(context,initListener);
        //设置听写引擎
        //mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        mIat.setParameter(SpeechConstant.DOMAIN, "iat");
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        //设置返回结果格式,默认为json
        //speechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin ");
        mIat.setParameter(SpeechConstant.ASR_PTT, ",");
    }

    /**
     * 在线带UI语音识别
     */
    private void startSpeechwithUI(){
        resultJSON = "[";

        recognizerDialog.setListener(new RecognizerDialogListener() {
            @Override
            public void onResult(com.iflytek.cloud.RecognizerResult
                                         recognizerResult, boolean isLast) {

                if (!isLast) {
                    resultJSON += recognizerResult.getResultString() + ",";
                } else {
                    resultJSON += recognizerResult.getResultString() + "]";
                    //离线识别，正则表达式
                    if (ONLINE_ORNOT == 0) {
                        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
                        String regEx = "[\u4e00-\u9fa5]";
                        // 编译正则表达式
                        Pattern pattern = Pattern.compile(regEx);
                        // 忽略大小写的写法
                        // Pattern pat = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
                        Matcher matcher = pattern.matcher(resultJSON);
                        // 字符串是否与正则表达式相匹配
                        List<String> result = new ArrayList<>();
                        while (matcher.find()) {
                            result.add(matcher.group());
                        }
                        for (String s : result) {
                            editText.append(s);
                            Log.d(TAG, s);
                            //Log.d(TAG,resultJSON);
                        }
                    } else {
                        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
                        Gson gson = new Gson();
                        List<DictationResult> dictationResultList =
                                gson.fromJson(resultJSON,
                                        new TypeToken<List<DictationResult>>() {
                                        }.getType());
                        String finalResult = "";
                        for (int i = 0; i < dictationResultList.size() - 1; i++) {
                            finalResult += dictationResultList.get(i).toString();
                        }
                        editText.append(finalResult);
                        editText.requestFocus();
                        editText.setSelection(editText.getText().length());
                    }
                }
            }
            @Override
            public void onError(SpeechError speechError) {

            }
        });
        recognizerDialog.show();
    }

    /**
     * 检测网络状态，用于逻辑判断
     * @param context
     * @return
     */
    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    /**
     * 检测是否安装了语记
     * @param context
     */
    private boolean checkService(final Context context){
        if(!SpeechUtility.getUtility().checkServiceInstalled ()) {
            if (isNetworkConnected(this)) {
                alertbuilder = new AlertDialog.Builder(context);
                alertbuilder.setTitle("注意！");
                alertbuilder.setMessage("系统检测到您尚未安装语音引擎，需要安装讯飞语记？");
                alertbuilder.setCancelable(false);

                alertbuilder.setPositiveButton("欣然同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = SpeechUtility.getUtility().getComponentUrl();
                        Uri uri = Uri.parse(url);

                        Intent it = new Intent(Intent.ACTION_VIEW, uri);
                        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(it);
                    }
                });

                alertbuilder.setNegativeButton("残忍拒绝", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alertbuilder.show();
            }else{
                showTip("离线识别需要下载本地引擎，请连接网络后重试");
            }
            return false;
        }else{
            return true;
        }
    }

    /**
     * 初始化引擎
     */
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Toast.makeText(MainActivity.this, "初始化失败，错误码："
                        + code, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * 提示弹窗
     * @param string
     */
    private void showTip(final String string){
        mToast.setText(string);
        mToast.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FlowerCollector.onResume(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        FlowerCollector.onPause(this);
    }

    /**
     * 自定义圆形进度条
     */
//    private void showProgressDialog() {
//        pDialog = new ProgressDialog(MainActivity.this);
//
//        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        pDialog.setProgress(100);
//        pDialog.setMessage("请稍等...");
//        pDialog.setIndeterminate(false);
//        pDialog.show();
//    }

    /**
     * 离线不带UI语音识别，添加自定义进度圆条,经过验证连无法识别原因在于json数据格式不同，采用和
     * 在线识别相同的解析方法无法解析，并非是引擎的原因。因此语音识别不需要分在线和离线，只需在
     * 识别的时候选择相应的引擎和数据解析方法即可。用的都是官方带UI的识别方法RecognizerDialog
     *
     */
//    private void startOfflinewithoutUI(){
//        resultJSON = "[";
//
//        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
//        speechRecognizer = SpeechRecognizer.createRecognizer(this,mInitListener);
//        speechRecognizer.startListening(
//                new com.iflytek.cloud.RecognizerListener() {
//
//            @Override
//            public void onResult(com.iflytek.cloud.RecognizerResult
//                                         recognizerResult, boolean isLast) {
//                if(!isLast){
//                    resultJSON += recognizerResult.getResultString() +",";
//                }
//                else{
//                    resultJSON += recognizerResult.getResultString() +"]";
//                    //editText.setText(resultJSON);
//                    //Log.d(TAG,resultJSON);
//
//                    //采用正则表达式进行解析,这里只是匹配查找，无法去除文字后的句号
//                    String regEx = "[\u4e00-\u9fa5]";
//                    // 编译正则表达式
//                    Pattern pattern = Pattern.compile(regEx);
//                    // 忽略大小写的写法
//                    // Pattern pat = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
//                    Matcher matcher = pattern.matcher(resultJSON);
//                    // 字符串是否与正则表达式相匹配
//                    List<String> result = new ArrayList<>();
//                    while (matcher.find()){
//                        result.add(matcher.group());
//                    }for (String s:result) {
//                        editText.append(s);
//                        Log.d(TAG,s);
//                        //去掉结尾的句号
////                        if(s == "\u3002"){}
////                        else {
////                            editText.append(s);
////                            Log.d(TAG, s);
////                        }
//                    }
////                    用Gson解析数据，但是本地识别返回的json数据格式与云端数据不同，解析无果
////                    Gson gson = new Gson();
////                    List<DicationResult_offline> dictationResultList =
////                            gson.fromJson(resultJSON,
////                                    new TypeToken<List<DicationResult_offline>>()
////                                    {}.getType());
////                    String finalResult = "";
////                    for (int i = 0; i < dictationResultList.size() - 1; i++) {
////                        Log.d(TAG,dictationResultList.get(i).toString());
////                        finalResult += dictationResultList.get(i).toString();
////                    }
////                    editText.append(finalResult+"aaaaaaaa");
////                    editText.requestFocus();
////                    editText.setSelection(editText.getText().length());
////                    editText.append("aaa");
//                }
//                pDialog.dismiss();
//            }
//
//            @Override
//            public void onVolumeChanged(int i, byte[] bytes) {
//
//            }
//
//            @Override
//            public void onBeginOfSpeech() {
//                Log.d(TAG,"开始语音");
//            }
//
//            @Override
//            public void onEndOfSpeech() {
//                Log.d(TAG,"结束语音");
//            }
//
//            @Override
//            public void onError(SpeechError speechError) {
//
//            }
//
//            @Override
//            public void onEvent(int i, int i1, int i2, Bundle bundle) {
//
//            }
//        });
//    }

}
