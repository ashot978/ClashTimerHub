package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExportNameRepairTest {
 @Test public void previousUnknownIdsNowResolveWithRealLabels() {
  assertEquals("Sour Elixir Cauldron", ClashNames.name(1000083L));
  assertEquals("Witch Golem", ClashNames.name(4000102L));
  assertEquals("Broom Witch", ClashNames.name(4000125L));
  assertEquals("Debt Collector", ClashNames.name(4000136L));
  assertEquals("Super Yeti", ClashNames.name(4000147L));
  assertEquals("Meteor Golem", ClashNames.name(4000167L));
  assertEquals("Totem Thrower", ClashNames.name(4000194L));
  assertEquals("Santa's Surprise", ClashNames.name(26000006L));
  assertEquals("Bag of Frostmites", ClashNames.name(26000073L));
  assertEquals("Super Wizard", ClashNames.name(4000083L));
  assertEquals("Firespitter", ClashNames.name(1000089L));
  assertEquals("Crafting Station", ClashNames.name(1000097L));
  assertEquals("Squad Barracks", ClashNames.name(1000106L));
 }
 @Test public void oldImportedFallbacksRepairWithoutAnotherSync() {
  assertEquals("Super Wizard", ClashNames.resolvedName("Upgrade #4000083",-1L));
  assertEquals("Squad Barracks", ClashNames.resolvedName("Upgrade #1000106",-1L));
  assertEquals("Custom building", ClashNames.resolvedName("Custom building",1000106L));
  assertEquals("Upgrade #1000107", ClashNames.resolvedName("Upgrade #1000107",-1L));
 }
 @Test public void allVerifiedNewEntriesResolveBothWays() {
  assertEquals("Sour Elixir Cauldron", ClashNames.resolvedName("Upgrade #1000083", 1000083L));
  assertEquals("Super Barbarian", ClashNames.resolvedName("Upgrade #4000026", 4000026L));
  assertEquals("Super Archer", ClashNames.resolvedName("Upgrade #4000027", 4000027L));
  assertEquals("Super Wall Breaker", ClashNames.resolvedName("Upgrade #4000028", 4000028L));
  assertEquals("Super Giant", ClashNames.resolvedName("Upgrade #4000029", 4000029L));
  assertEquals("Ice Wizard", ClashNames.resolvedName("Upgrade #4000030", 4000030L));
  assertEquals("Battle Ram", ClashNames.resolvedName("Upgrade #4000045", 4000045L));
  assertEquals("Royal Ghost", ClashNames.resolvedName("Upgrade #4000047", 4000047L));
  assertEquals("Pumpkin Barbarian", ClashNames.resolvedName("Upgrade #4000048", 4000048L));
  assertEquals("Giant Skeleton", ClashNames.resolvedName("Upgrade #4000050", 4000050L));
  assertEquals("Sneaky Goblin", ClashNames.resolvedName("Upgrade #4000055", 4000055L));
  assertEquals("Super Miner", ClashNames.resolvedName("Upgrade #4000056", 4000056L));
  assertEquals("Rocket Balloon", ClashNames.resolvedName("Upgrade #4000057", 4000057L));
  assertEquals("Skeleton Barrel", ClashNames.resolvedName("Upgrade #4000061", 4000061L));
  assertEquals("Inferno Dragon", ClashNames.resolvedName("Upgrade #4000063", 4000063L));
  assertEquals("Super Valkyrie", ClashNames.resolvedName("Upgrade #4000064", 4000064L));
  assertEquals("Super Witch", ClashNames.resolvedName("Upgrade #4000066", 4000066L));
  assertEquals("M.E.C.H.A", ClashNames.resolvedName("Upgrade #4000067", 4000067L));
  assertEquals("Party Wizard", ClashNames.resolvedName("Upgrade #4000072", 4000072L));
  assertEquals("Ice Hound", ClashNames.resolvedName("Upgrade #4000076", 4000076L));
  assertEquals("Super Bowler", ClashNames.resolvedName("Upgrade #4000080", 4000080L));
  assertEquals("Super Dragon", ClashNames.resolvedName("Upgrade #4000081", 4000081L));
  assertEquals("Super Minion", ClashNames.resolvedName("Upgrade #4000084", 4000084L));
  assertEquals("Ram Rider", ClashNames.resolvedName("Upgrade #4000094", 4000094L));
  assertEquals("Super Hog Rider", ClashNames.resolvedName("Upgrade #4000098", 4000098L));
  assertEquals("Barcher", ClashNames.resolvedName("Upgrade #4000101", 4000101L));
  assertEquals("Witch Golem", ClashNames.resolvedName("Upgrade #4000102", 4000102L));
  assertEquals("Hog Wizard", ClashNames.resolvedName("Upgrade #4000103", 4000103L));
  assertEquals("Lavaloon", ClashNames.resolvedName("Upgrade #4000104", 4000104L));
  assertEquals("C.O.O.K.I.E", ClashNames.resolvedName("Upgrade #4000118", 4000118L));
  assertEquals("Firecracker", ClashNames.resolvedName("Upgrade #4000119", 4000119L));
  assertEquals("Azure Dragon", ClashNames.resolvedName("Upgrade #4000120", 4000120L));
  assertEquals("Barbarian Kicker", ClashNames.resolvedName("Upgrade #4000121", 4000121L));
  assertEquals("Giant Thrower", ClashNames.resolvedName("Upgrade #4000122", 4000122L));
  assertEquals("Broom Witch", ClashNames.resolvedName("Upgrade #4000125", 4000125L));
  assertEquals("Ice Minion", ClashNames.resolvedName("Upgrade #4000130", 4000130L));
  assertEquals("Debt Collector", ClashNames.resolvedName("Upgrade #4000136", 4000136L));
  assertEquals("Snake Barrel", ClashNames.resolvedName("Upgrade #4000142", 4000142L));
  assertEquals("Super Yeti", ClashNames.resolvedName("Upgrade #4000147", 4000147L));
  assertEquals("Giant Giant", ClashNames.resolvedName("Upgrade #4000156", 4000156L));
  assertEquals("K.A.N.E", ClashNames.resolvedName("Upgrade #4000157", 4000157L));
  assertEquals("The Disarmer", ClashNames.resolvedName("Upgrade #4000158", 4000158L));
  assertEquals("YEETer", ClashNames.resolvedName("Upgrade #4000159", 4000159L));
  assertEquals("Meteor Golem", ClashNames.resolvedName("Upgrade #4000167", 4000167L));
  assertEquals("Elephant Rider", ClashNames.resolvedName("Upgrade #4000185", 4000185L));
  assertEquals("Yeti Undertaker", ClashNames.resolvedName("Upgrade #4000190", 4000190L));
  assertEquals("Totem Thrower", ClashNames.resolvedName("Upgrade #4000194", 4000194L));
  assertEquals("Santa's Surprise", ClashNames.resolvedName("Upgrade #26000006", 26000006L));
  assertEquals("Bag of Frostmites", ClashNames.resolvedName("Upgrade #26000073", 26000073L));
 }
}
