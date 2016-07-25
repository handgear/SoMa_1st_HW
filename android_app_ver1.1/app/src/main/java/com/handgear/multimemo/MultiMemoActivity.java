package com.handgear.multimemo;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;

import com.handgear.multimemo.common.TitleBitmapButton;
import com.handgear.multimemo.db.MemoDatabase;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import static com.handgear.multimemo.BasicInfo.*;

/**
 * 멀티메모 메인 액티비티
 *
 * @author Mike
 */
public class MultiMemoActivity extends Activity {

	public static final String TAG = "MultiMemoActivity";

	/**
	 * 메모 리스트뷰
	 */
	ListView mMemoListView;

	/**
	 * 메모 리스트 어댑터
	 */
	MemoListAdapter mMemoListAdapter;

	/**
	 * 메모 갯수
	 */
	int mMemoCount = 0;


	/**
	 * 데이터베이스 인스턴스
	 */
	public static MemoDatabase mDatabase = null;

	/**
	 * HTTP test
	 */
	//		EditText input01;
	TextView txtMsg;

	public static final String defaultUrl = "http://handgear.pythonanywhere.com/api/test";
	public static final String urlStr_post = "http://handgear.pythonanywhere.com/api/update";
	Handler handler = new Handler();

	//트위터용 핸들러
	TextView nameText;
	Button connectBtn;
	StatusListView statusList;
	StatusListAdapter statusAdapter;
	Button writeBtn;
	EditText writeInput;
	Handler mHandler = new Handler();

	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.multimemo);

		// SD Card checking
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			Toast.makeText(this, "SD 카드가 없습니다. SD 카드를 넣은 후 다시 실행하십시오.", Toast.LENGTH_LONG).show();
			return;
		} else {
			String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
			if (!ExternalChecked && externalPath != null) {
				ExternalPath = externalPath + File.separator;
				Log.d(TAG, "ExternalPath : " + ExternalPath);

				FOLDER_PHOTO = ExternalPath + FOLDER_PHOTO;
				FOLDER_VIDEO = ExternalPath + FOLDER_VIDEO;
				FOLDER_VOICE = ExternalPath + FOLDER_VOICE;
				FOLDER_HANDWRITING = ExternalPath + FOLDER_HANDWRITING;
				DATABASE_NAME = ExternalPath + DATABASE_NAME;

				ExternalChecked = true;
			}
		}


		// 메모 리스트
		mMemoListView = (ListView) findViewById(R.id.memoList);
		mMemoListAdapter = new MemoListAdapter(this);
		mMemoListView.setAdapter(mMemoListAdapter);
		mMemoListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				viewMemo(position);
			}
		});


		// 새 메모 버튼 설정
		TitleBitmapButton newMemoBtn = (TitleBitmapButton) findViewById(R.id.newMemoBtn);
		newMemoBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "newMemoBtn clicked.");

				Intent intent = new Intent(getApplicationContext(), MemoInsertActivity.class);
				intent.putExtra(KEY_MEMO_MODE, MODE_INSERT);
				startActivityForResult(intent, REQ_INSERT_ACTIVITY);
			}
		});

		// 닫기 버튼 설정
		TitleBitmapButton closeBtn = (TitleBitmapButton) findViewById(R.id.closeBtn);
		closeBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});


		//HTTP test

//		input01 = (EditText) findViewById(R.id.input01);
//		input01.setText(defaultUrl);

		txtMsg = (TextView) findViewById(R.id.textView);

//		// GET(test) 버튼 이벤트 처리
//		Button requestBtn = (Button) findViewById(R.id.testBtn);
//		requestBtn.setOnClickListener(new OnClickListener() {
//			public void onClick(View v) {
//				String urlStr = defaultUrl;//input01.getText().toString();
//
//				ConnectThread thread = new ConnectThread(urlStr, "GET");
//				thread.start();
//
//			}
//		});

