package com.skt.ipc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import com.skt.ipc.rpc.RpcCallManager;
import com.st.util.ULog;

public class SktServer {
	private static final String SOCKET_PATH = "smart_touch_skt";
	private volatile LocalServerSocket mLocalServerSocket;
	private volatile InputStream mServerInputStream;
	private volatile OutputStream mServerOutputStream;
    private static SktServer mInstance;
    private boolean mIsRunning = false;
    
    private SktServer() {
    	
    }
    
    public synchronized static SktServer getInstance() {
    	if (mInstance == null) {
    		mInstance = new SktServer();
    	}
    	
    	return mInstance;
    }
    
	public void start() {
		ULog.i("SS服务器开始----------------");
		if (!mIsRunning) {
			new ServerThread().start();
		}
	}
	    
	//****************当地服务器接口****************
    private class ServerThread extends Thread {

        @Override
        public void run() {
            try {
            	ULog.i("SS服务器启动----------------");
                mLocalServerSocket = new LocalServerSocket(SOCKET_PATH);
                mIsRunning = true;
                LocalSocket socket = mLocalServerSocket.accept();
                if (socket == null) {
                	ULog.i("SS本地接口连接本地一接口服务器失败！！！");
                } else {
                	ULog.i("SS本地接口连接本地一接口服务器成功！！！");
                    mServerInputStream = socket.getInputStream();
                    mServerOutputStream = socket.getOutputStream();
                    for (;;) {
                        RpcCallManager.getInstance().listener(mServerInputStream);
                    }
                }
            } catch (IOException e) {
            	ULog.i("server io ex:"+ e.getMessage());
                e.printStackTrace();
                closeLockSocketService();
            }
        }
    }
    
    //服务器发送信息到客户端
    public void sendByteData(byte[] data) throws IOException{
        if (mServerOutputStream != null) {
            mServerOutputStream.write(data);
            mServerOutputStream.flush();
        }
    }
    
    private synchronized void closeLockSocketService() {
        //关闭服务器接口
    	try {
            if (mLocalServerSocket != null) {
                mLocalServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mLocalServerSocket = null;
        }
        
        //关闭输入流
        try {
            if (mServerInputStream != null) {
                mServerInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mServerInputStream = null;
        }
        
        //关闭输出流
        try {
            if (mServerOutputStream != null) {
                mServerOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mServerOutputStream = null;
        }
        
        mIsRunning = false;
    }
    
    public void recyle() {
    	closeLockSocketService();
    }
    
}
