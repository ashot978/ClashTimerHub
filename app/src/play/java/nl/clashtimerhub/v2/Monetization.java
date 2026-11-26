package nl.clashtimerhub.v2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Closed-test prototype. Uses only official test ad IDs; no live monetization. */
final class Monetization {
 private static final String PRODUCT="remove_ads";
 private static final String TEST_APP_OPEN="ca-app-pub-3940256099942544/9257395921";
 private static final long STARTUP_TIMEOUT_MS=4000L;
 private static final ColdStartAdPolicy PROCESS=new ColdStartAdPolicy();
 private final Activity activity;
 private final Runnable refresh;
 private final SharedPreferences prefs;
 private final ConsentInformation consent;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final BillingClient billing;
 private final Set<String> acknowledging=new HashSet<>();
 private boolean resumed,closed,connecting,ownershipKnown,premium,pendingPurchase;
 private boolean launchClaimed,consentRequested,consentReady,loading,consentShowing,adRequested,adShowing,displayed;
 private boolean restoreRequested;
 private int queryGeneration;
 private Runnable finishStartup;
 private ProductDetails product;
 private ProductDetails.OneTimePurchaseOfferDetails offer;
 private final Runnable timeout=this::finish;

 Monetization(Activity activity,Runnable refresh){
  this.activity=activity;
  this.refresh=refresh;
  prefs=activity.getSharedPreferences("playMonetization",Activity.MODE_PRIVATE);
  premium=prefs.getBoolean("adFree",false);
  consent=UserMessagingPlatform.getConsentInformation(activity);
  billing=BillingClient.newBuilder(activity.getApplicationContext())
   .setListener((result,purchases)->onMain(()->purchasesUpdated(result,purchases)))
   .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
   .enableAutoServiceReconnection()
   .build();
 }

 boolean enabled(){return true;}

 boolean shouldGateStartup(boolean newInstance,Intent intent){
  boolean launcher=intent!=null&&Intent.ACTION_MAIN.equals(intent.getAction())
   &&intent.hasCategory(Intent.CATEGORY_LAUNCHER)&&!intent.hasExtra("openAccount")
   &&!intent.hasExtra("openSettings")&&!intent.hasExtra("widgetEnableOverlay");
  launchClaimed=PROCESS.claim(newInstance,launcher);
  if(!launchClaimed)return false;
  int count=prefs.getInt("launcherStarts",0);
  if(count<Integer.MAX_VALUE)count++;
  prefs.edit().putInt("launcherStarts",count).apply();
  return ColdStartAdPolicy.eligible(count,premium);
 }

 void renderLoading(){
  LinearLayout screen=new LinearLayout(activity);
  screen.setOrientation(LinearLayout.VERTICAL);
  screen.setGravity(Gravity.CENTER);
  screen.setPadding(24,24,24,24);
  screen.setBackgroundColor(0xff160f25);
  TextView title=new TextView(activity);
  title.setText("CLASH TIMER HUB");title.setTextSize(24);title.setTextColor(0xffffd88a);
  title.setGravity(Gravity.CENTER);screen.addView(title);
  ProgressBar progress=new ProgressBar(activity);screen.addView(progress);
  TextView subtitle=new TextView(activity);
  subtitle.setText(text("App wordt gestart…","Starting app…"));subtitle.setTextColor(Color.WHITE);
  subtitle.setGravity(Gravity.CENTER);screen.addView(subtitle);
  Button skip=new Button(activity);
  skip.setText(text("Doorgaan","Continue"));skip.setOnClickListener(v->skipStartup());
  screen.addView(skip);
  activity.setContentView(screen);
 }

 void onResume(){
  resumed=true;
  connect();
  // Non-ad launches may refresh the privacy choices, but never present a surprise form.
  if(launchClaimed&&!ColdStartAdPolicy.eligible(prefs.getInt("launcherStarts",0),premium))
   requestConsent(false);
 }

 void beginStartup(Runnable finish){
  if(closed)return;
  if(finishStartup!=null)return;
  finishStartup=finish;
  loading=true;
  if(premium){finish();return;}
  handler.postDelayed(timeout,STARTUP_TIMEOUT_MS);
  requestConsent(true);
  maybeAd();
 }

 void onPause(){
  resumed=false;
  // Never show a delayed ad when the user returns from another app.
  if(loading&&!adShowing&&!consentShowing)finish();
 }

 void skipStartup(){
  if(!adShowing&&!consentShowing)finish();
 }

 private void finish(){
  handler.removeCallbacks(timeout);
  loading=false;
  Runnable continuation=finishStartup;finishStartup=null;
  if(!closed&&continuation!=null)continuation.run();
 }

 void close(){
  closed=true;resumed=false;loading=false;finishStartup=null;
  handler.removeCallbacksAndMessages(null);
  billing.endConnection();
 }

