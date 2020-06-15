package com.reactlibrary;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.IsEnvReadyResult;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.support.api.client.Status;

import java.util.ArrayList;
import java.util.List;

public class AppGalleryIapModule extends ReactContextBaseJavaModule {

    private final String SUCCESS = "SUCCESS";
    private final String E_NO_PRODUCTS_ADDED = "E_NO_PRODUCTS_ADDED";
    private final String E_PURCHASE_INTENT_NO_DATA = "E_PURCHASE_INTENT_NO_DATA";

    private final int LOGIN_RESULT = 1111;
    private final int CREATE_PURCHASE_INTENT_RESULT = 2222;

    private Promise mAppGalleryIapPromise;
    private ReactContext reactContext;

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode == LOGIN_RESULT) {
                if (data != null) {
                    int returnCode = data.getIntExtra("returnCode", 1);
                    mAppGalleryIapPromise.resolve(returnCode);
                }
                mAppGalleryIapPromise = null;
                return;
            }

            if (requestCode == CREATE_PURCHASE_INTENT_RESULT) {
                if (data == null) {
                    mAppGalleryIapPromise.reject(E_PURCHASE_INTENT_NO_DATA, "No callback data.");
                }

                PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(reactContext).parsePurchaseResultInfoFromIntent(data);
                switch(purchaseResultInfo.getReturnCode()) {
                    case OrderStatusCode.ORDER_STATE_CANCEL:
                        mAppGalleryIapPromise.reject(String.valueOf(OrderStatusCode.ORDER_STATE_CANCEL), "Order cancelled by user.");
                        break;

                    case OrderStatusCode.ORDER_STATE_FAILED:
                        mAppGalleryIapPromise.reject(String.valueOf(OrderStatusCode.ORDER_STATE_FAILED), "Order failed.");
                        break;

                    case OrderStatusCode.ORDER_PRODUCT_OWNED:
                        mAppGalleryIapPromise.reject(String.valueOf(OrderStatusCode.ORDER_PRODUCT_OWNED), "Item already owned.");
                        break;

                    case OrderStatusCode.ORDER_STATE_SUCCESS:
                        String inAppPurchaseData = purchaseResultInfo.getInAppPurchaseData();
                        String inAppPurchaseDataSignature = purchaseResultInfo.getInAppDataSignature();

                        WritableMap response = Arguments.createMap();
                        response.putString("data", inAppPurchaseData);
                        response.putString("signature", inAppPurchaseDataSignature);

                        mAppGalleryIapPromise.resolve(response);
                        break;
                    default:
                        break;
                }

                mAppGalleryIapPromise = null;
                return;
            }
        }
    };

    public AppGalleryIapModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        reactContext.addActivityEventListener(mActivityEventListener);
    }

    @Override
    public String getName() {
        return "AppGalleryIap";
    }

    @ReactMethod
    public void initialize(final Promise promise) {
        Task<IsEnvReadyResult> task = Iap.getIapClient(getCurrentActivity()).isEnvReady();
        task.addOnSuccessListener(new OnSuccessListener<IsEnvReadyResult>() {
            @Override
            public void onSuccess(IsEnvReadyResult result) {
                promise.resolve(SUCCESS);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                IapApiException apiException = (IapApiException) e;
                Status status = apiException.getStatus();

                if (status.getStatusCode() == OrderStatusCode.ORDER_HWID_NOT_LOGIN) {
                    if (status.hasResolution()) {
                        try {
                            mAppGalleryIapPromise = promise;
                            status.startResolutionForResult(getCurrentActivity(), LOGIN_RESULT);
                        } catch (IntentSender.SendIntentException exp) {
                            promise.reject(String.valueOf(OrderStatusCode.ORDER_HWID_NOT_LOGIN), "Requires login. Attempt failed.");
                        }
                        return;
                    }
                }

                promise.reject(String.valueOf(status.getStatusCode()), status.getStatusMessage());
            }
        });
    }

    @ReactMethod
    public void fetchProducts(int priceType, ReadableArray productIds, final Promise promise) {
        List<String> productIdList = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            productIdList.add(productIds.getString(i));
        }
        ProductInfoReq req = new ProductInfoReq();
        req.setPriceType(priceType);
        req.setProductIds(productIdList);

        if (productIdList.size() <= 0) {
            promise.reject(E_NO_PRODUCTS_ADDED, "Please add products first before fetching.");
            return;
        }

        Task<ProductInfoResult> task = Iap.getIapClient(getCurrentActivity()).obtainProductInfo(req);
        task.addOnSuccessListener(new OnSuccessListener<ProductInfoResult>() {
            @Override
            public void onSuccess(ProductInfoResult result) {
                List<ProductInfo> productList = result.getProductInfoList();
                WritableArray response = Arguments.createArray();
                for (int i = 0; i < productList.size(); i++) {
                    WritableMap product = Arguments.createMap();
                    product.putString("id", productList.get(i).getProductId());
                    product.putString("currency", productList.get(i).getCurrency());
                    product.putString("price", productList.get(i).getPrice());
                    product.putString("name", productList.get(i).getProductName());
                    product.putString("description", productList.get(i).getProductDesc());

                    response.pushMap(product);
                }
                Log.d("WW_HMS", productList.get(0).getCurrency());
                promise.resolve(response);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                promise.reject(e);
            }
        });
    }

    @ReactMethod
    public void purchaseProduct(int priceType, String productId, boolean isTesting, final Promise promise) {
        PurchaseIntentReq req = new PurchaseIntentReq();
        req.setProductId(productId);
        req.setPriceType(priceType);

        if (isTesting) {
            req.setDeveloperPayload("test");
        }

        final Activity activity = getCurrentActivity();
        Task<PurchaseIntentResult> task = Iap.getIapClient(activity).createPurchaseIntent(req);
        task.addOnSuccessListener(new OnSuccessListener<PurchaseIntentResult>() {
            @Override
            public void onSuccess(PurchaseIntentResult result) {
                Status status = result.getStatus();
                if (status.hasResolution()) {
                    try {
                        mAppGalleryIapPromise = promise;
                        status.startResolutionForResult(activity, CREATE_PURCHASE_INTENT_RESULT);
                    } catch (IntentSender.SendIntentException exp) {
                        promise.reject(String.valueOf(OrderStatusCode.ORDER_STATE_FAILED), "Purchase failed. Message : " + exp.getMessage());
                    }
                    return;
                }

                promise.reject(String.valueOf(status.getStatusCode()), status.getStatusMessage());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException)e;
                    Status status = apiException.getStatus();
                    int returnCode = apiException.getStatusCode();

                    promise.reject(String.valueOf(returnCode), status.getErrorString());
                } else {
                    promise.reject(e);
                }
            }
        });
    }

    @ReactMethod
    public void consumePurchase(String purchaseToken, final Promise promise) {
        ConsumeOwnedPurchaseReq req = new ConsumeOwnedPurchaseReq();
        req.setPurchaseToken(purchaseToken);
        Activity activity = getCurrentActivity();
        Task<ConsumeOwnedPurchaseResult> task = Iap.getIapClient(activity).consumeOwnedPurchase(req);
        task.addOnSuccessListener(new OnSuccessListener<ConsumeOwnedPurchaseResult>() {
            @Override
            public void onSuccess(ConsumeOwnedPurchaseResult result) {
                promise.resolve(result.getConsumePurchaseData());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException)e;
                    Status status = apiException.getStatus();
                    int returnCode = apiException.getStatusCode();

                    promise.reject(String.valueOf(returnCode), status.getErrorString());
                } else {
                    promise.reject(e);
                }
            }
        });
    }

    @ReactMethod
    public void fetchOwnedPurchases(int priceType, final Promise promise) {
        OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
        ownedPurchasesReq.setPriceType(priceType);
        Activity activity = getCurrentActivity();
        Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchasesReq);
        task.addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
            @Override
            public void onSuccess(OwnedPurchasesResult result) {
                WritableArray response = Arguments.createArray();

                if (result != null && result.getInAppPurchaseDataList() != null) {
                    for (int i = 0; i < result.getInAppPurchaseDataList().size(); i++) {
                        String inAppPurchaseData = result.getInAppPurchaseDataList().get(i);
                        String inAppSignature = result.getInAppSignature().get(i);

                        WritableMap inAppPurchase = Arguments.createMap();
                        inAppPurchase.putString("data", inAppPurchaseData);
                        inAppPurchase.putString("signature", inAppSignature);

                        response.pushMap(inAppPurchase);
                    }
                }

                promise.resolve(response);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException)e;
                    Status status = apiException.getStatus();
                    int returnCode = apiException.getStatusCode();

                    promise.reject(String.valueOf(returnCode), status.getErrorString());
                } else {
                    promise.reject(e);
                }
            }
        });
    }
}
