package com.skt.ipc.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import android.os.Handler;

import com.skt.ipc.rpc.CallDefine;
import com.skt.ipc.SktServer;
import com.st.util.STConfig;
import com.st.util.ULog;

public class RpcCallManager {
	private static RpcCallManager mInstance;
	private Map<String, CallDefine> mCalls = new ConcurrentHashMap<String, CallDefine>();
	private Map<String, Call> Calls = new ConcurrentHashMap<String, Call>();
	private Handler mHandler;
	private boolean mIsContented = false;
	
	private RpcCallManager() {
		
	}
	
	public static synchronized RpcCallManager getInstance() {
		if (mInstance == null) {
			mInstance = new RpcCallManager();
		}
		
		return mInstance;
	}
	public void init() {
		ULog.d("RCM----------------------init----------------------");
		RpcCall obj = new RpcCall();
		addCall(RpcConfig.REMOTE_METHOD_EXEC_LUA, obj);
		addCall(RpcConfig.REMOTE_METHOD_STOP_LUA, obj);
		addCall(RpcConfig.REMOTE_METHOD_PAUSE_LUA, obj);
	}
	private void addCall(String method, RpcCall obj) {
		mCalls.put(method, new CallDefine(obj, Utils.getMethod(RpcCall.class, method)));
	}
	public void setHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}

	public Object invoke(String method, String luaPath) throws RpcException {
		CallInfo info = new CallInfo(method, luaPath);
		Call call = new Call();
		Calls.put(info.getCallId(), call);
		try {
			ULog.d("RCM----------------------server send data----------------------");
			SktServer.getInstance().sendByteData(Utils.pack(info));
		} catch (IOException e) {
			mCalls.remove(info.getCallId());
			throw new RpcException("rpc exception:" + e.getLocalizedMessage());
		}

		CallResponse response = call.getResponse();
		mCalls.remove(info.getCallId());

		ULog.i("state="+response.getState()+"   res:"+response.getResult());
		switch(response.getState()){
			case CallResponse.EXCEPTION:
				throw new RpcException("rpc call exception");
			case CallResponse.METHOD_NOT_FOUND:
				throw new RpcException("method not found");
			case CallResponse.SUCCESS:
				return response.getResult();
			default:
				return null;
		}
	}

//	public CallResponse invoke(CallInfo info) {
//		CallResponse response = new CallResponse();
//		response.setCallId(info.getCallId());
//
//		// method not found
//		if (info.getMethod() == null) {
//			response.setState(CallResponse.METHOD_NOT_FOUND);
//			return response;
//		}
//
//		CallDefine callDefine = mCalls.get(info.getMethod());
//		if (callDefine == null) {
//			response.setState(CallResponse.METHOD_NOT_FOUND);
//		} else {
//			try {
//				// call the method
//				Object rs = callDefine.invoke(info.getLuaPath());
//				response.setResult(rs);
//				response.setState(CallResponse.SUCCESS);
//			} catch (Exception e) {
//				ULog.i("call ex:" + e.getMessage());
//				response.setState(CallResponse.EXCEPTION);
//			}
//		}
//		return response;
//	}

	public void listener(InputStream is) throws IOException{
		byte[] byteNum = new byte[4];
		int num = is.read(byteNum);
		if(num < 1) {
            return;
    	} else if (num != 4){
			return;
		}
		// 得到数据区的长度，这里简单处理直接读取全部数据区.
		int len = Utils.bytes2Int(byteNum);
		byte[] content = new byte[len];
		//读取数据区并反序列化
		is.read(content);
        Object obj = Utils.unseralize(content);
        if(obj instanceof CallResponse){
        	CallResponse response = (CallResponse)obj;
        	Call call = Calls.get(response.getCallId());
        	if (call != null) {
        		//通知获取结果
				ULog.i("STA:call != null");
        		call.setResponse(response);
        		if (mHandler != null) {
    				mHandler.sendEmptyMessage(STConfig.RPC_CALL_RESPONSE);
    			}
        	} else {
        		ULog.i("watch from server");
        		if (!mIsContented) {
        			if (mHandler != null) {
        				mHandler.sendEmptyMessage(STConfig.RPC_CONNECTED);
        			}
        		}
        	}
        	
    		mIsContented = true;
        }
        
	}
	
	private class Call{
		//用阻塞队列来通知
		ArrayBlockingQueue<CallResponse> mQueue = new ArrayBlockingQueue<CallResponse>(1);
		
		public void setResponse(CallResponse response){
			mQueue.add(response);
		}
		
		public CallResponse getResponse(){
			try {
				return mQueue.take();
			} catch (InterruptedException e) {
				return null;
			}
		}
	}
	
}
