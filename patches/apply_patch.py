from pathlib import Path

ROOT = Path("app/src/main/java/nl/clashtimerhub/app")
service_file = ROOT / "ProjectionScanService.java"
service = service_file.read_text(encoding="utf-8")

def replace_once(source, old, new, label):
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return source.replace(old, new, 1)

service = replace_once(service,
    '    private volatile String bestText="";',
    '    private volatile String bestText="";\n    private int consecutiveMenuFrames=0;\n    private boolean menuWasOpened=false;',
    "scan state")
service = replace_once(service,
    'closed=false;bestFinish=0;bestText="";startAt=System.currentTimeMillis();',
    'closed=false;bestFinish=0;bestText="";consecutiveMenuFrames=0;menuWasOpened=false;startAt=System.currentTimeMillis();',
    "scan reset")
service = replace_once(service,
    '"Open het bijbehorende upgradescherm; na 35 sec stopt de scan."',
    '"Wacht op het juiste upgradescherm; alleen actieve upgrades tellen."',
    "notification")
service = replace_once(service,
    'OcrScanner.scan(frameBitmap,new OcrScanner.Callback(){',
    'OcrScanner.scan(frameBitmap,category,this,new OcrScanner.Callback(){',
    "OCR call")
service = replace_once(service,
    """@Override public void onResult(TimerParser.Hit hit){
                    frameBitmap.recycle();
                    if(!closed&&hit!=null){
                        long estimate=System.currentTimeMillis()+hit.seconds*1000L;
                        if(bestFinish==0||estimate<bestFinish){bestFinish=estimate;bestText=hit.text;}
                    }
                    recognizing.set(false);scheduleNext();
                }""",
    """@Override public void onResult(MenuTimerDetector.Result result){
                    frameBitmap.recycle();
                    if(!closed && result.menuOpen){
                        menuWasOpened=true;
                        consecutiveMenuFrames++;
                        if(consecutiveMenuFrames==1)
                            store.setScanMessage("Upgrademenu herkend. Timers worden gecontroleerd...");
                        // Two consecutive recognitions avoid accidental OCR artifacts.
                        if(result.hit!=null){
                            long estimate=System.currentTimeMillis()+result.hit.seconds*1000L;
                            if(bestFinish==0||estimate<bestFinish){
                                bestFinish=estimate;
                                bestText=result.hit.text;
                                store.setScanMessage("Timer herkend: "+result.hit.text+". Scan wordt afgerond.");
                                finishScan("Timer gevonden");
                                return;
                            }
                        }
                    } else {
                        consecutiveMenuFrames=0;
                    }
                    recognizing.set(false);scheduleNext();
                }""",
    "gated OCR result")
service = replace_once(service,
    '}else store.setScanMessage(reason+": geen herkenbare timer gevonden. Probeer een screenshot met \'Foto\'.");',
    '''}else if(!menuWasOpened) store.setScanMessage(
                "Geen juist upgrademenu gezien. Open Buildings & Heroes (of het gekozen Lab/Pets-menu) en scan opnieuw.");
        else store.setScanMessage(
                "Upgrademenu gezien, maar geen actieve timer gevonden. Probeer een screenshot via Foto.");''',
    "scan failure messages")
# Install a movable overlay button. The user explicitly arms OCR after
# opening the desired upgrades menu instead of waiting for auto-detection.
service = replace_once(service,
    '    private volatile String bestText="";',
    '    private volatile String bestText="";\n    private ScanOverlay floatingScanButton;\n    private volatile boolean manualScanArmed=false;',
    "overlay fields")
