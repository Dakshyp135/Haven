package thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import util.DatabaseConnection;

/** Checks every minute for today's arrivals and overdue check-outs. */
public class OverdueReminderThread extends Thread {
    private DatabaseConnection database;
    private boolean running = true;

    /** Receives the database connection helper. */
    public OverdueReminderThread(DatabaseConnection database) {
        this.database = database;
        setDaemon(true);
    }

    /** Requests the thread to stop. */
    public void stopReminder() {
        running = false;
        interrupt();
    }

    /** Repeats the reminder query until the application exits. */
    public void run() {
        while (running) {
            printReminders();
            try {
                Thread.sleep(60000);
            } catch (InterruptedException error) {
                running = false;
            }
        }
    }

    /** Prints matching reservations. */
    private void printReminders() {
        String sql = "SELECT reservation_id, reservation_status FROM reservation "
                + "WHERE (reservation_status = 'BOOKED' AND check_in_date = CURDATE()) "
                + "OR (reservation_status = 'CHECKED_IN' AND check_out_date < CURDATE())";
        try {
            Connection connection = database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                System.out.println("[Reminder] Reservation "
                        + results.getInt("reservation_id") + " is "
                        + results.getString("reservation_status"));
            }
            results.close();
            statement.close();
            connection.close();
        } catch (Exception error) {
            System.out.println("Reminder could not check database: " + error.getMessage());
        }
    }
}
