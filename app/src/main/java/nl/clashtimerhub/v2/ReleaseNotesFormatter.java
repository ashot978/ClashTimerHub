package nl.clashtimerhub.v2;

import java.util.Locale;

/** Keep release notes complete; AlertDialog's message area is scrollable. */
final class ReleaseNotesFormatter {
 private ReleaseNotesFormatter(){}

 static String format(String input,boolean english){
  String fallback=english?"See the full list of changes on GitHub.":
   "Bekijk de volledige wijzigingen op GitHub.";
  if(input==null||input.trim().isEmpty())return fallback;
  String raw=input.replace("\\n","\n");
  String[] lines=raw.split("\n");
  int chosen=english?2:1;
  int section=0;
  boolean bilingual=false;
  StringBuilder result=new StringBuilder();
  for(String original:lines){
   String line=original.trim();
   String heading=line.toLowerCase(Locale.ROOT);
   if(heading.startsWith("### ")){
    if(heading.equals("### nl")||heading.equals("### nederlands")||heading.equals("### dutch")){
     section=1;bilingual=true;
    }else if(heading.equals("### en")||heading.equals("### english")||heading.equals("### engels")){
     section=2;bilingual=true;
    }else section=0;
    continue;
   }
   if(line.startsWith("## ")){
    if(bilingual)break;
    continue;
   }
   if(section!=chosen||!(line.startsWith("- ")||line.startsWith("* ")))continue;
   String content=line.substring(2).replace("**","").trim();
   if(content.isEmpty())continue;
   if(result.length()>0)result.append('\n');
   result.append("• ").append(content);
  }
  return bilingual&&result.length()>0?result.toString():fallback;
 }
}
