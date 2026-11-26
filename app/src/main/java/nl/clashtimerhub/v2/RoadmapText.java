package nl.clashtimerhub.v2;

/** Select the requested language from the bundled, offline roadmap. */
final class RoadmapText {
 private RoadmapText(){}
 static String localized(String markdown,boolean english){
  if(markdown==null)return "";
  String wanted=english?"## EN":"## NL";
  StringBuilder section=new StringBuilder();
  boolean active=false;
  for(String raw:markdown.split("\\r?\\n")){
   String line=raw.trim();
   if(line.startsWith("## ")){
    if(active)break;
    active=wanted.equals(line);
    continue;
   }
   if(active)section.append(line).append('\n');
  }
  return section.toString().trim();
 }
}