//		// 동기화(test) 버튼 이벤트 처리
//		Button syncBtn = (Button) findViewById(R.id.testBtn);
//		syncBtn.setOnClickListener(new OnClickListener() {
//			public void onClick(View v) {
//				String urlStr = defaultUrl;//input01.getText().toString();
//
//				ConnectThread thread = new ConnectThread(urlStr, "GET");
//				thread.start();
//
//			}
//		});

		// POST(postBtn) 버튼 이벤트 처리
		Button postBtn = (Button) findViewById(R.id.postBtn);
		postBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String urlStr = urlStr_post;

				ConnectThread thread = new ConnectThread(urlStr, "POST");
				thread.start();

			}
		});

		connectBtn = (Button) findViewById(R.id.tweetBtn);
		connectBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				connect();
			}
		});

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
	}

	@Override
	public void onStop() {
		super.onStop();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"MultiMemo Page", // TODO: Define a title for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app deep link URI is correct.
				Uri.parse("android-app://com.handgear.multimemo/http/host/path")
		);
		AppIndex.AppIndexApi.end(client, viewAction);
		client.disconnect();
	}

	/**
	 * 소켓 연결할 스레드 정의
	 */
	class ConnectThread extends Thread {
		String urlStr;
		String option;

		public ConnectThread(String inStr) {
			urlStr = inStr;
		}

		public ConnectThread(String inStr, String httpOption) {
			urlStr = inStr;
			option = httpOption;
		}

//		public ConnectThread(String inStr, String httpOption, JSONObject jsonPost) {
//			urlStr = inStr;
//			option = httpOption;
//			JSONObject jsonParam;
//			jsonParam = jsonPost;
//		}

		String output_handler;
		public void run() {
			final String output;

			try {
				if (option == "GET") {
					output = request(urlStr);
				}
				else if (option == "POST") {


					openDatabase();
					String SQL = "select _id, INPUT_DATE, CONTENT_TEXT, ID_PHOTO, ID_VIDEO, ID_VOICE, ID_HANDWRITING from MEMO order by INPUT_DATE desc";

					int recordCount = -1;
					if (MultiMemoActivity.mDatabase != null) {
						Cursor outCursor = MultiMemoActivity.mDatabase.rawQuery(SQL);

						recordCount = outCursor.getCount();
						Log.d(TAG, "cursor count : " + recordCount + "\n");

						mMemoListAdapter.clear();
						Resources res = getResources();


						for (int i = 0; i < recordCount; i++) {
							outCursor.moveToNext();

							String memoId = outCursor.getString(0);

							String dateStr = outCursor.getString(1);
							if (dateStr.length() > 10) {
								dateStr = dateStr.substring(0, 10);
							}

							String memoStr = outCursor.getString(2);

							String android_id = Settings.Secure.getString(getContentResolver(),
									Settings.Secure.ANDROID_ID);
							JSONObject jsonParam = new JSONObject();
							try {
//								jsonParam.put("userId", android_id);
								jsonParam.put("memoId", memoId);
								jsonParam.put("date", dateStr);
								jsonParam.put("memo", memoStr);
							}
							catch (JSONException e){

							}

							output_handler = post(urlStr, jsonParam);
							handler.post(new Runnable() {
								public void run() {
									txtMsg.setText(output_handler);
								}
							});
						}

						outCursor.close();


					}

					output = "success post";

				// set JSON
//					JSONObject jsonParam = new JSONObject();
//				try {
//					jsonParam.put("phoneNum", "01010100000");
//					jsonParam.put("name", "test name");
//					jsonParam.put("address", "test address");
//				}
//				catch (JSONException e){
//
//				}
//					output = post(urlStr, jsonParam);
				}

				else {
					output = "Error from http run func";
				}


				handler.post(new Runnable() {
					public void run() {
						txtMsg.setText(output);
					}
				});

			} catch (Exception ex) {
				ex.printStackTrace();
			}
			loadMemoListData();
		}


		private String request(String urlStr) {
			StringBuilder output = new StringBuilder();
			try {
				URL url = new URL(urlStr);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				if (conn != null) {
					conn.setConnectTimeout(10000);
					conn.setRequestMethod("GET");
					conn.setDoInput(true);
					conn.setDoOutput(true);

					int resCode = conn.getResponseCode();
					if (resCode == HttpURLConnection.HTTP_OK) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						String line = null;
						while (true) {
							line = reader.readLine();
							if (line == null) {
								break;
							}
							output.append(line + "\n");
						}

						reader.close();
						conn.disconnect();
					}
				}
			} catch (Exception ex) {
				Log.e("SampleHTTP", "Exception in processing response.", ex);
				ex.printStackTrace();
			}

			return output.toString();
		}

		private String post(String urlStr, JSONObject jsonParam) {
			StringBuilder output = new StringBuilder();
//			String response = null;
//			urlStr = urlStr_post; //intercept default url to post url

			try {
				OutputStream os   = null;
				InputStream           is   = null;
				ByteArrayOutputStream baos = null;

				URL url = new URL(urlStr);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(10000 /* milliseconds */);
				conn.setConnectTimeout(15000 /* milliseconds */);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.connect();

				//post JSON
				os = conn.getOutputStream();
				os.write(jsonParam.toString().getBytes());
				os.flush();

				int resCode = conn.getResponseCode();
				if (resCode == HttpURLConnection.HTTP_OK) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String line = null;
					while (true) {
						line = reader.readLine();
						if (line == null) {
							break;
						}
						output.append(line + "\n");
					}

					reader.close();
					conn.disconnect();
				}
			} //end try
			catch (IOException ex) {
				Log.e("SampleHTTP", "Exception in processing response.", ex);
				ex.printStackTrace();
			}
			return output.toString();
		}

