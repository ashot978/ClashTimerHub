package nl.clashtimerhub.v2;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public class SyncExtrasTest {
 @Test public void equipmentAndCollectionsAreReadWithCounts()throws Exception{
  JSONObject source=new JSONObject("{\"equipment\":[{\"data\":90000010,\"lvl\":13}],"
   +"\"decos\":[{\"data\":18000250,\"cnt\":2},{\"data\":18000250,\"cnt\":1}],"
   +"\"obstacles\":[{\"data\":8000139}],\"skins\":[52000381],\"sceneries\":[]}");
  JSONObject got=SyncExtras.normalize(null,source,1763400000000L);
  assertEquals(13,got.getJSONArray("equipment").getJSONObject(0).getInt("lvl"));
  assertEquals(3,got.getJSONArray("decos").getJSONObject(0).getInt("cnt"));
  assertEquals(8000139L,got.getJSONArray("obstacles").getJSONObject(0).getLong("data"));
  assertEquals(52000381L,got.getJSONArray("skins").getJSONObject(0).getLong("data"));
  assertEquals(0,got.getJSONArray("sceneries").length());
 }
 @Test public void missingSectionsDoNotEraseCollections()throws Exception{
  JSONObject old=new JSONObject("{\"decos\":[{\"data\":18000250,\"cnt\":1}],\"equipment\":[{\"data\":90000008,\"lvl\":9}]}");
  JSONObject got=SyncExtras.normalize(old,new JSONObject("{\"equipment\":[]}"),1763400000000L);
  assertEquals(1,got.getJSONArray("decos").length());
  assertEquals(0,got.getJSONArray("equipment").length());
 }
 @Test public void unsupportedShapeKeepsEarlierItems()throws Exception{
  JSONObject old=new JSONObject("{\"skins\":[{\"data\":52000001,\"cnt\":1}]}");
  JSONObject got=SyncExtras.normalize(old,new JSONObject("{\"skins\":[\"unknown\"]}"),1234L);
  assertEquals(1,got.getJSONArray("skins").length());
  assertTrue(got.optBoolean("skins_unreadable"));
 }
 @Test public void malformedSectionIsRejected()throws Exception{
  try{SyncExtras.normalize(null,new JSONObject("{\"decos\":{}}"),1234L);fail();}
  catch(org.json.JSONException expected){assertTrue(expected.getMessage().contains("decos"));}
 }
 @Test public void costsStopAtCurrentMax()throws Exception{
  JSONObject item=new JSONObject("{\"max\":3,\"ore\":[[120,0,0],[240,20,0],[0,0,0]]}");
  assertEquals("120 Shiny · 0 Glowy · 0 Starry",SyncCatalog.oreText(item,1,false));
  assertEquals("360 Shiny · 20 Glowy · 0 Starry",SyncCatalog.oreText(item,1,true));
  assertEquals("",SyncCatalog.oreText(item,3,true));
 }
}