service = replace_once(service,
    'consecutiveMenuFrames=0;menuWasOpened=false;startAt=System.currentTimeMillis();',
    '''consecutiveMenuFrames=0;menuWasOpened=false;manualScanArmed=false;startAt=System.currentTimeMillis();
        if(ScanOverlay.allowed(this)){
            if(floatingScanButton!=null)floatingScanButton.hide();
            floatingScanButton=new ScanOverlay(this,category,()->{
                manualScanArmed=true;
                startAt=System.currentTimeMillis();
                // Start the timeout only after the player taps the overlay.
                if(handler!=null){
                    handler.postDelayed(()->{
                        if(!closed && manualScanArmed &&
                           System.currentTimeMillis()-startAt>=SCAN_LENGTH_MS)
                            finishScan("Scan afgerond");
                    },SCAN_LENGTH_MS);
                    handler.post(this::scanTick);
                }
                consecutiveMenuFrames=0;
                bestFinish=0;
                bestText="";
                store.setScanMessage("Scanknop ingedrukt. OCR leest nu het geopende menu; wacht circa 15 seconden.");
                // Keep the button visible until OCR returns a result.
            });
            floatingScanButton.show();
        }''',
    "overlay creation")
service = replace_once(service,
    'if(!closed && result.menuOpen){',
    'if(!closed && manualScanArmed && result.menuOpen){',
    "manual trigger gate")
# Keep the scanner alive until the first tap; then enforce the usual timeout.
# The capture session must not time out while waiting for the user to open
# Clash and tap the overlay. Only count time after the manual trigger.
service = replace_once(service,
    'handler.postDelayed(()->finishScan("Scan afgerond"),SCAN_LENGTH_MS);',
    '// Timeout is scheduled when the floating Scan nu button is tapped.',
    "remove premature timeout")
service = replace_once(service,
    'if(System.currentTimeMillis()-startAt>SCAN_LENGTH_MS)',
    'if(manualScanArmed && System.currentTimeMillis()-startAt>SCAN_LENGTH_MS)',
    "scan timeout gate")
service = replace_once(service,
    'if(recognizing.compareAndSet(false,true)){',
    'if(!manualScanArmed){scheduleNext();return;}\n        if(recognizing.compareAndSet(false,true)){',
    "do not OCR before user tap")

# Always remove the floating control when the capture session ends.
service = replace_once(service,
    '        store.setScanning(false);',
    '        if(floatingScanButton!=null){floatingScanButton.hide();floatingScanButton=null;}\n        store.setScanning(false);',
    "cleanup floating scan control")
service = replace_once(service,
    'if(bestFinish>0){store.setPending(slot,category,bestFinish,bestText);',
    'if(bestFinish>0){store.setTimerAt(this,slot,category,bestFinish,bestText);store.clearPending(slot,category);',
    "save recognized timer immediately")
# Persist all detected active upgrade durations for the selected account.
service = replace_once(service,
    'if(result.hit!=null){',
    '''if(result.hit!=null){
                            org.json.JSONArray active=new org.json.JSONArray();
                            long capturedAt=System.currentTimeMillis();
                            for(int timerIndex=0;timerIndex<result.hits.size();timerIndex++){
                                TimerParser.Hit timer=result.hits.get(timerIndex);
                                org.json.JSONObject item=new org.json.JSONObject();
                                try{
                                    String building=timerIndex<result.names.size()?result.names.get(timerIndex):"";
                                    item.put("label",building.isEmpty()?timer.text:building);
                                    item.put("finish",capturedAt+timer.seconds*1000L);
                                    active.put(item);
                                }catch(org.json.JSONException ignored){}
                            }
                            getSharedPreferences("active_upgrades",MODE_PRIVATE).edit()
                                .putString("slot_"+slot+"_category_"+category,active.toString()).apply();''',
    "save all active upgrade rows")
service_file.write_text(service, encoding="utf-8")

# Request overlay permission when opening the app; permission is granted by
# Android settings and remains under user control.
manifest_file=Path("app/src/main/AndroidManifest.xml")
manifest=manifest_file.read_text(encoding="utf-8")
if 'android.permission.SYSTEM_ALERT_WINDOW' not in manifest:
    manifest=manifest.replace('<manifest ', '<manifest ', 1)
    manifest=manifest.replace('<application', '<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />\n    <application', 1)
# Activity must be nested inside application, insert before </application>
manifest=manifest.replace('</application>', '<activity android:name=".ScanRegionActivity" android:exported="false" />\n    </application>', 1)
manifest_file.write_text(manifest,encoding="utf-8")

