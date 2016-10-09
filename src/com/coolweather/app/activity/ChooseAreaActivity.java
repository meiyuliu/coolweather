package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.DownloadManager.Query;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.app.R;
import com.coolweather.app.model.City;
import com.coolweather.app.model.CoolWeatherDB;
import com.coolweather.app.model.Country;
import com.coolweather.app.model.Province;
import com.coolweather.app.model.Utility;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;

public class ChooseAreaActivity extends Activity {
	public static final int LEVEL_PROVINCE=0;
	public static final int LEVEL_CITY=1;
	public static final int LEVEL_COUNTRY=2;
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList=new ArrayList<String>();
	/*
	 * 省列表
	 * */
	private List<Province> provinceList;
	/*
	 * 市列表
	 * */
	private List<City> cityList;
	
	/*
	 * 县列表
	 * */
	private List<Country> countryList;
	/*
	 * 选中省份
	 * */
	private Province selectedProvince;
	/*
	 * 选中的城市
	 * */
	private City selectedCity;
	/*
	 * 选中的级别
	 * */
	private int currentLevel;
	/*
	 * 是否从WeatherActivity中跳转过来
	 * */
	private boolean isFromWeatherActivity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFromWeatherActivity=getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
		//已经选择了城市且不是从WeatherActivity跳转过来的，才会直接跳转到WeatherActivity
		if(prefs.getBoolean("city_selected", false)&&!isFromWeatherActivity){
			Intent intent=new Intent(this,WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView=(ListView)findViewById(R.id.list_view);
		titleText=(TextView)findViewById(R.id.title_text);
		adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,dataList);
		listView.setAdapter(adapter);
		coolWeatherDB=CoolWeatherDB.getInstantance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				if(currentLevel==LEVEL_PROVINCE){
					selectedProvince = provinceList.get(index);
					queryCities();
				}else if(currentLevel==LEVEL_CITY){
					selectedCity = cityList.get(index);
					queryCountries();
				} else if(currentLevel==LEVEL_COUNTRY){
					
						String countyCode = countryList.get(index)
								.getCountryCode();
						Intent intent = new Intent(ChooseAreaActivity.this,
								WeatherActivity.class);
						intent.putExtra("county_code", countyCode);
						startActivity(intent);
					finish();
				}

			}

		});
		queryProvinces();
		
	}
	/*
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
	 * */
	private void queryProvinces(){
		provinceList=coolWeatherDB.loadProvinces();
		if(provinceList.size()>0){
			dataList.clear();
			for(Province p:provinceList){
				dataList.add(p.getProvincename());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel=LEVEL_PROVINCE;
		}else{
			queryFromServer(null,"province");
		}
	}
	/*
	 * 查询选中省内所有的市
	 * */
	private void queryCities(){
		cityList=coolWeatherDB.loadCities(selectedProvince.getId());
//		String s=selectedProvince.getId()+"";
//		Log.d("ceshi", s);
		if(cityList.size()>0){
			dataList.clear();
			for(City c:cityList){
				dataList.add(c.getCityName());
				//Log.d("xianshi", c.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvincename());
			currentLevel=LEVEL_CITY;
		}
		else{
//			String s1=selectedProvince.getProvinceCode()+"";
//			Log.d("ceshi", s1);
			queryFromServer(selectedProvince.getProvinceCode(),"city");
		}
	}
	/*查询选中市内所有县
	 * 
	 * */
	private void queryCountries(){
		countryList=coolWeatherDB.loadCountry(selectedCity.getId());
		if(countryList.size()>0){
			dataList.clear();
			for(Country c:countryList){
				dataList.add(c.getCountryName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel=LEVEL_COUNTRY;
		}
		else{
			queryFromServer(selectedCity.getCityCode(),"country");
		}
	}
	/*
	 * 根据传入的代号和类型从服务器上查询省市县数据
	 * */
	private void queryFromServer(final String code,final String type){

		String address;
		if(!TextUtils.isEmpty(code)){
			address="http://www.weather.com.cn/data/list3/city"+code+".xml";
		}else{
			address="http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener(){


			@Override
			public void onFinish(String response) {
				boolean result=false;
				if("province".equals(type)){
					result=Utility.handleProvinceResponse(coolWeatherDB, response);
				}else if("city".equals(type)){
					result=Utility.handleCityResponse(coolWeatherDB, response, selectedProvince.getId());
				}else if("country".equals(type)){
					result=Utility.handleCountryResponse(coolWeatherDB, response, selectedCity.getId());
				}
				if(result){
					//通过runOnUiThread()方法回到主线程处理逻辑
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							closeProgressDialog();
							if("province".equals(type)){
								queryProvinces();
							}else if("city".equals(type)){
								queryCities();
							}else{
								queryCountries();
							}
						}
					});
				}
				
			}

			@Override
			public void onError(Exception e) {
				//通过runOnUiThread()方法回到主线程
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_LONG).show();
						
					}
				});
				
			}
			
		});
		
	}
	/*
	 * 显示进度对话框
	 * */
	private void showProgressDialog(){
		if(progressDialog==null){
			progressDialog=new ProgressDialog(this);
			progressDialog.setMessage("正在加载。。。");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	/*
	 * 关闭进度对话框
	 * */
	private void closeProgressDialog(){
		if(progressDialog!=null){
			progressDialog.dismiss();
		}
	}
	/*
	 * 捕获Back按键，根据当前级别来判断，此时应该返回市列表，省列表，还是直接退出
	 * */
	@Override
	public void onBackPressed() {
		if(currentLevel==LEVEL_COUNTRY){
			queryCities();
		}else if(currentLevel==LEVEL_CITY){
			queryProvinces();
		}else{
			if (isFromWeatherActivity) {				
				Intent intent = new Intent(this,
						WeatherActivity.class);
				startActivity(intent);
			}
			finish();                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
		}
	}
	
}
