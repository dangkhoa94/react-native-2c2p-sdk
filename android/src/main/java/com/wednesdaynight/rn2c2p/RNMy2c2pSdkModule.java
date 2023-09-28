package com.wednesdaynight.rn2c2p;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.ccpp.pgw.sdk.android.builder.PGWSDKParamsBuilder;
import com.ccpp.pgw.sdk.android.core.PGWSDK;
import com.ccpp.pgw.sdk.android.enums.APIEnvironment;

import com.ccpp.pgw.sdk.android.model.core.*;
import com.ccpp.pgw.sdk.android.model.core.PGWSDKParams;
import com.ccpp.pgw.sdk.android.model.PaymentCode;
import com.ccpp.pgw.sdk.android.model.PaymentRequest;
import com.ccpp.pgw.sdk.android.model.api.TransactionResultRequest;
import com.ccpp.pgw.sdk.android.model.api.TransactionResultResponse;
import com.ccpp.pgw.sdk.android.builder.TransactionResultRequestBuilder;
import com.ccpp.pgw.sdk.android.builder.CardPaymentBuilder;
import com.ccpp.pgw.sdk.android.builder.DigitalPaymentBuilder;
import com.ccpp.pgw.sdk.android.callback.APIResponseCallback;
import com.ccpp.pgw.sdk.android.callback.PGWWebViewClientCallback;
import com.ccpp.pgw.sdk.android.callback.PGWWebViewTransactionStatusCallback;
import com.ccpp.pgw.sdk.android.core.authenticate.PGWWebViewClient;
import com.ccpp.pgw.sdk.android.enums.APIResponseCode;


import java.util.HashMap;
import org.json.simple.JSONObject;
import com.auth0.jwt.*;
import com.auth0.jwt.algorithms.*;
import com.auth0.jwt.interfaces.*;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;

public class RNMy2c2pSdkModule extends ReactContextBaseJavaModule {
 
  private final ReactApplicationContext reactContext;
  private Promise promise;
  private static final String TAG = "RNMy2c2pSdkModule";
  private static final String PAYMENT_REQUEST_ERROR = "PAYMENT_REQUEST_ERROR";
  private static final String NO_RESPONSE = "NO_RESPONSE";
  private static final int REQUEST_SDK = 1;
  private static final String ACTIVITY_DOES_NOT_EXIST = "ACTIVITY_DOES_NOT_EXIST";