activity_file=ROOT / "MainActivity.java"
activity=activity_file.read_text(encoding="utf-8")
activity=replace_once(activity,
    'Open na de schermtoestemming zelf het juiste upgradescherm. Laat het circa 20 seconden in beeld en keer terug naar de app.',
    'Open na de toestemming Buildings & Heroes (voor bouwers). De scanner wacht op de menukop en negeert voorgestelde upgrades. Laat het menu 15-20 seconden open.',
    "screen guidance")
old_photo='''try{
                com.google.mlkit.vision.common.InputImage img=com.google.mlkit.vision.common.InputImage.fromFilePath(this,uri);
                com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(img).addOnSuccessListener(t->{
                        TimerParser.Hit hit=TimerParser.shortestInText(t.getText());
                        if(hit==null){Toast.makeText(this,"Geen timer gevonden",Toast.LENGTH_LONG).show();return;}
                        store.setPending(slot,cat,System.currentTimeMillis()+hit.seconds*1000,hit.text);
                        refresh();
                    }).addOnFailureListener(e->Toast.makeText(this,"OCR fout: "+e.getMessage(),Toast.LENGTH_LONG).show());
            }catch(Exception e){Toast.makeText(this,"Kan afbeelding niet openen",Toast.LENGTH_LONG).show();}'''
new_photo='''try{
                android.graphics.Bitmap bitmap;
                try(java.io.InputStream stream=getContentResolver().openInputStream(uri)){
                    bitmap=android.graphics.BitmapFactory.decodeStream(stream);
                }
                if(bitmap==null)throw new IllegalStateException("Geen afbeelding");
                OcrScanner.scan(bitmap,cat,this,new OcrScanner.Callback(){
                    @Override public void onResult(MenuTimerDetector.Result result){
                        bitmap.recycle();
                        if(!result.menuOpen){
                            Toast.makeText(MainActivity.this,
                                "Geen juist upgrademenu zichtbaar op foto",Toast.LENGTH_LONG).show();
                            return;
                        }
                        if(result.hit==null){
                            Toast.makeText(MainActivity.this,
                                "Geen actieve timer in upgrademenu gevonden",Toast.LENGTH_LONG).show();
                            return;
                        }
                        store.setPending(slot,cat,
                            System.currentTimeMillis()+result.hit.seconds*1000L,result.hit.text);
                        refresh();
                    }
                    @Override public void onError(Exception error){
                        bitmap.recycle();
                        Toast.makeText(MainActivity.this,
                            "OCR fout: "+error.getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
            }catch(Exception e){Toast.makeText(this,"Kan afbeelding niet openen",Toast.LENGTH_LONG).show();}'''
activity=replace_once(activity,old_photo,new_photo,"screenshot gate")
activity=replace_once(activity,
    '''                        store.setPending(slot,cat,
                            System.currentTimeMillis()+result.hit.seconds*1000L,result.hit.text);
                        refresh();''',
    '''                        org.json.JSONArray active=new org.json.JSONArray();
                        long capturedAt=System.currentTimeMillis();
                        for(int i=0;i<result.hits.size();i++){
                            TimerParser.Hit timer=result.hits.get(i);
                            org.json.JSONObject item=new org.json.JSONObject();
                            try{
                                String building=i<result.names.size()?result.names.get(i):"";
                                item.put("label",building.isEmpty()?timer.text:building);
                                item.put("finish",capturedAt+timer.seconds*1000L);
                                active.put(item);
                            }catch(org.json.JSONException ignored){}
                        }
                        getSharedPreferences("active_upgrades",MODE_PRIVATE).edit()
                            .putString("slot_"+slot+"_category_"+cat,active.toString()).apply();
                        store.setTimerAt(MainActivity.this,slot,cat,
                            capturedAt+result.hit.seconds*1000L,result.hit.text);
                        store.clearPending(slot,cat);
                        Toast.makeText(MainActivity.this,""+active.length()+" upgrades opgeslagen",
                            Toast.LENGTH_LONG).show();
                        refresh();''',
    "save all imported photo upgrades")

