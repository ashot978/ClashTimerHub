import nl.clashtimerhub.app.BuilderAvailability;

public class BuilderAvailabilityTest {
    private static void expect(String text, int expected) {
        int actual=BuilderAvailability.parse(text);
        if(actual!=expected) throw new AssertionError(text+" -> "+actual+" != "+expected);
    }
    public static void main(String[] args) {
        for(int total=6;total<=7;total++) {
            for(int free=0;free<=total;free++)
                expect(free+"/"+total,free);
        }
        expect("Builder 2/6",2);
        expect("2 / 7",2);
        expect("8/7",-1);
        expect("not recognized",-1);
        expect("1/6 2/6",-1);
        expect("2/6 2/6",2);
        System.out.println("Builder availability tests passed");
    }
}
