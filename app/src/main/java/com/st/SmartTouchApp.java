package com.st;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.skt.ipc.rpc.RpcCallManager;
import com.st.util.STConfig;
import com.st.util.ULog;


public class SmartTouchApp extends Application {
    private Handler mHandler;

    @Override
    public void onCreate() {
        ULog.i("STA.oncreat");
        super.onCreate();
        Log.d("kk", "onCreate: SmartTouchApp");
        registerHandler();
        initIpc();
    }

    private void initIpc() {
        ULog.i("STA===============启动LocalServerServic=====================");
        Intent intent = new Intent(STConfig.ACTION_INIT);
        intent.setClass(this, LocalServerService.class);
        startService(intent);
    }

    private void registerHandler() {
        ULog.i("STA================registerHandler====================");
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                ULog.i("STA================connect=true or false====================");
                switch (msg.what) {
                    case STConfig.RPC_CONNECTED:
                        STConfig.mConnected = true;
                        DataManager.getmInstance().notifyScriptLibConnectChanged(true);
                        Toast.makeText(SmartTouchApp.this, "脚本底层库已连接成功", Toast.LENGTH_SHORT).show();
                        break;

                    case STConfig.RPC_CALL_RESPONSE:
                        break;
                }
            }

        };

        RpcCallManager.getInstance().setHandler(mHandler);
    }
}
