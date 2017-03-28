package com.videoviewdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;


public class MainActivity extends AppCompatActivity {
    //那些没时间娱乐的人，他们的时间会花在生病上。——约翰 沃纳梅克
    private CosVideoView videoView;
    private ImageView imageView;
    private ImageView play_controller_img;
    private TextView mCurrent,mTotal;
    private SeekBar mSeekBar,seekbar_volum;
    public static final int UPDATE = 1;
    private int screen_width,screen_height;
    private RelativeLayout rl;
    private AudioManager manager;
    private boolean isFullScreen = false;
    private boolean isAdjust = false;//判断是不是误触
    private int threshold = 54;
    private float lastX = 0;
    private float lastY = 0;
    private float mBrightness;
    private ImageView operation_bg,operation_percent;
    private FrameLayout progress_layout;

    //////////////////////////////////
    private SensorManager sm,sm1;
    private Sensor sensor,sensor1;
    private OrientationSensorListener listener;
    private OrientationSensorListener2 listener1;
    private boolean sensor_flag = true;
    private boolean stretch_flag = true;
    private VolumeReceiver mVolumeReceiver;


    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 888:
                    int orientation = msg.arg1;
                    if (orientation > 45 && orientation < 135) {

                    } else if (orientation > 135 && orientation < 225) {

                    } else if (orientation > 225 && orientation < 315) {
                        System.out.println("切换成横屏");
                        setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
                        sensor_flag = false;
                        stretch_flag = false;

                    } else if ((orientation > 315 && orientation < 360) || (orientation > 0 && orientation < 45)) {
                        System.out.println("切换成竖屏");
                        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
                        sensor_flag = true;
                        stretch_flag = true;
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private int currentPosition;
    private int totalduration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        manager = (AudioManager) getSystemService(AUDIO_SERVICE);//获取音频服务
        initUI();
        setPlayerEvent();
        String path = "http://baobab.wandoujia.com/api/v1/playUrl?vid=2614&editionType=normal";
        videoView.setVideoURI(Uri.parse(path));
        videoView.start();
        UIHandler.sendEmptyMessage(UPDATE);
    }

    //设置时间
    private void updateTextViewWithTimeFormat(TextView textView,long time){
        textView.setText(formatTimes(time));
    }

    public static String formatTimes(long time){
        return String.format("%02d", time / 3600) + ":"
                + String.format("%02d", time % 3600 / 60) + ":"
                + String.format("%02d", time % 3600 % 60);

    }