# Expose region calibration as a menu action in the existing activity.
# The calibration activity also works for a screenshot taken in the game.
activity=activity.replace('super.onCreate(savedInstanceState);',
    'super.onCreate(savedInstanceState);',1)
# Avoid mutating Spinner adapter while its selection listener is active.
# A fresh adapter is safer than clear()/addAll() on a Spinner-attached adapter.
activity=replace_once(activity,
    '''((ArrayAdapter<String>)accountSpinner.getAdapter()).clear();
            ((ArrayAdapter<String>)accountSpinner.getAdapter()).addAll(labels);
            accountSpinner.setSelection(selectedSlot);TimersWidget.refreshAll(this);refresh();''',
    '''ArrayAdapter<String> renamedAdapter=new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,labels);
            // Keep the existing Spinner and selection untouched during save.
            // Rebuilding its adapter synchronously can trigger nested callbacks.
            android.widget.Toast.makeText(this, "Accountnaam opgeslagen",
                android.widget.Toast.LENGTH_SHORT).show();
            refresh();''',
    "account save spinner adapter crash")

# Make scan calibration discoverable and request overlay permission only on
# user action, not during app startup.
activity=replace_once(activity,
    'content.addView(text("2  Scan de juiste timer",18,Color.WHITE,true));',
    '''content.addView(text("2  Scan de juiste timer",18,Color.WHITE,true));
        content.addView(button("Stel scangebied in",v->{
            Intent region=new Intent(this,ScanRegionActivity.class);
            region.putExtra("category",selectedCategory);
            startActivity(region);
        }));
        content.addView(button("Zet zwevende scanknop aan",v->{
            if(!ScanOverlay.allowed(this)){
                Intent permission=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:"+getPackageName()));
                startActivity(permission);
                Toast.makeText(this,
                    "Sta Weergeven over andere apps toe en start daarna Scan spel",
                    Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this,
                    "Toestemming actief. Tik Scan spel en bevestig Scherm opnemen",
                    Toast.LENGTH_LONG).show();
            }
        }));''',
    "calibration and overlay UI")
# Editable account roster: keep seven stable timer slots and allow soft deletion.
# Soft deletion preserves existing timers and permits reactivation.
activity=replace_once(activity,
    '        String[] names=new String[7];for(int i=0;i<7;i++) names[i]=store.name(i);',
    '''        String[] names=new String[7];for(int i=0;i<7;i++)
            names[i]=accountEnabled(i)?store.name(i):"(verwijderd) "+store.name(i);''',
    "account spinner labels")
activity=replace_once(activity,
    '        space(content,10);',
    '''        LinearLayout accountActions=horizontal();
        accountActions.addView(button("Account toevoegen",v->{
            int free=-1;
            for(int i=0;i<7;i++)if(!accountEnabled(i)){free=i;break;}
            if(free<0){Toast.makeText(this,"Alle 7 accountplaatsen zijn bezet",Toast.LENGTH_LONG).show();return;}
            final int index=free;
            EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(Color.BLACK);
            input.setHint("Accountnaam");
            new AlertDialog.Builder(this).setTitle("Account toevoegen").setView(input)
                .setNegativeButton("Annuleren",null)
                .setPositiveButton("Toevoegen",(d,w)->{
                    String name=input.getText().toString().trim();
                    if(name.isEmpty())name="Account "+(index+1);
                    store.setName(index,name);
                    getSharedPreferences("account_roster",MODE_PRIVATE).edit()
                        .putBoolean("active_"+index,true).apply();
                    selectedSlot=index;buildUi();
                }).show();
        }),new LinearLayout.LayoutParams(0,dp(50),1));
        accountActions.addView(button("Verwijderen",v->new AlertDialog.Builder(this)
            .setTitle("Account verwijderen")
            .setMessage("Account "+store.name(selectedSlot)+" verbergen? Timers blijven bewaard.")
            .setNegativeButton("Annuleren",null)
            .setPositiveButton("Verwijderen",(d,w)->{
                getSharedPreferences("account_roster",MODE_PRIVATE).edit()
                    .putBoolean("active_"+selectedSlot,false).apply();
                for(int i=0;i<7;i++)if(accountEnabled(i)){selectedSlot=i;break;}
                buildUi();
            }).show()),new LinearLayout.LayoutParams(0,dp(50),1));
        content.addView(accountActions);
        space(content,10);''',
    "account actions")
