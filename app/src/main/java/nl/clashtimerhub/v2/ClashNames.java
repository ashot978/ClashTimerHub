package nl.clashtimerhub.v2;

import java.util.HashMap;
import java.util.Map;

/** Names ported from the supplied iOS Scriptable reference. */
final class ClashNames {
 private static final Map<Long,String> NAMES=new HashMap<>();
 static {
  NAMES.put(1000000L, "Army Camp");
  NAMES.put(1000001L, "Town Hall");
  NAMES.put(1000002L, "Elixir Collector");
  NAMES.put(1000003L, "Elixir Storage");
  NAMES.put(1000004L, "Gold Mine");
  NAMES.put(1000005L, "Gold Storage");
  NAMES.put(1000006L, "Barracks");
  NAMES.put(1000007L, "Laboratory");
  NAMES.put(1000008L, "Cannon");
  NAMES.put(1000009L, "Archer Tower");
  NAMES.put(1000010L, "Wall");
  NAMES.put(1000011L, "Wizard Tower");
  NAMES.put(1000012L, "Air Defense");
  NAMES.put(1000013L, "Mortar");
  NAMES.put(1000014L, "Clan Castle");
  NAMES.put(1000015L, "Builder's Hut");
  NAMES.put(1000019L, "Hidden Tesla");
  NAMES.put(1000020L, "Spell Factory");
  NAMES.put(1000021L, "X-Bow");
  NAMES.put(1000023L, "Dark Elixir Drill");
  NAMES.put(1000024L, "Dark Elixir Storage");
  NAMES.put(1000026L, "Dark Barracks");
  NAMES.put(1000027L, "Inferno Tower");
  NAMES.put(1000028L, "Air Sweeper");
  NAMES.put(1000029L, "Dark Spell Factory");
  NAMES.put(1000031L, "Eagle Artillery");
  NAMES.put(1000032L, "Bomb Tower");
  NAMES.put(1000059L, "Workshop");
  NAMES.put(1000067L, "Scattershot");
  NAMES.put(1000068L, "Pet House");
  NAMES.put(1000070L, "Blacksmith");
  NAMES.put(1000071L, "Hero Hall");
  NAMES.put(1000072L, "Spell Tower");
  NAMES.put(1000077L, "Monolith");
  NAMES.put(1000084L, "Multi-Archer Tower");
  NAMES.put(1000085L, "Ricochet Cannon");
  NAMES.put(1000079L, "Multi-Gear Tower");
  NAMES.put(1000086L, "Revenge Tower");
  NAMES.put(107000008L, "Logger");
  NAMES.put(1000089L, "Firespitter");
  NAMES.put(1000093L, "Helper Hut");
  NAMES.put(1000097L, "Crafting Station");
  NAMES.put(1000102L, "Super Wizard Tower");
  NAMES.put(12000000L, "Bomb");
  NAMES.put(12000001L, "Spring Trap");
  NAMES.put(12000002L, "Giant Bomb");
  NAMES.put(12000005L, "Air Bomb");
  NAMES.put(12000006L, "Seeking Air Mine");
  NAMES.put(12000008L, "Skeleton Trap");
  NAMES.put(12000016L, "Tornado Trap");
  NAMES.put(12000020L, "Giga Bomb");
  NAMES.put(28000000L, "Barbarian King");
  NAMES.put(28000001L, "Archer Queen");
  NAMES.put(28000002L, "Grand Warden");
  NAMES.put(28000004L, "Royal Champion");
  NAMES.put(28000006L, "Minion Prince");
  NAMES.put(73000000L, "L.A.S.S.I");
  NAMES.put(73000001L, "Mighty Yak");
  NAMES.put(73000002L, "Electro Owl");
  NAMES.put(73000003L, "Unicorn");
  NAMES.put(73000004L, "Phoenix");
  NAMES.put(73000007L, "Poison Lizard");
  NAMES.put(73000008L, "Diggy");
  NAMES.put(73000009L, "Frosty");
  NAMES.put(73000010L, "Spirit Fox");
  NAMES.put(73000011L, "Angry Jelly");
  NAMES.put(73000016L, "Sneezy");
  NAMES.put(73000017L, "Greedy Raven");
  // Seasonal defences and their individual upgrade modules (2026 exports).
  NAMES.put(103000008L, "Roaster");
  NAMES.put(103000009L, "Air Bombs");
  NAMES.put(103000010L, "Lava Launcher");
  NAMES.put(103000011L, "Hot Candle");
  NAMES.put(103000012L, "Hero Hunter");
  NAMES.put(103000013L, "Cake-A-Pult");
  NAMES.put(103000033L, "Hot Candle - Hitpoints");
  NAMES.put(102000034L, "Hot Candle - DPS");
  NAMES.put(102000035L, "Hot Candle - TSA");
  NAMES.put(102000036L, "Hero Hunter - Hitpoints");
  NAMES.put(102000037L, "Hero Hunter - DPS");
  NAMES.put(102000038L, "Hero Hunter - PSL");
  NAMES.put(102000039L, "Cake-A-Pult - Hitpoints");
  NAMES.put(102000040L, "Cake-A-Pult - DPH");
  NAMES.put(102000041L, "Cake-A-Pult - ED");
  NAMES.put(4000000L, "Barbarian");
  NAMES.put(4000001L, "Archer");
  NAMES.put(4000002L, "Goblin");
  NAMES.put(4000003L, "Giant");
  NAMES.put(4000004L, "Wall Breaker");
  NAMES.put(4000005L, "Balloon");
  NAMES.put(4000006L, "Wizard");
  NAMES.put(4000007L, "Healer");
  NAMES.put(4000008L, "Dragon");
  NAMES.put(4000009L, "P.E.K.K.A");
  NAMES.put(4000010L, "Minion");
  NAMES.put(4000011L, "Hog Rider");
  NAMES.put(4000012L, "Valkyrie");
  NAMES.put(4000013L, "Golem");
  NAMES.put(4000015L, "Witch");
  NAMES.put(4000017L, "Lava Hound");
  NAMES.put(4000022L, "Bowler");
  NAMES.put(4000023L, "Baby Dragon");
  NAMES.put(4000024L, "Miner");
  NAMES.put(4000053L, "Yeti");
  NAMES.put(4000058L, "Ice Golem");
  NAMES.put(4000059L, "Electro Dragon");
  NAMES.put(4000065L, "Dragon Rider");
  NAMES.put(4000082L, "Headhunter");
  NAMES.put(4000083L, "Electro Titan");
  NAMES.put(4000051L, "Wall Wrecker");
  NAMES.put(4000052L, "Battle Blimp");
  NAMES.put(4000062L, "Stone Slammer");
  NAMES.put(4000075L, "Siege Barracks");
  NAMES.put(4000087L, "Log Launcher");
  NAMES.put(4000091L, "Flame Flinger");
  NAMES.put(4000092L, "Battle Drill");
  NAMES.put(26000000L, "Lightning Spell");
  NAMES.put(26000001L, "Healing Spell");
  NAMES.put(26000002L, "Rage Spell");
  NAMES.put(26000003L, "Jump Spell");
  NAMES.put(26000005L, "Freeze Spell");
  NAMES.put(26000009L, "Poison Spell");
  NAMES.put(26000010L, "Earthquake Spell");
  NAMES.put(26000011L, "Haste Spell");
  NAMES.put(26000016L, "Clone Spell");
  NAMES.put(26000017L, "Skeleton Spell");
  NAMES.put(26000028L, "Bat Spell");
  NAMES.put(26000035L, "Invisibility Spell");
  NAMES.put(26000053L, "Recall Spell");
  NAMES.put(26000070L, "Overgrowth Spell");
  NAMES.put(26000109L, "Ice Block Spell");
 }
 static String name(long id){String n=NAMES.get(id);return n==null?"Upgrade #"+id:n;}

 /** Resolve only machine-generated fallback names. User-edited labels are untouched.
  * This also repairs older imported timers immediately, without another export. */
 static String resolvedName(String stored,long sourceId){
  String label=stored==null?"":stored.trim();
  boolean fallback=label.isEmpty()||label.matches("(?i)Upgrade\\s*#\\s*[0-9]+");
  if(!fallback)return label;
  long id=sourceId;
  if(id<0&&!label.isEmpty()){
   try{id=Long.parseLong(label.substring(label.indexOf('#')+1).trim());}
   catch(NumberFormatException ignored){}
  }
  return id<0?(label.isEmpty()?"Upgrade":label):name(id);
 }
 static String displayName(org.json.JSONObject timer){
  return resolvedName(timer.optString("name",""),timer.optLong("sourceId",-1L));
 }
 private ClashNames(){}
}