//		String post_response_str;
//		private String postJson(final String email, final String pwd) {
//			String response_str = null;
//			Thread t = new Thread() {
//
//
//				public void run() {
//					Looper.prepare(); //For Preparing Message Pool for the child Thread
//					HttpClient client = new DefaultHttpClient();
//					HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
//					HttpResponse response;
//					JSONObject json = new JSONObject();
//
//					try {
//						HttpPost post = new HttpPost(urlStr_post);
//						json.put("email", email);
//						json.put("password", pwd);
//						StringEntity se = new StringEntity(json.toString());
//						se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
//						post.setEntity(se);
//						response = client.execute(post);
//
//                    /*Checking response */
//						if (response != null) {
//							InputStream in = response.getEntity().getContent(); //Get the data in the entity
//							StringBuffer sb = new StringBuffer();
//							byte[] b = new byte[4096];
//							for (int n; (n = in.read(b)) != -1;) {
//								sb.append(new String(b, 0, n));
//							}
//							post_response_str = sb.toString();
//
//						}
//
//					} catch (Exception e) {
//						e.printStackTrace();
//						Log.d("Error", "Cannot Estabilish Connection");
//					}
//
//					Looper.loop(); //Loop in the message queue
//				}
//			};
//
//			t.start();
//			response_str = post_response_str;
//			return response_str;
//		}

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}


	protected void onStart() {

		// 데이터베이스 열기
		openDatabase();

		// 메모 데이터 로딩
		loadMemoListData();


		super.onStart();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client.connect();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"MultiMemo Page", // TODO: Define a title for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app deep link URI is correct.
				Uri.parse("android-app://com.handgear.multimemo/http/host/path")
		);
		AppIndex.AppIndexApi.start(client, viewAction);
	}


	/**
	 * 데이터베이스 열기 (데이터베이스가 없을 때는 만들기)
	 */
	public void openDatabase() {
		// open database
		if (mDatabase != null) {
			mDatabase.close();
			mDatabase = null;
		}

		mDatabase = MemoDatabase.getInstance(this);
		boolean isOpen = mDatabase.open();
		if (isOpen) {
			Log.d(TAG, "Memo database is open.");
		} else {
			Log.d(TAG, "Memo database is not open.");
		}
	}


	/**
	 * 메모 리스트 데이터 로딩
	 */
	public int loadMemoListData() {
		String SQL = "select _id, INPUT_DATE, CONTENT_TEXT, ID_PHOTO, ID_VIDEO, ID_VOICE, ID_HANDWRITING from MEMO order by INPUT_DATE desc";

		int recordCount = -1;
		if (MultiMemoActivity.mDatabase != null) {
			Cursor outCursor = MultiMemoActivity.mDatabase.rawQuery(SQL);

			recordCount = outCursor.getCount();
			Log.d(TAG, "cursor count : " + recordCount + "\n");

			mMemoListAdapter.clear();
			Resources res = getResources();

			for (int i = 0; i < recordCount; i++) {
				outCursor.moveToNext();

				String memoId = outCursor.getString(0);

				String dateStr = outCursor.getString(1);
				if (dateStr.length() > 10) {
					dateStr = dateStr.substring(0, 10);
				}

				String memoStr = outCursor.getString(2);
				String photoId = outCursor.getString(3);
				String photoUriStr = getPhotoUriStr(photoId);

				String videoId = outCursor.getString(4);
				String videoUriStr = null;

				String voiceId = outCursor.getString(5);
				String voiceUriStr = null;

				String handwritingId = outCursor.getString(6);
				String handwritingUriStr = null;

				// Stage3 added
				handwritingUriStr = getHandwritingUriStr(handwritingId);

				mMemoListAdapter.addItem(new MemoListItem(memoId, dateStr, memoStr, handwritingId, handwritingUriStr, photoId, photoUriStr, videoId, videoUriStr, voiceId, voiceUriStr));
			}

			outCursor.close();

			mMemoListAdapter.notifyDataSetChanged();
		}

		return recordCount;
	}

	public String loadMemoData() {
		String SQL = "select _id, INPUT_DATE, CONTENT_TEXT, ID_PHOTO, ID_VIDEO, ID_VOICE, ID_HANDWRITING from MEMO order by INPUT_DATE desc";
		String outputStr = null;
		int recordCount = -1;
		if (MultiMemoActivity.mDatabase != null) {
			Cursor outCursor = MultiMemoActivity.mDatabase.rawQuery(SQL);

			recordCount = outCursor.getCount();
			Log.d(TAG, "cursor count : " + recordCount + "\n");

//			mMemoListAdapter.clear();
//			Resources res = getResources();


				outCursor.moveToNext();

				String memoId = outCursor.getString(0);

				String dateStr = outCursor.getString(1);
				if (dateStr.length() > 10) {
					dateStr = dateStr.substring(0, 10);
				}
				String memoStr = outCursor.getString(2);

				outputStr = memoId;
			    outputStr = outputStr.concat(dateStr);
				outputStr = outputStr.concat(memoStr);



//				String photoId = outCursor.getString(3);
//				String photoUriStr = getPhotoUriStr(photoId);
//
//				String videoId = outCursor.getString(4);
//				String videoUriStr = null;
//
//				String voiceId = outCursor.getString(5);
//				String voiceUriStr = null;
//
//				String handwritingId = outCursor.getString(6);
//				String handwritingUriStr = null;


//				handwritingUriStr = getHandwritingUriStr(handwritingId);
//
//				mMemoListAdapter.addItem(new MemoListItem(memoId, dateStr, memoStr, handwritingId, handwritingUriStr, photoId, photoUriStr, videoId, videoUriStr, voiceId, voiceUriStr));


			outCursor.close();

//			mMemoListAdapter.notifyDataSetChanged();
		}

		return outputStr;
	}

	/**
	 * 사진 데이터 URI 가져오기
	 */
	public String getPhotoUriStr(String id_photo) {
		String photoUriStr = null;
		if (id_photo != null && !id_photo.equals("-1")) {
			String SQL = "select URI from " + MemoDatabase.TABLE_PHOTO + " where _ID = " + id_photo + "";
			Cursor photoCursor = MultiMemoActivity.mDatabase.rawQuery(SQL);
			if (photoCursor.moveToNext()) {
				photoUriStr = photoCursor.getString(0);
			}
			photoCursor.close();
		} else if (id_photo == null || id_photo.equals("-1")) {
			photoUriStr = "";
		}

		return photoUriStr;
	}

	/**
	 * 손글씨 데이터 URI 가져오기
	 */
	public String getHandwritingUriStr(String id_handwriting) {
		Log.d(TAG, "Handwriting ID : " + id_handwriting);

		String handwritingUriStr = null;
		if (id_handwriting != null && id_handwriting.trim().length() > 0 && !id_handwriting.equals("-1")) {
			String SQL = "select URI from " + MemoDatabase.TABLE_HANDWRITING + " where _ID = " + id_handwriting + "";
			Cursor handwritingCursor = MultiMemoActivity.mDatabase.rawQuery(SQL);
			if (handwritingCursor.moveToNext()) {
				handwritingUriStr = handwritingCursor.getString(0);
			}
			handwritingCursor.close();
		} else {
			handwritingUriStr = "";
		}

		return handwritingUriStr;
	}


	private void viewMemo(int position) {
		MemoListItem item = (MemoListItem) mMemoListAdapter.getItem(position);

		// 메모 보기 액티비티 띄우기
		Intent intent = new Intent(getApplicationContext(), MemoInsertActivity.class);
		intent.putExtra(KEY_MEMO_MODE, MODE_VIEW);
		intent.putExtra(KEY_MEMO_ID, item.getId());
		intent.putExtra(KEY_MEMO_DATE, item.getData(0));
		intent.putExtra(KEY_MEMO_TEXT, item.getData(1));

		intent.putExtra(KEY_ID_HANDWRITING, item.getData(2));
		intent.putExtra(KEY_URI_HANDWRITING, item.getData(3));

		intent.putExtra(KEY_ID_PHOTO, item.getData(4));
		intent.putExtra(KEY_URI_PHOTO, item.getData(5));

		intent.putExtra(KEY_ID_VIDEO, item.getData(6));
		intent.putExtra(KEY_URI_VIDEO, item.getData(7));

		intent.putExtra(KEY_ID_VOICE, item.getData(8));
		intent.putExtra(KEY_URI_VOICE, item.getData(9));

		startActivityForResult(intent, REQ_VIEW_ACTIVITY);
	}


	/**
	 * 다른 액티비티의 응답 처리
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case REQ_INSERT_ACTIVITY:
				if (resultCode == RESULT_OK) {
					loadMemoListData();
				}

				break;

			case REQ_VIEW_ACTIVITY:
				loadMemoListData();

				break;
			case REQ_CODE_TWIT_LOGIN:
				OAuthAccessTokenThread thread = new OAuthAccessTokenThread(data);
				thread.start();

				break;



		}
	}

	//트위터 연동
	private void updateStatus(String statusText) {

		UpdateStatusThread thread = new UpdateStatusThread(statusText);
		thread.start();

	}

	class UpdateStatusThread extends Thread {
		String statusText;

		public UpdateStatusThread(String inText) {
			statusText = inText;
		}

		public void run() {
			try {
				Status status = TwitInstance.updateStatus(statusText);
				final Date curDate = status.getCreatedAt();

				mHandler.post(new Runnable() {
					public void run() {
						Toast.makeText(getApplicationContext(), "글을 업데이트했습니다 : " + DateFormat.format(curDate), Toast.LENGTH_SHORT).show();

						showUserTimeline();
					}
				});

			} catch(Exception ex) {
				ex.printStackTrace();
			}

		}
	}



	private void connect() {
		Log.d(TAG, "connect() called.");

		if (TwitLogin) {
			Log.d(TAG, "twitter already logged in.");
			Toast.makeText(getBaseContext(), "twitter already logged in.", Toast.LENGTH_LONG).show();

			try {
				ConfigurationBuilder builder = new ConfigurationBuilder();

				builder.setOAuthAccessToken(TWIT_KEY_TOKEN);
				builder.setOAuthAccessTokenSecret(TWIT_KEY_TOKEN_SECRET);
				builder.setOAuthConsumerKey(TWIT_CONSUMER_KEY);
				builder.setOAuthConsumerSecret(TWIT_CONSUMER_SECRET);

				Configuration config = builder.build();
				TwitterFactory tFactory = new TwitterFactory(config);
				TwitInstance = tFactory.getInstance();

				Toast.makeText(getBaseContext(), "twitter connected.", Toast.LENGTH_LONG).show();

			} catch (Exception ex) {
				ex.printStackTrace();
			}

//			showUserTimeline();

		} else {

			RequestTokenThread thread = new RequestTokenThread();
			thread.start();

		}

	}

	/**
	 * RequestToken 요청 스레드
	 */
	class RequestTokenThread extends Thread {
		public void run() {

			try {
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setDebugEnabled(true);
				builder.setOAuthConsumerKey(TWIT_CONSUMER_KEY);
				builder.setOAuthConsumerSecret(TWIT_CONSUMER_SECRET);

				TwitterFactory factory = new TwitterFactory(builder.build());
				Twitter mTwit = factory.getInstance();
				final RequestToken mRequestToken = mTwit.getOAuthRequestToken();
				String outToken = mRequestToken.getToken();
				String outTokenSecret = mRequestToken.getTokenSecret();

				Log.d(TAG, "Request Token : " + outToken + ", " + outTokenSecret);
				Log.d(TAG, "AuthorizationURL : " + mRequestToken.getAuthorizationURL());

				TwitInstance = mTwit;
				TwitRequestToken = mRequestToken;


				mHandler.post(new Runnable() {
					public void run() {

						Intent intent = new Intent(getApplicationContext(), TwitLogin.class);
						intent.putExtra("authUrl", mRequestToken.getAuthorizationURL());
						startActivityForResult(intent, REQ_CODE_TWIT_LOGIN);

					}
				});

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}
	}

	/**
	 * 다른 액티비티로부터의 응답 처리
	 */
//	protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
//		super.onActivityResult(requestCode, resultCode, resultIntent);
//
//		if (resultCode == RESULT_OK) {
//			if (requestCode == BasicInfo.REQ_CODE_TWIT_LOGIN) {
//
//				OAuthAccessTokenThread thread = new OAuthAccessTokenThread(resultIntent);
//				thread.start();
//
//			}
//		}
//	}


	class OAuthAccessTokenThread extends Thread {
		Intent resultIntent;

		public OAuthAccessTokenThread(Intent intent) {
			resultIntent = intent;
		}

		public void run() {
			try {
				Twitter mTwit = TwitInstance;

				AccessToken mAccessToken = mTwit.getOAuthAccessToken(TwitRequestToken, resultIntent.getStringExtra("oauthVerifier"));

				TwitLogin = true;
				TWIT_KEY_TOKEN = mAccessToken.getToken();
				TWIT_KEY_TOKEN_SECRET = mAccessToken.getTokenSecret();

				TwitAccessToken = mAccessToken;

				TwitScreenName = mTwit.getScreenName();

				mHandler.post(new Runnable() {
					public void run() {
						Toast.makeText(getBaseContext(), "Twitter connection succeeded : " + TWIT_KEY_TOKEN, Toast.LENGTH_LONG).show();

//						showUserTimeline();
					}
				});

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}


	private void showUserTimeline() {
		Log.d(TAG, "showUserTimeline() called.");

		connectBtn.setVisibility(View.GONE);
//		nameText.setVisibility(View.VISIBLE);
		nameText.setText(TwitScreenName);

		// UserTimeline 요청
		GetUserTimelineThread thread = new GetUserTimelineThread();
		thread.start();

	}


	class GetUserTimelineThread extends Thread {
		public void run() {
			getUserTimeline();
		}

		/**
		 * UserTimeline 요청
		 */
		private void getUserTimeline() {
			Twitter mTwit = TwitInstance;

			try {
				final List<Status> statuses = mTwit.getUserTimeline();

//				mHandler.post(new Runnable() {
//					public void run() {
//						statusAdapter.setListItems(statuses);
//						statusAdapter.notifyDataSetChanged();
//					}
//				});

			} catch(Exception ex) {
				ex.printStackTrace();
			}

		}

	}


	protected void onPause() {
		super.onPause();

		saveProperties();
	}

	protected void onResume() {
		super.onResume();

		loadProperties();
	}

	private void saveProperties() {
		SharedPreferences pref = getSharedPreferences("TWIT", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		editor.putBoolean("TwitLogin", TwitLogin);
		editor.putString("TWIT_KEY_TOKEN", TWIT_KEY_TOKEN);
		editor.putString("TWIT_KEY_TOKEN_SECRET", TWIT_KEY_TOKEN_SECRET);

		editor.commit();
	}

	private void loadProperties() {
		SharedPreferences pref = getSharedPreferences("TWIT", MODE_PRIVATE);

		TwitLogin = pref.getBoolean("TwitLogin", false);
		TWIT_KEY_TOKEN = pref.getString("TWIT_KEY_TOKEN", "");
		TWIT_KEY_TOKEN_SECRET = pref.getString("TWIT_KEY_TOKEN_SECRET", "");

	}


}