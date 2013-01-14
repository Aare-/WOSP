package com.subfty.wosp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class MainActivity extends Activity {

	private final String W_LOGIN = "http://wosp2.krakowpodgorze.zhp.pl/ajax/zaloguj",
						 W_SUBMIT="http://wosp2.krakowpodgorze.zhp.pl/ajax/rozlicz";
	
	final int V_LOGIN = 0,
			  V_MENU = 1,
			  V_USER_RECOL = 2;
	ViewFlipper flipper;
	ProgressDialog progress;
	private Activity me;
	
	private String sLogin,
				   sPassword;
	
  //WORKERS
	private String getToURL(String adress, Map<String, String> m){
		Iterator<Map.Entry<String, String>> it = m.entrySet().iterator();
		adress += "?";
		while(it.hasNext()){
			Map.Entry<String, String> pairs = it.next();
			adress += pairs.getKey()+"="+pairs.getValue()+"&";
		}
		
		return adress.substring(0, adress.length()-1);
	}
	
	private Integer doURLQuery(URL arg0){
		String result = "";
		Log.i("WOSP", "URL: "+arg0.toString());
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(arg0.openStream()));

			String inputLine;
			while ((inputLine = in.readLine()) != null)
				result = result + inputLine;

			in.close();
		}catch(IOException e){
			Log.i("WOSP", "IOException");
			return 0;
		}
		
		Log.i("WOSP", "Result: '"+result+"'");
		
		if(result.charAt(result.length()-1) == '1'){
			Log.i("WOSP", "OK!");
			return 1;
		}
		Log.i("WOSP", "ERROR");
		return 0;
	}
	
	private class PerformLogin extends AsyncTask<URL, Integer, Integer >{
		@Override
		protected Integer doInBackground(URL... arg0) {
			return doURLQuery(arg0[0]);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
	         progress.cancel();
	         ((TextView)findViewById(R.id.login_error)).setText("");
	         if(result == 1){
	        	 setNextFlipperAnim();
	        	 flipper.setDisplayedChild(V_MENU);
	        	 ((TextView)findViewById(R.id.name)).setText(sLogin);
	        	 Log.i("WOSP", "OK");
	         }else{
	        	 ((EditText)findViewById(R.id.pass)).setText("");
				 ((TextView)findViewById(R.id.login_error)).setText("Blad logowania!");
				 Log.i("WOSP", "Error");
	         }
	    }
	}
	
	private class PerformDataSubmit extends AsyncTask<URL, Integer, Integer>{
		@Override
		protected Integer doInBackground(URL... arg0){
			return doURLQuery(arg0[0]);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
	         progress.cancel();
	         if(result == 1){
	        	 Toast.makeText(me, "Sukces :D", Toast.LENGTH_SHORT)
	        	 	  .show();
	        	 Log.i("WOSP", "OK");
	         }else{
	        	 Toast.makeText(me, "Porazka :(", Toast.LENGTH_LONG)
	        	 	  .show();
	        	 Log.i("WOSP", "Error");
	         }
	         
	         setNextFlipperAnim();
        	 flipper.setDisplayedChild(V_MENU);
	    }
	}
	
  //ON CREATE
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;
        setContentView(R.layout.activity_main);
        
        flipper = (ViewFlipper) findViewById(R.id.main_view);
        progress = new ProgressDialog(this);
        progress.setMessage("Laczenie z serwerem");
    	progress.setCancelable(false);
    	
        ((Button)this.findViewById(R.id.lButton))
	        .setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					EditText login = (EditText)findViewById(R.id.login),
							 passwd = (EditText)findViewById(R.id.pass);
					
					
					sLogin = login.getText().toString();
					sPassword = passwd.getText().toString();
					
				  //HASHING PASSWORD
					MessageDigest m;
					try {
						m = MessageDigest.getInstance("MD5");
					
						m.update(sPassword.getBytes(), 0, sPassword.length());
						sPassword = new BigInteger(1,m.digest()).toString(16);
					} catch (NoSuchAlgorithmException e1) {
						Log.e("WOSP", "Nie mam pojecia jaki to algorytm!");
						e1.printStackTrace();
					}
					
					Log.i("WOSP", "password: "+passwd.getText().toString()+" password hash: "+sPassword);
					
					progress.show();
					((EditText)findViewById(R.id.pass)).setText("");
					try{
						Map<String, String> map = new HashMap<String, String>();
						  map.put("login", sLogin);
						  map.put("password", sPassword);
						String url = getToURL(W_LOGIN, map);
						Log.i("WOSP", "Logging: "+url);
						
						new PerformLogin().execute(new URL(url));
					}catch(MalformedURLException e){
						Toast.makeText(me, "Malformed URL exception", Toast.LENGTH_LONG)
							 .show();
						progress.cancel();
					}
				
				}
			});
        
        ((Button)this.findViewById(R.id.reconcil))
					 .setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							try {
					            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
					            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
					
					            startActivityForResult(intent, 0);
						    } catch (Exception e) {
						            Uri marketUri = Uri
						                    .parse("market://details?id=com.google.zxing.client.android");
						            Intent marketIntent = new Intent(Intent.ACTION_VIEW,
						                    marketUri);
						            startActivity(marketIntent);
						   }
						}
					});
       
        ((Button)this.findViewById(R.id.recol))
					 .setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							float multip[] = {0.01f, 0.02f, 0.05f, 0.1f, 0.2f, 0.5f, 1.0f, 2.0f, 5.0f, 10.0f, 20.0f, 50.0f, 100.0f, 200.0f};
							int mId=0;
							float sum = 0;
							LinearLayout l = (LinearLayout)findViewById(R.id.uFillDataForm);
							try {
				        		for(int i=0; i<l.getChildCount()-1; i++)
				        			if(l.getChildAt(i) instanceof EditText){
				        			   if(((EditText)l.getChildAt(i)).getText().toString().length() > 0)
				        				   sum += Float.parseFloat(((EditText)l.getChildAt(i)).getText().toString()) * multip[mId];
				        			   mId++;
				        			}
									
							} catch (NotFoundException e) {
								Log.e("WOSP", "NOT FOUND");
								e.printStackTrace();
							}
							
							new AlertDialog.Builder(me)
								   .setMessage("Suma poprawna: "+sum+" ?")
							       .setCancelable(false)
							       .setPositiveButton("Tak", new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							        	   progress.show();
										   try{
											   Map<String, String> map = new HashMap<String, String>();
											   map.put("login", sLogin);
											   map.put("password", sPassword);
											   map.put("id", ((TextView)findViewById(R.id.user_ID)).getText().toString());
											   
											   LinearLayout l = (LinearLayout)findViewById(R.id.uFillDataForm);
									           try {
									        		for(int i=0; i<l.getChildCount(); i++)
									        			if(l.getChildAt(i) instanceof EditText &&
									        			   ((EditText)l.getChildAt(i)).getText().toString().length() > 0)
															map.put(getResources().getResourceEntryName(((EditText)l.getChildAt(i)).getId()), 
																	java.net.URLEncoder.encode(((EditText)l.getChildAt(i)).getText().toString(), "ISO-8859-1"));
												} catch (NotFoundException e) {
													Log.e("WOSP", "NOT FOUND");
													e.printStackTrace();
												} catch (UnsupportedEncodingException e) {
													Log.e("WOSP", "Unsupported encoding");
													e.printStackTrace();
												}
											   
											   String url = getToURL(W_SUBMIT, map); 
											   Log.i("WOSP", "submitting data: "+url);
											   new PerformDataSubmit().execute(new URL(url));
											   
											   //?login=login&password=has³o&id=1&comments=komentarz&coinsNo002=6&coinsNo01=17&coinsNo50=8
										   }catch(MalformedURLException e){
											   Toast.makeText(me, "Malformed URL exception", Toast.LENGTH_LONG)
												    .show();
											   progress.cancel();
										   }
							           }
							       })
							       .setNegativeButton("Momencik...", new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							                
							           }
							       })
							       .create()
							       .show();
						}
					});
    }
    
    
    @Override
    public void onBackPressed(){
    	AlertDialog.Builder builder;
    	
    	switch(flipper.getCurrentView().getId()){
    	case R.id.l_form:
    		this.moveTaskToBack(true);
    		break;
    	case R.id.p_form:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("Wylogowac?")
			       .setCancelable(false)
			       .setPositiveButton("Yup!", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   setPrevFlipperAnim();
			        	   flipper.setDisplayedChild(V_LOGIN);
			           }
			       })
			       .setNegativeButton("Meh...", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                
			           }
			       });
			builder.create()
		           .show();
    		break;
    	case R.id.u_form:
    		builder = new AlertDialog.Builder(this);
			builder.setMessage("Anulowac rozliczenie?")
			       .setCancelable(false)
			       .setPositiveButton("Poprosze", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   setPrevFlipperAnim();
			        	   flipper.setDisplayedChild(V_MENU);
			           }
			       })
			       .setNegativeButton("Nie", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                
			           }
			       });
			builder.create()
			       .show();
    		break;
    	}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    Log.i("WOSP", "ActivityResult requestCode: "+requestCode+" resultCode: "+resultCode);
	        if (requestCode == 0) 
	            if (resultCode == RESULT_OK) {
	                String contents = data.getStringExtra("SCAN_RESULT");
	                Log.i("WOSP", "Content: '"+contents+"'");
	                
	                String arr[] = contents.split("_");
	                contents = arr[0]+arr[1];
	                
	                ((TextView)findViewById(R.id.user_ID)).setText(contents.substring(contents.indexOf(".")+1));
	                setNextFlipperAnim();
		        	flipper.setDisplayedChild(V_USER_RECOL);
		        	
		        	LinearLayout l = (LinearLayout)findViewById(R.id.uFillDataForm);
		        	for(int i=0; i<l.getChildCount(); i++)
		        		if(l.getChildAt(i) instanceof EditText)
		        			((EditText)l.getChildAt(i)).setText("");
		        	
	            }else{
	            	Toast.makeText(this.getApplicationContext(), "Porazka ;_;", Toast.LENGTH_LONG)
	            		 .show();
	            }