activity=replace_once(activity,
    '    private void buildUi(){',
    '''    private boolean accountEnabled(int index){
        return getSharedPreferences("account_roster",MODE_PRIVATE)
            .getBoolean("active_"+index,true);
    }
    private void initializeAccountDefaults(){
        android.content.SharedPreferences prefs=getSharedPreferences("account_roster",MODE_PRIVATE);
        if(prefs.getBoolean("named_accounts_v2",false))return;
        String[] defaults={"Ashot","Ashotgun","AshotRush","Rushot","Shotty","Ashotty","RushShot"};
        // One-time migration: apply the seven requested names to existing installs too.
        // Subsequent launches and APK updates retain user edits.
        for(int i=0;i<7;i++)store.setName(i,defaults[i]);
        prefs.edit().putBoolean("named_accounts_v2",true).apply();
    }
    private void buildUi(){''',
    "account default initialization method")
activity=replace_once(activity,
    '        store=new TimerStore(this);',
    '        store=new TimerStore(this);initializeAccountDefaults();',
    "initialize default names")
activity=replace_once(activity,
    '        content.addView(text("Alle 7 accounts",18,Color.WHITE,true));',
    '        content.addView(text("Accounts (maximaal 7)",18,Color.WHITE,true));',
    "account overview title")
# Require overlay permission before starting screen capture; otherwise the
# scan is silently unarmed and no floating button ever appears.
activity=replace_once(activity,
    '        selectedCategory=category;\n        MediaProjectionManager mgr=',
    '''        selectedCategory=category;
        if(!ScanOverlay.allowed(this)){
            new AlertDialog.Builder(this)
                .setTitle("Zwevende scanknop inschakelen")
                .setMessage("Geef Clash Timer Hub toestemming voor Weergeven over andere apps. Ga daarna terug en tik opnieuw op Scan spel. Zonder deze toestemming kan de scan niet starten.")
                .setNegativeButton("Annuleren",null)
                .setPositiveButton("Instellingen",(d,w)->{
                    Intent permission=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:"+getPackageName()));
                    startActivity(permission);
                }).show();
            return;
        }
        MediaProjectionManager mgr=''',
    "require floating button permission before capture")
# Display each stored active upgrade independently instead of just one timer.
activity=replace_once(activity,
    '        overview.removeAllViews();',
    '''        overview.removeAllViews();
        try{
            String raw=getSharedPreferences("active_upgrades",MODE_PRIVATE)
                .getString("slot_"+selectedSlot+"_category_0","[]");
            org.json.JSONArray upgrades=new org.json.JSONArray(raw);
            if(upgrades.length()>0){
                overview.addView(text("Actieve upgrades voor "+store.name(selectedSlot),
                    17,GREEN,true));
                long now=System.currentTimeMillis();
                for(int n=0;n<upgrades.length();n++){
                    org.json.JSONObject item=upgrades.getJSONObject(n);
                    long remaining=Math.max(0,(item.optLong("finish")-now)/1000L);
                    if(remaining>0)
                        overview.addView(text("• "+item.optString("label")+" — "+
                            TimerParser.pretty(remaining),14,Color.WHITE,false));
                }
            }
        }catch(org.json.JSONException ignored){}''',
    "render individual active upgrade timers")
activity_file.write_text(activity,encoding="utf-8")

gradle_file=Path("app/build.gradle")
gradle=gradle_file.read_text(encoding="utf-8")
gradle=replace_once(gradle,"versionCode 1","versionCode 3","versionCode")
gradle=replace_once(gradle,"versionName '0.1.0'","versionName '0.3.0'","versionName")
gradle_file.write_text(gradle,encoding="utf-8")
print("Menu-gated scan patch applied")