 private void requestConsent(boolean duringStartup){
  if(consentRequested)return;
  consentRequested=true;
  consent.requestConsentInfoUpdate(activity,new ConsentRequestParameters.Builder().build(),
   ()->onMain(()->{
    if(closed)return;
    refresh.run();
    if(!duringStartup||!loading)return;
    if(consent.canRequestAds()){
     consentReady=true;maybeAd();
    }else if(resumed&&consent.isConsentFormAvailable()){
     // Load separately: do not let a late consent form jump in over the main UI.
     UserMessagingPlatform.loadConsentForm(activity,form->onMain(()->{
      if(!loading||!resumed||closed)return;
      consentShowing=true;handler.removeCallbacks(timeout);
      form.show(activity,error->onMain(()->{consentShowing=false;refresh.run();finish();}));
     }),error->onMain(this::finish));
    }else finish();
   }),error->onMain(()->{
    if(closed)return;
    refresh.run();
    if(duringStartup&&loading)finish(); // fail closed when consent cannot be updated
   }));
 }

 boolean privacyOptionsRequired(){
  return consent.getPrivacyOptionsRequirementStatus()==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
 }

 void showPrivacyOptions(){
  if(!resumed||!privacyOptionsRequired())return;
  UserMessagingPlatform.showPrivacyOptionsForm(activity,error->onMain(()->{
   if(error!=null)toast(text("Privacyopties zijn niet beschikbaar.","Privacy options are unavailable."));
   refresh.run();
  }));
 }

 private boolean mayShow(){
  return !closed&&!activity.isFinishing()&&!activity.isDestroyed()&&
   ColdStartAdPolicy.mayShow(loading,resumed,consentReady&&consent.canRequestAds(),
    ownershipKnown,premium,pendingPurchase,displayed);
 }

 private void maybeAd(){
  if(!loading||closed)return;
  if(ownershipKnown&&(premium||pendingPurchase)){finish();return;}
  if(adRequested||!mayShow())return;
  adRequested=true;
  new Thread(()->MobileAds.initialize(activity.getApplicationContext(),status->onMain(()->{
   if(!mayShow())return;
   AppOpenAd.load(activity,TEST_APP_OPEN,new AdRequest.Builder().build(),
    new AppOpenAd.AppOpenAdLoadCallback(){
     @Override public void onAdLoaded(AppOpenAd ad){
      if(!mayShow())return;
      displayed=true;adShowing=true;handler.removeCallbacks(timeout);
      ad.setFullScreenContentCallback(new FullScreenContentCallback(){
       @Override public void onAdDismissedFullScreenContent(){onMain(()->{adShowing=false;finish();});}
       @Override public void onAdFailedToShowFullScreenContent(AdError error){onMain(()->{adShowing=false;finish();});}
      });
      try{ad.show(activity);}catch(RuntimeException error){adShowing=false;finish();}
     }
     @Override public void onAdFailedToLoad(LoadAdError error){onMain(Monetization.this::finish);}
    });
  })),"cth-test-ads").start();
 }

 private void connect(){
  if(closed)return;
  // A separately installed preview cannot own the production Play product.
  // Only this isolated debug package may exercise demo ads without Billing.
  if(BuildConfig.ISOLATED_PREVIEW){ownershipKnown=true;pendingPurchase=false;maybeAd();return;}
  if(billing.isReady()){queryOwnership();queryProduct();return;}
  if(connecting)return;
  connecting=true;
  billing.startConnection(new BillingClientStateListener(){
   @Override public void onBillingSetupFinished(BillingResult result){onMain(()->{
    connecting=false;
    if(closed)return;
    if(ok(result)){queryOwnership();queryProduct();}
    else{if(restoreRequested){restoreRequested=false;toast(text("Google Play niet beschikbaar.","Google Play unavailable."));}if(loading)finish();}
   });}
   @Override public void onBillingServiceDisconnected(){onMain(()->{connecting=false;});}
  });
 }