    //
    private Handler UIHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==UPDATE) {
                //获取视频当前的播放时间
                currentPosition = videoView.getCurrentPosition();
                //获取视频播放的总时间
                totalduration = videoView.getDuration();
                //格式化视频播放时间
                updateTextViewWithTimeFormat(mCurrent, currentPosition /1000);
                updateTextViewWithTimeFormat(mTotal, totalduration /1000);

                mSeekBar.setMax(totalduration);
                mSeekBar.setProgress(currentPosition);

                UIHandler.sendEmptyMessageDelayed(UPDATE,500);

            }

        }
    };


    private void setPlayerEvent() {
        play_controller_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(videoView.isPlaying()) {
                    play_controller_img.setImageResource(R.mipmap.cd_icon_bofa01);
                    //暂停播放
                    videoView.pause();
                    UIHandler.removeMessages(UPDATE);
                }else {
                    play_controller_img.setImageResource(R.mipmap.cd_icon_bofa02);
                    //继续播放
                    videoView.start();
                    UIHandler.sendEmptyMessage(UPDATE);
                }
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTextViewWithTimeFormat(mCurrent,progress/1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                UIHandler.removeMessages(UPDATE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = mSeekBar.getProgress();
                //视频播放进度遵循seekbar停止拖动时的进度
                videoView.seekTo(progress);
                UIHandler.sendEmptyMessage(UPDATE);
            }
        });

        seekbar_volum.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //设置当前设备的音量
                //类型 音量大小 标记
                manager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFullScreen) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                play_controller_img.setImageResource(R.mipmap.cd_icon_bofa01);
                UIHandler.removeMessages(UPDATE);
            }
        });

        //控制videoView手势事件
        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //触摸时手势在x和y的位置
                float x = event.getX();
                float y = event.getY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN :
                        //手指落在屏幕那一刻（只会调用一次）
                        lastX = x;
                        lastY = y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        //手指在屏幕上移动（调用多次）
                        float detlaX = x - lastX;
                        float detlaY = y - lastY;
                        //计算手指滑动时X轴和Y轴偏移量的绝对值
                        float absdetlaX = Math.abs(detlaX);
                        float absdetlaY = Math.abs(detlaY);

                        if(absdetlaX>threshold&&absdetlaY>threshold) {
                            if(absdetlaX<absdetlaY) {
                                isAdjust = true;
                            }else {
                                isAdjust = false;
                            }
                        }else if(absdetlaX<threshold&&absdetlaY>threshold) {
                            isAdjust = true;
                        }else if(absdetlaX>threshold&&absdetlaY<threshold) {
                            isAdjust = false;
                        }
                        
                        if(isAdjust) {
                            //在判断好当前手势事件已经合法的前提下，去区分此时手势应该调节亮度还是声音
                            if(x<screen_width/2) {
                                //调节亮度
                                changeVolume(-detlaY);

                            }else {
                                changeBrightness(-detlaY);
                            }
                        }

                        lastX = x;
                        lastY = y;
                        
                        break;
                    case MotionEvent.ACTION_UP:
                        //手指离开屏幕那一刻（只会调用一次）
                        lastX = 0;
                        lastY = 0;
                        progress_layout.setVisibility(View.GONE);
                        break;
                }
                return true;
            }
        });
    }


    //手势改变声音大小
    private void changeVolume(float detlaY){
        int max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int index = (int)(detlaY/screen_height*max*3);//偏移量
        int volume = Math.max(current+index,0);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);

        if(progress_layout.getVisibility()==View.GONE) {
            progress_layout.setVisibility(View.VISIBLE);
        }
        operation_bg.setImageResource(R.mipmap.ic_launcher);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width = (int)(PixelUtils.dp2px(94)*(float)volume/max);
        operation_percent.setLayoutParams(layoutParams);

        seekbar_volum.setProgress(volume);

    }

    //手势改变屏幕亮度
    public void changeBrightness(float detlaY){
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        mBrightness = attributes.screenBrightness;//该属性控制屏幕的亮度0~1
        float index = detlaY/screen_height;
        mBrightness +=index;
        if(mBrightness>1.0f) {
            mBrightness = 1.0f;
        }
        if(mBrightness<0.01f) {
            mBrightness=0.01f;
        }
        attributes.screenBrightness = mBrightness;


        if(progress_layout.getVisibility()==View.GONE) {
            progress_layout.setVisibility(View.VISIBLE);
        }
        operation_bg.setImageResource(R.mipmap.ic_launcher);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width = (int)(PixelUtils.dp2px(94)*mBrightness);
        operation_percent.setLayoutParams(layoutParams);


        getWindow().setAttributes(attributes);
    }

    private void initUI() {
        mVolumeReceiver = new VolumeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(mVolumeReceiver, intentFilter);// 注册声音广播接受者

        PixelUtils.initContext(this);
        videoView = (CosVideoView) findViewById(R.id.videoview);
        imageView = (ImageView)findViewById(R.id.change);
        play_controller_img = (ImageView)findViewById(R.id.player);
        mCurrent = (TextView)findViewById(R.id.tv_current);
        mTotal = (TextView)findViewById(R.id.tv_total);
        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        seekbar_volum = (SeekBar)findViewById(R.id.seekbar_volum);
        rl = (RelativeLayout)findViewById(R.id.rl);
        operation_bg = (ImageView)findViewById(R.id.operation_bg);
        operation_percent = (ImageView)findViewById(R.id.operation_percent);
        progress_layout = (FrameLayout)findViewById(R.id.progress_layout);
        screen_width = getResources().getDisplayMetrics().widthPixels;
        screen_height = getResources().getDisplayMetrics().heightPixels;
        //当前设备最大音量
        int streamMaxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //获取当前音量
        int streamVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekbar_volum.setMax(streamMaxVolume);
        seekbar_volum.setProgress(streamVolume);

        //////////////////////////////////////////////
        //注册重力感应器  屏幕旋转
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        listener = new OrientationSensorListener(handler);
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);


        //根据旋转之后点击符合之后 激活sm
        sm1 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor1 = sm1.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        listener1 = new OrientationSensorListener2();
        sm1.registerListener(listener1, sensor1, SensorManager.SENSOR_DELAY_UI);
    }

    private void setVideoViewScale(int width ,int height){
        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        videoView.setLayoutParams(layoutParams);

        ViewGroup.LayoutParams layoutParams1 = rl.getLayoutParams();
        layoutParams1.width = width;
        layoutParams1.height = height;
        rl.setLayoutParams(layoutParams1);
    }


    //监听屏幕方向的改变
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //横屏
        if(getResources().getConfiguration().orientation ==Configuration.ORIENTATION_LANDSCAPE) {
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
            isFullScreen = true;
            //
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }else {
        //竖屏
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,PixelUtils.dp2px(200));
            isFullScreen = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView.canSeekForward() && currentPosition != 0) {
            videoView.seekTo(currentPosition);
            play_controller_img.setImageResource(R.mipmap.cd_icon_bofa02);
            UIHandler.sendEmptyMessage(UPDATE);
            videoView.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.canPause()) {
            play_controller_img.setImageResource(R.mipmap.cd_icon_bofa01);
            currentPosition = videoView.getCurrentPosition();
            videoView.pause();
        }
        if (UIHandler.hasMessages(UPDATE)) {
            UIHandler.removeMessages(UPDATE);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView.canPause()) {
            videoView.stopPlayback();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(0);// 移除所有消息
            handler = null;
        }
        if (mVolumeReceiver != null) {
            unregisterReceiver(mVolumeReceiver);// 解注册
        }
    }

    // 返回事件
    @Override
    public void onBackPressed() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            super.onBackPressed();
        }
    }


    ///////////////////////////////////////

    // 音量的广播接收者，接收系统音量发生变化
    private class VolumeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
                int volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (volume == 0) {
                    //将图片设为静音图片
                } else {
                    //将图片设为音量图片
                }
                seekbar_volum.setProgress(volume);
            }
        }
    }


    /**
     * 重力感应监听者
     */
    public class OrientationSensorListener implements SensorEventListener {
        private static final int _DATA_X = 0;
        private static final int _DATA_Y = 1;
        private static final int _DATA_Z = 2;

        public static final int ORIENTATION_UNKNOWN = -1;

        private Handler rotateHandler;

        public OrientationSensorListener(Handler handler) {
            rotateHandler = handler;
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {}

        public void onSensorChanged(SensorEvent event) {

            if (sensor_flag != stretch_flag)  //只有两个不相同才开始监听行为
            {
                float[] values = event.values;
                int orientation = ORIENTATION_UNKNOWN;
                float X = -values[_DATA_X];
                float Y = -values[_DATA_Y];
                float Z = -values[_DATA_Z];
                float magnitude = X * X + Y * Y;
                // Don't trust the angle if the magnitude is small compared to the y value
                if (magnitude * 4 >= Z * Z) {
                    //屏幕旋转时
                    float OneEightyOverPi = 57.29577957855f;
                    float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                    orientation = 90 - (int) Math.round(angle);
                    // normalize to 0 - 359 range
                    while (orientation >= 360) {
                        orientation -= 360;
                    }
                    while (orientation < 0) {
                        orientation += 360;
                    }
                }
                if (rotateHandler != null) {
                    rotateHandler.obtainMessage(888, orientation, 0).sendToTarget();
                }

            }
        }
    }


    public class OrientationSensorListener2 implements SensorEventListener {
        private static final int _DATA_X = 0;
        private static final int _DATA_Y = 1;
        private static final int _DATA_Z = 2;

        public static final int ORIENTATION_UNKNOWN = -1;

        public void onAccuracyChanged(Sensor arg0, int arg1) {
            // TODO Auto-generated method stub

        }

        public void onSensorChanged(SensorEvent event) {

            float[] values = event.values;

            int orientation = ORIENTATION_UNKNOWN;
            float X = -values[_DATA_X];
            float Y = -values[_DATA_Y];
            float Z = -values[_DATA_Z];

            /**
             * 这一段据说是 android源码里面拿出来的计算 屏幕旋转的 不懂 先留着 万一以后懂了呢
             */
            float magnitude = X * X + Y * Y;
            // Don't trust the angle if the magnitude is small compared to the y value
            if (magnitude * 4 >= Z * Z) {
                //屏幕旋转时
                float OneEightyOverPi = 57.29577957855f;
                float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                orientation = 90 - (int) Math.round(angle);
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360;
                }
                while (orientation < 0) {
                    orientation += 360;
                }
            }

            if (orientation > 225 && orientation < 315) {  //横屏
                sensor_flag = false;
            } else if ((orientation > 315 && orientation < 360) || (orientation > 0 && orientation < 45)) {  //竖屏
                sensor_flag = true;
            }

            if (stretch_flag == sensor_flag) {  //点击变成横屏  屏幕也转横屏 激活
                System.out.println("激活");
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);

            }
        }
    }
}
