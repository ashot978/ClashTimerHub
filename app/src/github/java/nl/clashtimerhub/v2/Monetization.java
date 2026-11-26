package nl.clashtimerhub.v2;

import android.app.Activity;
import android.content.Intent;

/** GitHub distribution stays ad-free; all monetization dependencies are Play-only. */
final class Monetization {
 Monetization(Activity activity,Runnable refresh){}
 boolean enabled(){return false;}
 boolean shouldGateStartup(boolean newInstance,Intent intent){return false;}
 void renderLoading(){}
 void beginStartup(Runnable finish){finish.run();}
 void skipStartup(){}
 void onResume(){}
 void onPause(){}
 void close(){}
 String statusText(){return "";}
 void purchase(){}
 void restore(){}
 boolean privacyOptionsRequired(){return false;}
 void showPrivacyOptions(){}
}
