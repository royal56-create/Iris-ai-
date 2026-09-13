package com.iris.aivoiceassistant;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

public class AssistantOverlayService extends Service {
    private WindowManager wm; private TextView bubble;
    @Override public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        bubble=new TextView(this); bubble.setText("IRIS\nAI"); bubble.setTextColor(Color.WHITE); bubble.setTextSize(12); bubble.setGravity(Gravity.CENTER); bubble.setBackgroundColor(0xFF151520); bubble.setOnClickListener(v->{Intent i=new Intent(this,MainActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i);});
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(dp(72),dp(72),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT); p.gravity=Gravity.END|Gravity.CENTER_VERTICAL; p.x=dp(12); wm.addView(bubble,p);
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){return START_NOT_STICKY;}
    @Override public void onDestroy(){if(wm!=null&&bubble!=null)try{wm.removeView(bubble);}catch(Exception ignored){} super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
}
