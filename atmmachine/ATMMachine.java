package atmmachine;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;

public class ATMMachine {

    private final Connection connection;
    private final Scanner scanner = new Scanner(System.in);
    private BigInteger account_id;
    private Integer pin;

    public ATMMachine() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        this.connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres",
                "userM", "pass123");
        account_id = null;
        pin = null;
    }

    private boolean authenticate(BigInteger userId, int pin) throws SQLException {
        Statement statement = this.connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT pin FROM users WHERE account_id =" + userId);
        if(resultSet.next())
            return pin == resultSet.getInt("pin");
        else {
            return false;
        }
    }

    public void start() throws SQLException, InputMismatchException {
        this.clearScreen();
        boolean isAuthenticate = false;
        do {
            System.out.print("Enter User ID: ");
            BigInteger userId = getBigInput();

            System.out.print("Enter PIN Code: ");
            int pin = this.getInput();

            isAuthenticate = this.authenticate(userId, pin);
            if(isAuthenticate) {
                System.out.println("Authenticated\n");
                this.setAccount_id(userId);
                this.setPin(pin);
            } else {
                System.out.println("Invalid credentials");
            }
        } while(!isAuthenticate);

        while(true) {
            this.clearScreen();
            int option = menu();

            switch (option) {
                case 1 -> this.getTransactions();
                case 2 -> this.withdraw();
                case 3 -> this.deposit();
                case 4 -> this.transfer();
                case 5 -> {
                    System.out.println("|----------------------------------------------------------------------------");
                    System.out.println("\t Closing session!");
                    System.out.println("|----------------------------------------------------------------------------");
                    this.setAccount_id(null);
                    this.setPin(null);
                    return;
                }
                default -> {
                    System.out.println("Invalid option");
                }
            }
        }

    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private BigInteger getBigInput() {
        BigInteger value;
        while(true) {
            try {
                value = this.scanner.nextBigInteger();
                break;
            } catch(InputMismatchException e) {
                this.scanner.next();
                System.out.print("Enter a valid number!: ");
            }
        }
        return value;
    }

    private BigDecimal getBDecInput() {
        BigDecimal value;
        while(true) {
            try {
                value = this.scanner.nextBigDecimal();
                break;
            } catch(InputMismatchException e) {
                this.scanner.next();
                System.out.print("Enter a valid number!: ");
            }
        }
        return value;
    }

    private int getInput() {
        int value;
        while(true) {
            try {
                value = this.scanner.nextInt();
                break;
            } catch(InputMismatchException e) {
                this.scanner.next();
                System.out.print("Enter a valid number!: ");
            }
        }
        return value;
    }

    private int menu() {
        System.out.println("|----------------------------------------------------------------------------");
        System.out.println("\tATM Machine");
        System.out.println("|----------------------------------------------------------------------------");
        List<String> menuOptions = new ArrayList<>(Arrays.asList("Transaction History", "Withdraw", "Deposit", "Transfer", "Quit"));
        for(String option: menuOptions) {
            System.out.printf("%d. %s\n", menuOptions.indexOf(option) + 1, option);
        }

        System.out.println("|----------------------------------------------------------------------------");
        System.out.print("Select an option: ");

        while(true) {
            try {
                int choice = this.getInput();
                if(choice >= 1 && choice <= menuOptions.size()) {
                    return choice;
                } else {
                    System.out.printf("Enter a value between %d and %d: ", 1, menuOptions.size());
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid option, enter a number!");
            }
        }
    }

    private void getTransactions() throws SQLException {
        Statement statement = this.connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM transactions WHERE account_id = " + this.getAccount_id() + " ORDER BY id DESC;");
        System.out.println("|----------------------------------------------------------------------------");
        System.out.println("\tTransaction History: [Enter q to quit | Press return key to move further]");
        System.out.println("|----------------------------------------------------------------------------");
        System.out.println("Transaction Date\tValue Date\tTransaction Amount\t\tBalance Amount");
        if(!resultSet.next()){
            System.out.println("No Transactions!");
        } else {
            System.out.printf("%16s\t%10s\t%18.2f\t\t%14.2f", resultSet.getString("transaction_date"), resultSet.getString("value_date"), resultSet.getBigDecimal("transaction_amount"), resultSet.getBigDecimal("balance_amount"));
        }
        this.scanner.nextLine();
        while(resultSet.next() && this.scanner.nextLine().equals("")) {
            System.out.printf("%16s\t%10s\t%18.2f\t\t%14.2f", resultSet.getString("transaction_date"), resultSet.getString("value_date"), resultSet.getBigDecimal("transaction_amount"), resultSet.getBigDecimal("balance_amount"));
        }
        System.out.println("\n");
        statement.close();
        this.scanner.nextLine();
    }

    private BigDecimal getBalance() throws SQLException {
        Statement statement = this.connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT balance_amount FROM transactions WHERE account_id = " + this.getAccount_id() + " AND id = (SELECT max(id) FROM transactions WHERE account_id = " + this.getAccount_id() + ")");
        return resultSet.next() ? resultSet.getBigDecimal("balance_amount") : BigDecimal.valueOf(0);
    }

    private BigDecimal getBalance(BigInteger account_id) throws SQLException {
        Statement statement = this.connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT balance_amount FROM transactions WHERE account_id = " + account_id + " AND id = (SELECT max(id) FROM transactions WHERE account_id = " + account_id + ")");
        return resultSet.next() ? resultSet.getBigDecimal("balance_amount") : BigDecimal.valueOf(0);
    }

    private void withdraw() throws SQLException {
        System.out.print("Enter amount to withdraw: ");
        BigDecimal amount = this.getBDecInput();
        Statement statement = this.connection.createStatement();
        BigDecimal balance_amount = this.getBalance();
        if(balance_amount.compareTo(amount) > 0) {
            BigDecimal remaining_amount = balance_amount.subtract(amount);
            int isSuccessful = statement.executeUpdate("INSERT INTO transactions (account_id, transaction_date, value_date, transaction_amount, balance_amount)" +
                    "VALUES (" + this.getAccount_id() + ", " + "NOW(), NOW(), -" + amount + ", " + remaining_amount + ");"
            );
            if(isSuccessful > 0) {
                System.out.println("Amount " + amount + " withdrew successfully!\n");
            }
        } else {
            System.out.println("Your balance amount is not enough!\n");
        }
        statement.close();
        this.scanner.nextLine();
        this.scanner.nextLine();
    }

    private void deposit() throws SQLException {
        System.out.print("Enter amount to deposit: ");
        BigDecimal amount = this.getBDecInput();
        Statement statement = this.connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT balance_amount FROM transactions WHERE account_id = " + this.getAccount_id() + " AND id = (SELECT max(id) FROM transactions WHERE account_id = " + this.getAccount_id() + ");");
        BigDecimal balance_amount = resultSet.next() ? resultSet.getBigDecimal("balance_amount") : BigDecimal.valueOf(0);
        balance_amount = balance_amount.add(amount);
        int isSuccessful = statement.executeUpdate("INSERT INTO transactions (account_id, transaction_date, value_date, transaction_amount, balance_amount)" +
                "VALUES (" + this.getAccount_id() + ", " + "NOW(), NOW(), +" + amount + ", " + balance_amount + ");"
        );
        if(isSuccessful > 0) {
            System.out.println("Amount deposited successfully!\n");
        }
        statement.close();
        this.scanner.nextLine();
        this.scanner.nextLine();
    }

    private void transfer() throws SQLException {
        System.out.print("Enter recipient's account number: ");
        BigInteger account_id = this.getBigInput();
        Statement statement = this.connection.createStatement();
        ResultSet recipient = statement.executeQuery("SELECT * FROM users WHERE account_id = " + account_id);
        if(recipient.next()) {
            System.out.print("Enter amount to transfer: ");
            BigDecimal amount = this.getBDecInput();
            BigDecimal balance_amount = this.getBalance();
            if(balance_amount.compareTo(amount) > 0) {
                int isSuccessful = statement.executeUpdate("INSERT INTO transactions (account_id, transaction_date, value_date, transaction_amount, balance_amount) VALUES " +
                        "(" + this.getAccount_id() + ", NOW(), NOW(), -" + amount + ", " + balance_amount.subtract(amount) + ")");
                if(isSuccessful > 0)  {
                    isSuccessful = statement.executeUpdate("INSERT INTO transactions (account_id, transaction_date, value_date, transaction_amount, balance_amount) VALUES " +
                            "(" + account_id + ", NOW(), NOW(), +" + amount + ", " + this.getBalance(account_id).add(amount) + ")");
                    if(isSuccessful > 0) {
                        System.out.println("Transfer Successful\n");
                    }
                }
            } else {
                System.out.println("You don't have enough balance!\n");
            }
        } else {
            System.out.println("This account doesn't exist!\n");
        }
        statement.close();
        this.scanner.nextLine();
        this.scanner.nextLine();
    }

    public BigInteger getAccount_id() {
        return account_id;
    }

    public void setAccount_id(BigInteger account_id) {
        this.account_id = account_id;
    }

    public Integer getPin() {
        return pin;
    }

    public void setPin(Integer pin) {
        this.pin = pin;
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ATMMachine atmMachine = new ATMMachine();
        while(true)
            atmMachine.start();
    }
}
