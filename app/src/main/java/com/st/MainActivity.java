package com.st;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.st.adapter.FilelListAdapter;
import com.st.util.FileUtil;
import com.st.util.STConfig;
import com.st.util.ULog;

/**
 * smarttouch是一个android工程，而smarttouchlib是一个Java工程。
 * smarttouchlib是脚本的核心运行库，它封装了UIAutomator的操作（如模拟back键，点击某个地方打开最近的应用），
 * 上层的开发者可以编写lua脚本执行相应的操作，而lua的方法库定义在luascript类中，具体的实现是在luascriptimpl类中。
 * smarttouchlib是一个本地进程，smarttouch应通过socket（接口）和它进行通信。
 */


public class MainActivity extends Activity implements OnItemClickListener, DataManager.ScriptLibConnectListener {
	public static final String LUAPATH = FileUtil.getAndroidPath("/lua");// lua文件路径
	private ListView mListView;
	private FilelListAdapter mAdapter;
	private TextView mDesc;
	private String TAG="KK";

	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(newBase);
		MultiDex.install(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ULog.i("onCreate: mainactivity");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mDesc = (TextView)findViewById(R.id.desc);
		initListView();

		DataManager.getmInstance().registScriptLibConnectListener(this);
	}

	 /**
	    * 初始化listView
	    */
	private void initListView() {
		ULog.i("mainactivity.initListView");
		mListView = (ListView) findViewById(R.id.list_file);
		mListView.setOnItemClickListener(this);
		if (STConfig.mConnected) {
			mAdapter = new FilelListAdapter(this, FileUtil.getLuaFile(LUAPATH));
			mDesc.setText(R.string.script_lib_connected_txt);
		} else {
			mAdapter = new FilelListAdapter(this);
			mDesc.setText(R.string.script_lib_connecting_txt);
		}

		mListView.setAdapter(mAdapter);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		//execute
		String luaPath = (String)mAdapter.getItem(position);
		String name = String.format(getString(R.string.script_execute), luaPath);
		Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
		Intent intent=new Intent(STConfig.ACTION_DELAY_EXEC_LUA);
		intent.setClass(this, LocalServerService.class);
		intent.putExtra(STConfig.LUA_PATH, MainActivity.LUAPATH + "/" + luaPath);
		startService(intent);
	}

	@Override
	public void onConnectChanged(boolean succ) {
		ULog.i("MainActivity.onConnectChanged");
		if (succ) {
			mDesc.setText(R.string.script_lib_connected_txt);
			mAdapter.setDatas(FileUtil.getLuaFile(LUAPATH));
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		DataManager.getmInstance().unregistScriptLibConnectListener();
	}
}