 private void queryOwnership(){
  int generation=++queryGeneration;
  billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
   .setProductType(BillingClient.ProductType.INAPP).build(),(result,purchases)->onMain(()->{
    if(closed||generation!=queryGeneration)return;
    if(!ok(result)){
     if(restoreRequested){restoreRequested=false;toast(text("Herstellen mislukt. Probeer opnieuw.","Restore failed. Try again."));}
     if(loading)finish();return;
    }
    boolean owned=false,pending=false;
    for(Purchase purchase:purchases){
     if(!purchase.getProducts().contains(PRODUCT))continue;
     if(purchase.getPurchaseState()==Purchase.PurchaseState.PURCHASED){owned=true;acknowledge(purchase);}
     else if(purchase.getPurchaseState()==Purchase.PurchaseState.PENDING)pending=true;
    }
    ownershipKnown=true;pendingPurchase=pending;setPremium(owned);
    if(restoreRequested){
     restoreRequested=false;
     toast(owned?text("Aankoop hersteld.","Purchase restored."):
      pending?text("Betaling is nog in behandeling.","Payment is pending."):
      text("Geen aankoop gevonden op dit Google Play-account.","No purchase found on this Google Play account."));
    }
    refresh.run();maybeAd();
   }));
 }

 private void queryProduct(){
  if(!BuildConfig.MONETIZATION_TEST_PURCHASES)return;
  QueryProductDetailsParams params=QueryProductDetailsParams.newBuilder().setProductList(
   Collections.singletonList(QueryProductDetailsParams.Product.newBuilder()
    .setProductId(PRODUCT).setProductType(BillingClient.ProductType.INAPP).build())).build();
  billing.queryProductDetailsAsync(params,(result,details)->onMain(()->{
   if(closed)return;
   product=null;offer=null;
   if(ok(result)){
    for(ProductDetails detail:details.getProductDetailsList()){
     if(!PRODUCT.equals(detail.getProductId()))continue;
     // Configure exactly one permanent "buy" offer in Play Console.
     ProductDetails.OneTimePurchaseOfferDetails candidate=detail.getOneTimePurchaseOfferDetails();
     if(candidate!=null){product=detail;offer=candidate;break;}
    }
   }
   refresh.run();
  }));
 }

 String statusText(){
  if(BuildConfig.ISOLATED_PREVIEW)return text("Preview · €2,99 gepland · geen betaling","Preview · €2.99 planned · no payment");
  if(premium)return text("Actief · geen advertenties","Active · no ads");
  if(pendingPurchase)return text("Betaling in behandeling","Payment pending");
  if(!BuildConfig.MONETIZATION_TEST_PURCHASES)
   return text("Testmodus · aankopen nog uitgeschakeld","Test mode · purchases not enabled");
  if(offer==null)return text("Nog niet beschikbaar via Google Play","Not yet available from Google Play");
  return text("Eenmalig · geen abonnement · ","One-time · no subscription · ")+offer.getFormattedPrice();
 }

 void purchase(){
  if(premium){toast(text("Je hebt de advertentievrije versie al.","You already own the ad-free version."));return;}
  if(pendingPurchase){toast(text("Betaling is nog in behandeling.","Payment is pending."));return;}
  if(!BuildConfig.MONETIZATION_TEST_PURCHASES){
   toast(text("Aankopen zijn uitgeschakeld in deze testbuild.","Purchases are disabled in this test build."));return;
  }
  if(!resumed||!billing.isReady()||product==null||offer==null||!ownershipKnown){
   toast(text("Product nog niet beschikbaar. Probeer opnieuw.","Product unavailable. Try again."));connect();return;
  }
  BillingFlowParams.ProductDetailsParams.Builder line=BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product);
  String token=offer.getOfferToken();
  if(token!=null&&!token.isEmpty())line.setOfferToken(token);
  BillingResult result=billing.launchBillingFlow(activity,BillingFlowParams.newBuilder()
   .setProductDetailsParamsList(Collections.singletonList(line.build())).build());
  if(result.getResponseCode()==BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)restore();
  else if(!ok(result))toast(text("Aankoop kon niet worden gestart.","Could not start purchase."));
 }

 void restore(){
  if(BuildConfig.ISOLATED_PREVIEW){toast(text("Aankopen herstellen wordt getest via Google Play, niet in deze preview.","Purchase restoration is tested through Google Play, not this preview."));return;}
  restoreRequested=true;
  if(billing.isReady())queryOwnership();else connect();
 }

 private void purchasesUpdated(BillingResult result,List<Purchase> purchases){
  if(closed)return;
  if(ok(result)&&purchases!=null){
   // Test-only client entitlement. Production requires backend purchase verification.
   ++queryGeneration;
   for(Purchase purchase:purchases){
    if(!purchase.getProducts().contains(PRODUCT))continue;
    if(purchase.getPurchaseState()==Purchase.PurchaseState.PURCHASED){
     setPremium(true);pendingPurchase=false;acknowledge(purchase);
    }else if(purchase.getPurchaseState()==Purchase.PurchaseState.PENDING)pendingPurchase=true;
   }
   refresh.run();queryOwnership();
  }else if(result.getResponseCode()==BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)restore();
  else if(result.getResponseCode()!=BillingClient.BillingResponseCode.USER_CANCELED)
   toast(text("Aankoop niet afgerond.","Purchase not completed."));
 }

 private void setPremium(boolean value){
  if(premium==value)return;
  premium=value;prefs.edit().putBoolean("adFree",value).apply();
 }

 private void acknowledge(Purchase purchase){
  String token=purchase.getPurchaseToken();
  if(purchase.isAcknowledged()||!acknowledging.add(token))return;
  billing.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(),
   result->onMain(()->acknowledging.remove(token)));
 }

 private static boolean ok(BillingResult result){return result.getResponseCode()==BillingClient.BillingResponseCode.OK;}
 private void onMain(Runnable action){if(Looper.myLooper()==Looper.getMainLooper()){if(!closed)action.run();}else handler.post(()->{if(!closed)action.run();});}
 private String text(String nl,String en){return I18n.english(activity)?en:nl;}
 private void toast(String text){if(resumed&&!closed)Toast.makeText(activity,text,Toast.LENGTH_SHORT).show();}
}