  private final ActivityEventListener activityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      if (requestCode == REQUEST_SDK) {

        if (data != null) {
          Log.d(TAG, "result code: " + resultCode);
        }

        Log.d(TAG, "response activity " + activity);
        Log.d(TAG, "response requestCode" + requestCode);
        Log.d(TAG, "response data" + data);
        Log.d(TAG, "response data getExtras() " + data.getExtras());

        // PaymentOptionRequest responsePaymentOption = data.getExtras().getParcelable(PGWSDK.RESPONSE);
        Bundle transactionReponse = data.getExtras();

        WritableMap result = Arguments.createMap();
        result.putString("invoiceNo", transactionReponse.getString("invoiceNo"));
        result.putString("type", transactionReponse.getString("type"));
        result.putString("data", transactionReponse.getString("data"));
        result.putString("fallbackData", transactionReponse.getString("fallbackData"));
        result.putString("expiryDescription", transactionReponse.getString("expiryDescription"));
        result.putString("invoiceNo", transactionReponse.getString("invoiceNo"));
        result.putString("responseCode", transactionReponse.getString("responseCode"));
        result.putString("responseDescription", transactionReponse.getString("responseDescription"));

        promise.resolve(result);
      } else {
        promise.reject(NO_RESPONSE, "No response data");
      }
    }

    // @Override
    // public void onTransactionRequestResponse(TransactionResultResponse response) {
      
    // }
  };

  public void onCreate(ReactApplicationContext reactContext) {
    PGWSDKParams pgwsdkParams = new PGWSDKParamsBuilder(reactContext, APIEnvironment.Sandbox).build();
    PGWSDK.initialize(pgwsdkParams);
  }

  @Override
  public String getName() {
    return "RNMy2c2pSdk";
  }

  @ReactMethod
  public void setup(String privateKey, Boolean productionMode) {
    // onCreate();
  }

  private void sendRequest(Object params) {
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      promise.reject(ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
      return;
    }
    Intent intent = new Intent(currentActivity, PGWSDK.class);
    String strName = TAG;
    intent.putExtra("params", strName);
    currentActivity.startActivityForResult(intent, REQUEST_SDK);
  }

  public RNMy2c2pSdkModule(ReactApplicationContext reactContext) {
    super(reactContext);
    onCreate(reactContext);
    this.reactContext = reactContext;
    this.reactContext.addActivityEventListener(activityEventListener);
  }

  @ReactMethod
  public void requestJWTToken(ReadableMap params) {
    try {
      String token="";
      String secretKey = params.getString("secretKey");

      HashMap<String, Object> payload = new HashMap<>();
      payload.put("merchantID", params.getString("merchantID"));
      payload.put("invoiceNo", params.getString("invoiceNo"));
      payload.put("description", params.getString("description"));
      payload.put("amount", params.getString("amount"));
      payload.put("currencyCode", params.getString("currencyCode"));

      try {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
      
        token = JWT.create()
          .withPayload(payload).sign(algorithm);           
      
      } catch (JWTCreationException | IllegalArgumentException e){
        //Invalid Signing configuration / Couldn't convert Claims.
        Log.e(TAG, PAYMENT_REQUEST_ERROR, e);
        e.printStackTrace();
      }

      JSONObject requestData = new JSONObject();
      requestData.put("payload", token);

      try {
        String endpoint = params.getString("endpoint");
        URL obj = new URL(endpoint);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/*+json");
        con.setRequestProperty("Accept", "text/plain");
        con.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(requestData.toString());
        wr.flush();
        wr.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }

        WritableMap result = Arguments.createMap();
        // result.putString("raw", response);
        result.putString("jwtToken", token);
        promise.resolve(token);
        Log.d(TAG, "response token" + token);


        Log.d(TAG, "response response" + response);

        sendRequest(result);
        in.close();
        
      } catch(Exception e) {
        Log.e(TAG, PAYMENT_REQUEST_ERROR, e);
        e.printStackTrace();
      }

    } catch (Exception e) {
      Log.e(TAG, PAYMENT_REQUEST_ERROR, e);
      promise.reject(PAYMENT_REQUEST_ERROR, e.getMessage(), e);
    }
  }

  @ReactMethod
  public void requestPaymentCard(ReadableMap params) {
    PaymentCode paymentCode = new PaymentCode("CC");
   
    PaymentRequest paymentRequest = new CardPaymentBuilder(paymentCode, "4111111111111111")
                                    .setExpiryMonth(12)
                                    .setExpiryYear(2025)
                                    .setSecurityCode("123")
                                    .build();

    String paymentToken = params.getString("paymentToken");
    TransactionResultRequest transactionResultRequest = new TransactionResultRequestBuilder(paymentToken)
                                    .with(paymentRequest)
                                    .build();
    sendRequest(transactionResultRequest);
  }

  @ReactMethod
  public void requestPaymentAlipay(ReadableMap params) {
    PaymentCode paymentCode = new PaymentCode("ALIPAY");
   
    PaymentRequest paymentRequest = new DigitalPaymentBuilder(paymentCode)
                                .setName("Ashley")
                                .setEmail("ashley@engageplus.io")
                                .build();

    String paymentToken = params.getString("paymentToken");
    TransactionResultRequest transactionResultRequest = new TransactionResultRequestBuilder(paymentToken)
                                    .with(paymentRequest)
                                    .build();

    sendRequest(transactionResultRequest);
  }

  private void sendRequest(TransactionResultRequest transactionResultRequest) {
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      promise.reject(ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
      return;
    }
    // Intent intent = new Intent(currentActivity, PGWSDK.class);
    // intent.putExtra(PGWSDKParams, PGWSDK());
    // currentActivity.startActivityForResult(intent, REQUEST_SDK);

    PGWSDK.getInstance().proceedTransaction(transactionResultRequest, new APIResponseCallback<TransactionResultResponse>() {
  
      @Override
      public void onResponse(TransactionResultResponse response) {
   
           if(response.getResponseCode().equals(APIResponseCode.TransactionAuthenticateRedirect) || response.getResponseCode().equals(APIResponseCode.TransactionAuthenticateFullRedirect)) {
   
                 String redirectUrl = response.getData(); //Open WebView
                 promise.resolve(redirectUrl);
           } else if(response.getResponseCode().equals(APIResponseCode.TransactionCompleted)) {
                  String appDeepLink = response.getData(); //Open app
                  promise.resolve(appDeepLink);
                //Inquiry payment result by using invoice no.
           } else {
   
                //Get error response and display error.
           }
      }
   
      @Override
      public void onFailure(Throwable error) {
   
           //Get error response and display error.
      }
    });
  }
}