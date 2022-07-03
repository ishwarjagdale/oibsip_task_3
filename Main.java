import java.sql.SQLException;
import atmmachine.ATMMachine;
import java.util.*;

public class Main {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ATMMachine atmMachine = new ATMMachine();
        while(true)
            atmMachine.start();
    }
}
