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
	 * ʡ�б�
	 * */
	private List<Province> provinceList;
	/*
	 * ���б�
	 * */
	private List<City> cityList;
	
	/*
	 * ���б�
	 * */
	private List<Country> countryList;
	/*
	 * ѡ��ʡ��
	 * */
	private Province selectedProvince;
	/*
	 * ѡ�еĳ���
	 * */
	private City selectedCity;
	/*
	 * ѡ�еļ���
	 * */
	private int currentLevel;
	/*
	 * �Ƿ��WeatherActivity����ת����
	 * */
	private boolean isFromWeatherActivity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFromWeatherActivity=getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
		//�Ѿ�ѡ���˳����Ҳ��Ǵ�WeatherActivity��ת�����ģ��Ż�ֱ����ת��WeatherActivity
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
	 * ��ѯȫ�����е�ʡ�����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
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
			titleText.setText("�й�");
			currentLevel=LEVEL_PROVINCE;
		}else{
			queryFromServer(null,"province");
		}
	}
	/*
	 * ��ѯѡ��ʡ�����е���
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
	/*��ѯѡ������������
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
	 * ���ݴ���Ĵ��ź����ʹӷ������ϲ�ѯʡ��������
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
					//ͨ��runOnUiThread()�����ص����̴߳����߼�
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
				//ͨ��runOnUiThread()�����ص����߳�
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ��", Toast.LENGTH_LONG).show();
						
					}
				});
				
			}
			
		});
		
	}
	/*
	 * ��ʾ���ȶԻ���
	 * */
	private void showProgressDialog(){
		if(progressDialog==null){
			progressDialog=new ProgressDialog(this);
			progressDialog.setMessage("���ڼ��ء�����");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	/*
	 * �رս��ȶԻ���
	 * */
	private void closeProgressDialog(){
		if(progressDialog!=null){
			progressDialog.dismiss();
		}
	}
	/*
	 * ����Back���������ݵ�ǰ�������жϣ���ʱӦ�÷������б�ʡ�б�����ֱ���˳�
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
