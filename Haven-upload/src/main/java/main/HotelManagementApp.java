package main;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Scanner;
import service.HotelService;
import thread.OverdueReminderThread;
import util.DatabaseConnection;

/** Starts the menu and sends user choices to HotelService. */
public class HotelManagementApp {
    private Scanner keyboard;
    private HotelService hotelService;
    private int staffId;

    /** Creates shared application objects. */
    public HotelManagementApp() throws Exception {
        keyboard = new Scanner(System.in);
        DatabaseConnection database = new DatabaseConnection();
        hotelService = new HotelService(database);
        new File("reports").mkdirs();
        new File("logs").mkdirs();
        OverdueReminderThread reminder = new OverdueReminderThread(database);
        reminder.start();
    }

    /** Displays and processes the menu. */
    public void run() {
        System.out.print("Enter staff ID (sample database uses 3): ");
        staffId = readInteger();
        boolean keepRunning = true;
        while (keepRunning) {
            showMenu();
            int choice = readInteger();
            try {
                if (choice == 1) hotelService.showAvailableRooms();
                else if (choice == 2) registerGuest();
                else if (choice == 3) bookRoom();
                else if (choice == 4) changeStay(true);
                else if (choice == 5) changeStay(false);
                else if (choice == 6) exportReport(true);
                else if (choice == 7) exportReport(false);
                else if (choice == 8) recordPayment();
                else if (choice == 0) keepRunning = false;
                else System.out.println("Please choose a menu number from 0 to 8.");
            } catch (Exception error) {
                System.out.println("Could not complete action: " + error.getMessage());
            }
        }
        keyboard.close();
        System.out.println("Goodbye.");
    }

    /** Prints available actions. */
    private void showMenu() {
        System.out.println("\n--- Hotel Management System ---");
        System.out.println("1. Show available rooms");
        System.out.println("2. Register a guest");
        System.out.println("3. Create a reservation");
        System.out.println("4. Check in");
        System.out.println("5. Check out");
        System.out.println("6. Export reservations CSV");
        System.out.println("7. Export payments CSV");
        System.out.println("8. Record payment");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    /** Reads the guest's details and saves them. */
    private void registerGuest() throws SQLException {
        System.out.print("Name: ");
        String name = keyboard.nextLine();
        System.out.print("Phone: ");
        String phone = keyboard.nextLine();
        System.out.print("Email: ");
        String email = keyboard.nextLine();
        System.out.print("Type (REGULAR or LOYALTY): ");
        String type = keyboard.nextLine().toUpperCase();
        int id = hotelService.addGuest(name, phone, email, type);
        System.out.println("Guest saved. Guest ID: " + id);
    }

    /** Reads reservation details and asks the service to book the room. */
    private void bookRoom() throws SQLException {
        System.out.print("Guest ID: ");
        int guestId = readInteger();
        System.out.print("Room ID: ");
        int roomId = readInteger();
        System.out.print("Check-in date (YYYY-MM-DD): ");
        LocalDate checkIn = LocalDate.parse(keyboard.nextLine());
        System.out.print("Check-out date (YYYY-MM-DD): ");
        LocalDate checkOut = LocalDate.parse(keyboard.nextLine());
        int id = hotelService.bookRoom(guestId, roomId, staffId, checkIn, checkOut);
        System.out.println("Reservation saved. Reservation ID: " + id);
    }

    /** Reads a reservation ID and checks a guest in or out. */
    private void changeStay(boolean checkingIn) throws SQLException {
        System.out.print("Reservation ID: ");
        int reservationId = readInteger();
        hotelService.changeStay(reservationId, checkingIn);
        if (checkingIn) System.out.println("Guest checked in.");
        else System.out.println("Guest checked out.");
    }

    /** Creates either the reservation report or payment report. */
    private void exportReport(boolean reservations) throws Exception {
        hotelService.exportCsv(reservations);
        if (reservations) System.out.println("Saved reports/reservations.csv");
        else System.out.println("Saved reports/payments.csv");
    }

    /** Reads the payment details and saves them. */
    private void recordPayment() throws SQLException {
        System.out.print("Reservation ID: ");
        int reservationId = readInteger();
        System.out.print("Amount: ");
        BigDecimal amount = new BigDecimal(keyboard.nextLine());
        System.out.print("Method (CASH, CARD, BANK_TRANSFER, ONLINE): ");
        String method = keyboard.nextLine().toUpperCase();
        hotelService.addPayment(reservationId, staffId, amount, method);
        System.out.println("Payment saved.");
    }

    /** Reads a whole number from the keyboard. */
    private int readInteger() {
        return Integer.parseInt(keyboard.nextLine());
    }

    /** Starts the application and displays setup errors clearly. */
    public static void main(String[] args) {
        try {
            HotelManagementApp app = new HotelManagementApp();
            app.run();
        } catch (Exception error) {
            System.out.println("Could not start application: " + error.getMessage());
        }
    }
}
