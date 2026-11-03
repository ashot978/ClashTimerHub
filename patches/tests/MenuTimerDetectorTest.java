import nl.clashtimerhub.app.MenuTimerDetector;
import nl.clashtimerhub.app.TimerParser;

import java.util.Arrays;
import java.util.List;

/** Fast JVM regression tests for the screen gate, row grouping and exclusions. */
public class MenuTimerDetectorTest {
    private static MenuTimerDetector.Token t(String v, int x, int y) {
        return new MenuTimerDetector.Token(v,x,y,x+85,y+20);
    }
    private static void assertTime(MenuTimerDetector.Result result, long seconds) {
        if(!result.menuOpen || result.hit==null || result.hit.seconds!=seconds)
            throw new AssertionError("Expected "+seconds+", got "+
                (result.hit==null?"none":result.hit.seconds)+", menu="+result.menuOpen);
    }
    public static void main(String[] args) {
        // The village contains arbitrary timers; without the menu header do not scan.
        List<MenuTimerDetector.Token> village=Arrays.asList(
            t("14M",950,170),t("3H 2M",1130,40));
        MenuTimerDetector.Result shut=MenuTimerDetector.detect(
            village,1500,1000,0,y->true);
        if(shut.menuOpen || shut.hit!=null)throw new AssertionError("Village accepted");

        // The active list is visible. OCR separated "5H" from "14M".
        List<MenuTimerDetector.Token> active=Arrays.asList(
            t("Buildings & Heroes",1030,24),
            t("Elixir Collector",950,117), t("5H",1300,117),t("14M",1390,119),
            t("Crafting Station",950,190),t("7H 59M",1300,190),
            t("Suggested Upgrades",1020,390),
            t("Elixir Collector x 5",950,465),t("19H",1340,466));
        assertTime(MenuTimerDetector.detect(active,1500,1000,0,
            y->y<300),5*3600+14*60);

        // Even if OCR misses the divider, recommendations with NO green bar
        // must never replace the real completion time.
        List<MenuTimerDetector.Token> noDivider=Arrays.asList(
            t("Buildings & Heroes",1030,24),
            t("Elixir Collector",950,117), t("5H 14M",1300,117),
            t("New upgrade",950,460), t("1M",1320,460));
        assertTime(MenuTimerDetector.detect(noDivider,1500,1000,0,
            y->y<250),5*3600+14*60);

        // Menu must correspond to the category the user selected.
        if(MenuTimerDetector.detect(active,1500,1000,1,y->true).menuOpen)
            throw new AssertionError("Wrong menu accepted");

        System.out.println("MenuTimerDetector tests passed");
    }
}
