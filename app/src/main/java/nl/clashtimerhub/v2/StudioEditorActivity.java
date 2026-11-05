package nl.clashtimerhub.v2;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Widget Studio: a dedicated visual editor. No existing widget settings are modified. */
public final class StudioEditorActivity extends Activity {
 private static final int BACK=0xff171025,GOLD=0xffffd783;
 private static final String[] KEYS={"builders","next","lab","pets","helpers","finish"};
 private final ArrayList<String> accounts=new ArrayList<>(),accountOrder=new ArrayList<>(),fieldOrder=new ArrayList<>();
 private final LinkedHashMap<String,Boolean> selected=new LinkedHashMap<>(),enabled=new LinkedHashMap<>();
 private SharedPreferences prefs;
 private int id=AppWidgetManager.INVALID_APPWIDGET_ID;
 private boolean en;
 private LinearLayout body,accountsBox,fieldsBox,previewBox;
 private CheckBox allAccounts,labels,shortNames;
 private Spinner columns,sort;
 private SeekBar fontSize,spacing;
 private TextView fontLabel,spaceLabel;
 private EditText bg,card,textColor,accent;
 private String tr(String nl,String english){return en?english:nl;}
 private int dp(int d){return Math.round(d*getResources().getDisplayMetrics().density);}
 private String labelField(String key){
  if(key.equals("builders"))return tr("Bouwers gereed / actief","Builders ready / active");
  if(key.equals("next"))return tr("Volgende builder","Next builder");
  if(key.equals("lab"))return "Laboratory";
  if(key.equals("pets"))return "Pets";
  if(key.equals("helpers"))return "Helpers";
  return tr("Eindtijd","Finish time");
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);setResult(RESULT_CANCELED);en=I18n.english(this);
  id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(id==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
  prefs=StudioWidget.preferences(this);
  AppData.migrate(this);
  try{
   JSONArray a=new JSONArray(AppData.prefs(this).getString("accounts","[]"));
   for(int i=0;i<a.length();i++){String name=a.optString(i);if(!name.isEmpty()&&!accounts.contains(name))accounts.add(name);}
  }catch(Exception ignored){}
  accountOrder.addAll(accounts);
  try{
   JSONArray order=new JSONArray(prefs.getString("order_"+id,"[]"));
   ArrayList<String> ordered=new ArrayList<>();
   for(int i=0;i<order.length();i++)if(accountOrder.contains(order.optString(i))&&!ordered.contains(order.optString(i)))ordered.add(order.optString(i));
   for(String a:accountOrder)if(!ordered.contains(a))ordered.add(a);
   accountOrder.clear();accountOrder.addAll(ordered);
  }catch(Exception ignored){}
  HashSet<String> picked=new HashSet<>();
  try{
   JSONArray a=new JSONArray(prefs.getString("selected_"+id,"[]"));
   for(int i=0;i<a.length();i++)picked.add(a.optString(i));
  }catch(Exception ignored){}
  for(String a:accountOrder)selected.put(a,picked.contains(a));
  ArrayList<String> prior=StudioWidget.fieldOrder(prefs,id);
  fieldOrder.addAll(prior);
  for(String f:KEYS){if(!fieldOrder.contains(f))fieldOrder.add(f);enabled.put(f,prior.contains(f));}
  getWindow().setStatusBarColor(BACK);getWindow().setNavigationBarColor(BACK);
  ScrollView scroll=new ScrollView(this);
  body=new LinearLayout(this);body.setOrientation(1);body.setBackgroundColor(BACK);
  body.setPadding(dp(16),dp(20),dp(16),dp(22));
  scroll.addView(body);setContentView(scroll);
  title("WIDGET STUDIO",22);
  note(tr("Ontwerp deze widget zelf. Alle keuzes gelden alleen voor deze widget.",
   "Design this widget yourself. Choices only affect this widget."));
  section(tr("Voorbeeldweergave","Live preview"));
  previewBox=new LinearLayout(this);previewBox.setOrientation(1);body.addView(previewBox);
  section(tr("Accounts en volgorde","Accounts and order"));
  allAccounts=check(tr("Alle accounts tonen","Show all accounts"),prefs.getBoolean("all_"+id,true),body);
  allAccounts.setOnCheckedChangeListener((v,on)->{renderAccounts();preview();});
  note(tr("Gebruik de pijlen om accounts te verplaatsen. Iedere widget heeft eigen instellingen.",
   "Use arrows to reorder accounts. Each widget has independent settings."));
  accountsBox=new LinearLayout(this);accountsBox.setOrientation(1);body.addView(accountsBox);
  renderAccounts();
  section(tr("Zichtbare onderdelen en volgorde","Visible fields and order"));
  note(tr("Kies welke informatie verschijnt en verplaats onderdelen met de pijlen.",
   "Choose which details appear and reorder fields with the arrows."));
  fieldsBox=new LinearLayout(this);fieldsBox.setOrientation(1);body.addView(fieldsBox);renderFields();
  section(tr("Indeling","Layout"));
  columns=picker(new String[]{tr("Automatisch","Automatic"),tr("1 kolom","1 column"),tr("2 kolommen","2 columns"),tr("3 kolommen","3 columns")});
  columns.setSelection(Math.max(0,Math.min(3,prefs.getInt("cols_"+id,0))));
  field(tr("Aantal kolommen","Columns"),columns);
  sort=picker(new String[]{tr("Mijn volgorde","My order"),tr("Accountnaam","Account name"),tr("Volgende bouwer","Next builder")});
  sort.setSelection(Math.max(0,Math.min(2,prefs.getInt("sort_"+id,0))));
  field(tr("Sorteervolgorde","Sort order"),sort);
  labels=check(tr("Veldnamen tonen","Show field labels"),prefs.getBoolean("labels_"+id,true),body);
  shortNames=check(tr("Lange accountnamen inkorten","Shorten long account names"),prefs.getBoolean("short_"+id,false),body);
  fontLabel=note("");fontSize=new SeekBar(this);fontSize.setMax(8);
  fontSize.setProgress(Math.max(0,Math.min(8,prefs.getInt("font_"+id,12)-10)));body.addView(fontSize);
  spaceLabel=note("");spacing=new SeekBar(this);spacing.setMax(10);
  spacing.setProgress(Math.max(0,Math.min(10,prefs.getInt("space_"+id,3))));body.addView(spacing);
  section(tr("Kleuren","Colors"));
  note(tr("Gebruik kleuren in het formaat #RRGGBB. Kies een thema of pas iedere kleur apart aan.",
   "Use colors in #RRGGBB format. Choose a theme or customize every color."));
  LinearLayout presets=new LinearLayout(this);presets.setOrientation(LinearLayout.HORIZONTAL);body.addView(presets);
  preset(presets,tr("Paars","Purple"),"#241832","#38284A","#FFFFFF","#FFD783");
  preset(presets,tr("Donker","Dark"),"#131313","#262626","#FFFFFF","#8BE3AD");
  preset(presets,tr("Licht","Light"),"#F1ECF7","#FFFFFF","#241832","#6840A0");
  bg=colorInput(tr("Achtergrond","Background"),"bg_",StudioWidget.hex(StudioWidget.DEF_BG));
  card=colorInput(tr("Kaarten","Cards"),"card_",StudioWidget.hex(StudioWidget.DEF_CARD));
  textColor=colorInput(tr("Tekst","Text"),"text_",StudioWidget.hex(StudioWidget.DEF_TEXT));
  accent=colorInput(tr("Accent / accountnaam","Accent / account name"),"accent_",StudioWidget.hex(StudioWidget.DEF_ACCENT));
  Button update=button(tr("Voorbeeld verversen","Refresh preview"),()->preview());
  Button save=button(tr("✓ Ontwerp opslaan","✓ Save design"),()->save());
  save.setBackgroundTintList(ColorStateList.valueOf(0xff7652a3));
  columns.setOnItemSelectedListener(spinListener());sort.setOnItemSelectedListener(spinListener());
  labels.setOnCheckedChangeListener((v,on)->preview());
  shortNames.setOnCheckedChangeListener((v,on)->preview());
  SeekBar.OnSeekBarChangeListener seek=new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar s,int n,boolean from){
    fontLabel.setText(tr("Tekstgrootte: ","Text size: ")+(10+fontSize.getProgress())+" sp");
    spaceLabel.setText(tr("Rijafstand: ","Spacing: ")+spacing.getProgress()+" dp");
    preview();
   }
   public void onStartTrackingTouch(SeekBar s){}
   public void onStopTrackingTouch(SeekBar s){}
  };
  fontSize.setOnSeekBarChangeListener(seek);spacing.setOnSeekBarChangeListener(seek);
  fontLabel.setText(tr("Tekstgrootte: ","Text size: ")+(10+fontSize.getProgress())+" sp");
  spaceLabel.setText(tr("Rijafstand: ","Spacing: ")+spacing.getProgress()+" dp");
  preview();
 }
 private AdapterView.OnItemSelectedListener spinListener(){
  return new AdapterView.OnItemSelectedListener(){
   public void onItemSelected(AdapterView<?> p,View v,int position,long id){preview();}
   public void onNothingSelected(AdapterView<?> p){}
  };
 }
 private TextView note(String msg){
  TextView v=new TextView(this);v.setText(msg);v.setTextColor(0xffcbb5db);v.setTextSize(13);
  v.setPadding(0,dp(7),0,dp(9));body.addView(v);return v;
 }
 private void title(String msg,int size){
  TextView t=note(msg);t.setTextSize(size);t.setTextColor(GOLD);t.setTypeface(null,1);
 }
 private void section(String msg){title(msg,17);}
 private Button button(String text,Runnable action){
  Button b=new Button(this);b.setText(text);b.setAllCaps(false);
  body.addView(b,new LinearLayout.LayoutParams(-1,dp(49)));
  b.setOnClickListener(v->action.run());return b;
 }
 private CheckBox check(String text,boolean on,LinearLayout into){
  CheckBox b=new CheckBox(this);b.setText(text);b.setTextColor(Color.WHITE);b.setChecked(on);
  into.addView(b);return b;
 }
 private Spinner picker(String[] values){
  Spinner s=new Spinner(this);
  s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,values));return s;
 }
 private void field(String title,Spinner v){
  note(title);body.addView(v,new LinearLayout.LayoutParams(-1,dp(48)));
 }
 private EditText colorInput(String label,String key,String defaultValue){
  note(label);
  EditText e=new EditText(this);e.setText(prefs.getString(key+id,defaultValue));
  e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setInputType(1);
  body.addView(e,new LinearLayout.LayoutParams(-1,dp(45)));
  e.addTextChangedListener(new TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){preview();}
   public void afterTextChanged(Editable e){}
  });
  return e;
 }
 private void preset(LinearLayout row,String label,String background,String cards,String text,String highlight){
  Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(11);
  row.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));
  b.setOnClickListener(v->{bg.setText(background);card.setText(cards);textColor.setText(text);accent.setText(highlight);preview();});
 }
 private void renderAccounts(){
  if(accountsBox==null)return;
  accountsBox.removeAllViews();
  for(int i=0;i<accountOrder.size();i++){
   int pos=i;String key=accountOrder.get(i);
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);accountsBox.addView(row);
   CheckBox box=new CheckBox(this);box.setChecked(allAccounts.isChecked()||selected.getOrDefault(key,false));
   box.setEnabled(!allAccounts.isChecked());box.setText(AppData.displayName(this,key));
   box.setTextColor(Color.WHITE);row.addView(box,new LinearLayout.LayoutParams(0,dp(44),1));
   box.setOnCheckedChangeListener((v,on)->{selected.put(key,on);preview();});
   arrow(row,"↑",i>0,()->{Collections.swap(accountOrder,pos,pos-1);renderAccounts();preview();});
   arrow(row,"↓",i<accountOrder.size()-1,()->{Collections.swap(accountOrder,pos,pos+1);renderAccounts();preview();});
  }
 }
 private void renderFields(){
  fieldsBox.removeAllViews();
  for(int i=0;i<fieldOrder.size();i++){
   final int pos=i;String key=fieldOrder.get(i);
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);fieldsBox.addView(row);
   CheckBox box=new CheckBox(this);box.setText(labelField(key));box.setTextColor(Color.WHITE);
   box.setChecked(enabled.getOrDefault(key,false));row.addView(box,new LinearLayout.LayoutParams(0,dp(44),1));
   box.setOnCheckedChangeListener((v,on)->{enabled.put(key,on);preview();});
   arrow(row,"↑",i>0,()->{Collections.swap(fieldOrder,pos,pos-1);renderFields();preview();});
   arrow(row,"↓",i<fieldOrder.size()-1,()->{Collections.swap(fieldOrder,pos,pos+1);renderFields();preview();});
  }
 }
 private void arrow(LinearLayout row,String text,boolean active,Runnable action){
  Button b=new Button(this);b.setText(text);b.setTextSize(15);b.setAllCaps(false);b.setEnabled(active);
  row.addView(b,new LinearLayout.LayoutParams(dp(50),dp(45)));b.setOnClickListener(v->action.run());
 }
 private int color(EditText v,int fallback){
  if(v==null)return fallback;
  String text=v.getText().toString().trim();
  return StudioWidget.color(text,fallback);
 }
 private ArrayList<String> chosenFields(){
  ArrayList<String> fields=new ArrayList<>();
  for(String key:fieldOrder)if(enabled.getOrDefault(key,false))fields.add(key);
  return fields;
 }
 private ArrayList<StudioWidget.Status> previewAccounts(){
  ArrayList<StudioWidget.Status> out=new ArrayList<>();
  HashMap<String,StudioWidget.Status> lookup=new HashMap<>();
  for(StudioWidget.Status a:StudioWidget.read(this))lookup.put(a.key,a);
  for(String name:accountOrder)if((allAccounts.isChecked()||selected.getOrDefault(name,false))&&lookup.containsKey(name))out.add(lookup.get(name));
  if(sort.getSelectedItemPosition()==1)Collections.sort(out,(a,b)->a.name.compareToIgnoreCase(b.name));
  if(sort.getSelectedItemPosition()==2)Collections.sort(out,(a,b)->Long.compare(a.builder,b.builder));
  return out;
 }
 private void preview(){
  if(previewBox==null||allAccounts==null||columns==null||sort==null||
   fontSize==null||spacing==null||bg==null||card==null||textColor==null||accent==null)return;
  previewBox.removeAllViews();
  previewBox.setBackgroundColor(color(bg,StudioWidget.DEF_BG));
  int colorCard=color(card,StudioWidget.DEF_CARD),ink=color(textColor,StudioWidget.DEF_TEXT);
  int highlight=color(accent,StudioWidget.DEF_ACCENT),font=10+fontSize.getProgress();
  int gap=spacing.getProgress();
  ArrayList<String> fields=chosenFields();
  ArrayList<StudioWidget.Status> accounts=previewAccounts();
  int cols=columns.getSelectedItemPosition();
  if(cols==0)cols=2;cols=Math.max(1,cols);
  LinearLayout grid=new LinearLayout(this);grid.setOrientation(0);previewBox.addView(grid);
  LinearLayout[] containers=new LinearLayout[cols];
  for(int i=0;i<cols;i++){
   containers[i]=new LinearLayout(this);containers[i].setOrientation(1);
   grid.addView(containers[i],new LinearLayout.LayoutParams(0,-2,1));
  }
  long now=System.currentTimeMillis();
  int limit=Math.min(7,accounts.size());
  for(int i=0;i<limit;i++){
   StudioWidget.Status a=accounts.get(i);
   LinearLayout tile=new LinearLayout(this);tile.setOrientation(1);
   tile.setBackgroundColor(colorCard);tile.setPadding(dp(5),dp(4+gap),dp(5),dp(4+gap));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(2),dp(2),dp(2),dp(2));
   containers[i%cols].addView(tile,lp);
   TextView title=new TextView(this);title.setTextColor(highlight);title.setTextSize(font+1);
   title.setTypeface(null,1);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
   String name=a.name;if(shortNames.isChecked()&&name.length()>13)name=name.substring(0,12)+"…";
   title.setText(name);tile.addView(title);
   for(String key:fields){
    TextView line=new TextView(this);line.setTextColor(ink);line.setTextSize(font);line.setSingleLine(true);
    line.setEllipsize(android.text.TextUtils.TruncateAt.END);
    line.setText((labels.isChecked()?StudioWidget.fieldLabel(this,key)+": ":"")+
     StudioWidget.fieldValue(this,a,key,now));tile.addView(line);
   }
  }
  if(accounts.isEmpty()){
   TextView empty=new TextView(this);empty.setText(tr("Geen accounts geselecteerd","No accounts selected"));
   empty.setTextColor(ink);previewBox.addView(empty);
  }
  TextView caption=new TextView(this);caption.setText(tr("Voorbeeld (max. 7 accounts) · exacte Samsung-grootte kan afwijken",
   "Preview (up to 7 accounts) · exact Samsung widget size may differ"));
  caption.setTextColor(ink);caption.setTextSize(10);caption.setPadding(dp(5),dp(8),dp(5),dp(8));
  previewBox.addView(caption);
 }
 private String validColor(EditText input){
  String value=input.getText().toString().trim();
  if(!value.matches("#[0-9A-Fa-f]{6}"))throw new IllegalArgumentException(
   tr("Kleur moet #RRGGBB zijn: ","Color must be #RRGGBB: ")+value);
  return value.toUpperCase(Locale.ROOT);
 }
 private void save(){
  try{
   ArrayList<String> fields=chosenFields();
   if(fields.isEmpty())throw new IllegalArgumentException(tr("Kies minstens één onderdeel","Choose at least one field"));
   JSONArray selectedKeys=new JSONArray();
   for(String key:accountOrder)if(selected.getOrDefault(key,false))selectedKeys.put(key);
   if(!allAccounts.isChecked()&&selectedKeys.length()==0)throw new IllegalArgumentException(
    tr("Kies minstens één account","Choose at least one account"));
   SharedPreferences.Editor write=prefs.edit();
   write.putBoolean("all_"+id,allAccounts.isChecked());
   write.putString("selected_"+id,selectedKeys.toString());
   write.putString("order_"+id,new JSONArray(accountOrder).toString());
   write.putString("fields_"+id,new JSONArray(fields).toString());
   write.putInt("cols_"+id,columns.getSelectedItemPosition());
   write.putInt("sort_"+id,sort.getSelectedItemPosition());
   write.putInt("font_"+id,10+fontSize.getProgress());
   write.putInt("space_"+id,spacing.getProgress());
   write.putBoolean("labels_"+id,labels.isChecked());
   write.putBoolean("short_"+id,shortNames.isChecked());
   write.putString("bg_"+id,validColor(bg));write.putString("card_"+id,validColor(card));
   write.putString("text_"+id,validColor(textColor));write.putString("accent_"+id,validColor(accent));
   if(!write.commit())throw new Exception("Save failed");
   StudioWidget.updateAll(this);
   setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish();
  }catch(Exception e){new AlertDialog.Builder(this).setTitle(tr("Kan niet opslaan","Cannot save"))
   .setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
 }
}