//		if(resultCode == RESULT_CANCELLED){
//		//handle cancel
//		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.activity_main, menu);
        return false;
    }

    
  //LAYOUT ANIMATIONS
    private void setNextFlipperAnim(){
    	flipper.setInAnimation(inFromRightAnimation());
    	flipper.setOutAnimation(outToLeftAnimation());

    }
    private void setPrevFlipperAnim(){
    	flipper.setInAnimation(inFromLeftAnimation());
		flipper.setOutAnimation(outToRightAnimation());
    }
    
    private Animation inFromRightAnimation() {
    	Animation inFromRight = new TranslateAnimation(
	    	Animation.RELATIVE_TO_PARENT,  +1.0f, Animation.RELATIVE_TO_PARENT,  0.0f,
	    	Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
    	);
    	
    	inFromRight.setDuration(300);
    	inFromRight.setInterpolator(new AccelerateInterpolator());
    	return inFromRight;
    }
	private Animation outToLeftAnimation() {
		Animation outtoLeft = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  -1.0f,
			Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
		);
		
		outtoLeft.setDuration(300);
		outtoLeft.setInterpolator(new AccelerateInterpolator());
		
		return outtoLeft;
	}
	private Animation inFromLeftAnimation() {
		Animation inFromLeft = new TranslateAnimation(
		Animation.RELATIVE_TO_PARENT,  -1.0f, Animation.RELATIVE_TO_PARENT,  0.0f,
		Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
		);
		inFromLeft.setDuration(300);
		inFromLeft.setInterpolator(new AccelerateInterpolator());
		return inFromLeft;
	}
	private Animation outToRightAnimation() {
		Animation outtoRight = new TranslateAnimation(
		 Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  +1.0f,
		 Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
		);
		outtoRight.setDuration(300);
		outtoRight.setInterpolator(new AccelerateInterpolator());
		return outtoRight;
	}
}
