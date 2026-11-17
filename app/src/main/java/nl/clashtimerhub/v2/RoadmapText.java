package nl.clashtimerhub.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts the bundled Markdown roadmap into phone-readable sections and cards. */
final class RoadmapText {
 static final int TITLE=1,HEADING=2,SUBHEADING=3,BULLET=4,BODY=5,CARD=6,DIVIDER=7;
 static final class Entry {
  final int kind;
  final String text;
  Entry(int kind,String text){this.kind=kind;this.text=text;}
 }
 private RoadmapText(){}

 static List<Entry> parse(String markdown){
  if(markdown==null||markdown.trim().isEmpty())return Collections.emptyList();
  List<Entry> out=new ArrayList<>();
  for(String line:markdown.replace("\r","").split("\n")){
   String raw=line.trim();
   if(raw.isEmpty())continue;
   if(raw.matches("[-_]{3,}")){out.add(new Entry(DIVIDER,""));continue;}
   if(raw.startsWith("|")){
    String[] bits=raw.split("\\|",-1);
    List<String> cells=new ArrayList<>();
    for(int i=1;i<bits.length;i++){
     String cell=bits[i].trim();
     if(i==bits.length-1&&cell.isEmpty())continue;
     cells.add(clean(cell));
    }
    if(cells.isEmpty())continue;
    boolean separator=true;
    for(String cell:cells)if(!cell.matches(":?-{2,}:?"))separator=false;
    if(separator||cells.get(0).equalsIgnoreCase("Priority")||cells.get(0).equalsIgnoreCase("ID"))continue;
    if(cells.size()==4){
     out.add(new Entry(CARD,cells.get(0)+" · "+cells.get(1)
      +"\n"+cells.get(2)+"\nLet op: "+cells.get(3)));
    }else out.add(new Entry(CARD,String.join(" · ",cells)));
    continue;
   }
   int depth=0;
   while(depth<raw.length()&&raw.charAt(depth)=='#')depth++;
   if(depth>0&&depth<=6&&depth<raw.length()&&raw.charAt(depth)==' '){
    out.add(new Entry(depth==1?TITLE:depth==2?HEADING:SUBHEADING,clean(raw.substring(depth).trim())));
   }else if(raw.startsWith("- ")||raw.startsWith("* ")){
    out.add(new Entry(BULLET,"• "+clean(raw.substring(2))));
   }else if(raw.matches("^[0-9]+\\. .+")){
    out.add(new Entry(BULLET,clean(raw)));
   }else out.add(new Entry(BODY,clean(raw)));
  }
  return out;
 }
 private static String clean(String value){
  return value.replace("**","").replace("__","").replace(String.valueOf((char)96),"")
   .replaceAll("\\[([^\\]]+)\\]\\(([^)]*)\\)","$1").trim();
 }
}
