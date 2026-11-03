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
    'OcrScanner.scan(frameBitmap,category,new OcrScanner.Callback(){',
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
                        // Two consecutive recognitions avoid accidental OCR artifacts.
                        if(consecutiveMenuFrames>=2 && result.hit!=null){
                            long estimate=System.currentTimeMillis()+result.hit.seconds*1000L;
                            if(bestFinish==0||estimate<bestFinish){
                                bestFinish=estimate;
                                bestText=result.hit.text;
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
service_file.write_text(service, encoding="utf-8")

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
                OcrScanner.scan(bitmap,cat,new OcrScanner.Callback(){
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
activity_file.write_text(activity,encoding="utf-8")

gradle_file=Path("app/build.gradle")
gradle=gradle_file.read_text(encoding="utf-8")
gradle=replace_once(gradle,"versionCode 1","versionCode 2","versionCode")
gradle=replace_once(gradle,"versionName '0.1.0'","versionName '0.2.0'","versionName")
gradle_file.write_text(gradle,encoding="utf-8")
print("Menu-gated scan patch applied")
