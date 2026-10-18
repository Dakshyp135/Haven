package service;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import util.DatabaseConnection;

/** Contains the main hotel tasks and the SQL used for those tasks. */
public class HotelService {
    private DatabaseConnection database;

    /** Receives the helper that opens MySQL connections. */
    public HotelService(DatabaseConnection database) {
        this.database = database;
    }

    /** Prints available rooms and their room categories. */
    public void showAvailableRooms() throws SQLException {
        String sql = "SELECT room.room_id, room.room_number, room.floor_number, "
                + "room_type.type_name, room_type.standard_rate "
                + "FROM room JOIN room_type "
                + "ON room.room_type_id = room_type.room_type_id "
                + "WHERE room.room_status = 'AVAILABLE' ORDER BY room.room_number";
        Connection connection = database.getConnection();
        Statement statement = connection.createStatement();
        ResultSet results = statement.executeQuery(sql);
        System.out.println("ID | Room | Floor | Type | Rate");
        while (results.next()) {
            System.out.println(results.getInt("room_id") + " | "
                    + results.getString("room_number") + " | "
                    + results.getInt("floor_number") + " | "
                    + results.getString("type_name") + " | "
                    + results.getBigDecimal("standard_rate"));
        }
        results.close();
        statement.close();
        connection.close();
    }

    /** Adds a person and guest row in one transaction. */
    public int addGuest(String name, String phone, String email, String type)
            throws SQLException {
        Connection connection = database.getConnection();
        connection.setAutoCommit(false);
        try {
            String personSql = "INSERT INTO person(full_name, phone, email) VALUES (?, ?, ?)";
            PreparedStatement personStatement = connection.prepareStatement(
                    personSql, Statement.RETURN_GENERATED_KEYS);
            personStatement.setString(1, name);
            personStatement.setString(2, phone);
            personStatement.setString(3, email);
            personStatement.executeUpdate();
            ResultSet keys = personStatement.getGeneratedKeys();
            keys.next();
            int guestId = keys.getInt(1);
            keys.close();
            personStatement.close();

            String guestSql = "INSERT INTO guest(person_id, guest_type) VALUES (?, ?)";
            PreparedStatement guestStatement = connection.prepareStatement(guestSql);
            guestStatement.setInt(1, guestId);
            guestStatement.setString(2, type);
            guestStatement.executeUpdate();
            guestStatement.close();
            connection.commit();
            connection.close();
            return guestId;
        } catch (SQLException error) {
            connection.rollback();
            connection.close();
            throw error;
        }
    }

    /** Checks room dates and saves a reservation in one transaction. */
    public int bookRoom(int guestId, int roomId, int staffId,
                        LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }
        Connection connection = database.getConnection();
        connection.setAutoCommit(false);
        try {
            lockRoom(connection, roomId);
            checkRoomDates(connection, roomId, checkIn, checkOut);
            int reservationId = insertReservation(
                    connection, guestId, roomId, staffId, checkIn, checkOut);
            connection.commit();
            connection.close();
            return reservationId;
        } catch (SQLException error) {
            connection.rollback();
            connection.close();
            throw error;
        }
    }

    /** Locks the room row so two bookings do not check it at the same time. */
    private void lockRoom(Connection connection, int roomId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT room_id FROM room WHERE room_id = ? FOR UPDATE");
        statement.setInt(1, roomId);
        ResultSet results = statement.executeQuery();
        if (!results.next()) {
            results.close();
            statement.close();
            throw new SQLException("Room ID not found.");
        }
        results.close();
        statement.close();
    }

    /** Stops the booking when another active reservation overlaps its dates. */
    private void checkRoomDates(Connection connection, int roomId,
                                LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        String sql = "SELECT reservation_id FROM reservation WHERE room_id = ? "
                + "AND reservation_status IN ('BOOKED', 'CHECKED_IN') "
                + "AND ? < check_out_date AND ? > check_in_date";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, roomId);
        statement.setDate(2, Date.valueOf(checkIn));
        statement.setDate(3, Date.valueOf(checkOut));
        ResultSet results = statement.executeQuery();
        if (results.next()) {
            results.close();
            statement.close();
            throw new SQLException("That room is already booked for those dates.");
        }
        results.close();
        statement.close();
    }

    /** Inserts a reservation and its empty stay row, then returns its ID. */
    private int insertReservation(Connection connection, int guestId, int roomId,
                                  int staffId, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        String sql = "INSERT INTO reservation(guest_id, room_id, staff_id, "
                + "check_in_date, check_out_date) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, guestId);
        statement.setInt(2, roomId);
        statement.setInt(3, staffId);
        statement.setDate(4, Date.valueOf(checkIn));
        statement.setDate(5, Date.valueOf(checkOut));
        statement.executeUpdate();
        ResultSet keys = statement.getGeneratedKeys();
        keys.next();
        int id = keys.getInt(1);
        keys.close();
        statement.close();

        PreparedStatement stay = connection.prepareStatement(
                "INSERT INTO stay(reservation_id) VALUES (?)");
        stay.setInt(1, id);
        stay.executeUpdate();
        stay.close();
        return id;
    }

    /** Checks a guest in or out and updates the linked room and reservation. */
    public void changeStay(int reservationId, boolean checkIn) throws SQLException {
        Connection connection = database.getConnection();
        connection.setAutoCommit(false);
        try {
            int roomId = getAndCheckReservation(connection, reservationId, checkIn);
            updateStayTime(connection, reservationId, checkIn);
            updateReservationStatus(connection, reservationId, checkIn);
            updateRoomStatus(connection, roomId, checkIn);
            connection.commit();
            connection.close();
        } catch (SQLException error) {
            connection.rollback();
            connection.close();
            throw error;
        }
    }

    /** Checks the reservation status and returns its room ID. */
    private int getAndCheckReservation(Connection connection, int id,
                                       boolean checkIn) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT room_id, reservation_status FROM reservation "
                        + "WHERE reservation_id = ? FOR UPDATE");
        statement.setInt(1, id);
        ResultSet results = statement.executeQuery();
        if (!results.next()) throw new SQLException("Reservation not found.");
        String expected = checkIn ? "BOOKED" : "CHECKED_IN";
        if (!expected.equals(results.getString("reservation_status"))) {
            throw new SQLException("Reservation status must be " + expected + ".");
        }
        int roomId = results.getInt("room_id");
        results.close();
        statement.close();
        return roomId;
    }

    /** Saves the actual check-in or check-out time. */
    private void updateStayTime(Connection connection, int id, boolean checkIn)
            throws SQLException {
        String column = checkIn ? "actual_check_in" : "actual_check_out";
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE stay SET " + column + " = CURRENT_TIMESTAMP WHERE reservation_id = ?");
        statement.setInt(1, id);
        statement.executeUpdate();
        statement.close();
    }

    /** Changes the reservation to CHECKED_IN or COMPLETED. */
    private void updateReservationStatus(Connection connection, int id,
                                         boolean checkIn) throws SQLException {
        String status = checkIn ? "CHECKED_IN" : "COMPLETED";
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE reservation SET reservation_status = ? WHERE reservation_id = ?");
        statement.setString(1, status);
        statement.setInt(2, id);
        statement.executeUpdate();
        statement.close();
    }

    /** Marks a room occupied at check-in and available at check-out. */
    private void updateRoomStatus(Connection connection, int roomId,
                                  boolean checkIn) throws SQLException {
        String status = checkIn ? "OCCUPIED" : "AVAILABLE";
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE room SET room_status = ? WHERE room_id = ?");
        statement.setString(1, status);
        statement.setInt(2, roomId);
        statement.executeUpdate();
        statement.close();
    }

    /** Records a successful payment. */
    public void addPayment(int reservationId, int staffId,
                           BigDecimal amount, String method) throws SQLException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment must be greater than zero.");
        }
        String sql = "INSERT INTO payment(reservation_id, staff_id, amount, payment_method) "
                + "VALUES (?, ?, ?, ?)";
        Connection connection = database.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, reservationId);
        statement.setInt(2, staffId);
        statement.setBigDecimal(3, amount);
        statement.setString(4, method);
        statement.executeUpdate();
        statement.close();
        connection.close();
    }

    /** Saves reservations or payments as a comma-separated file. */
    public void exportCsv(boolean reservations) throws SQLException, IOException {
        String fileName;
        String sql;
        if (reservations) {
            fileName = "reports/reservations.csv";
            sql = "SELECT reservation_id, guest_id, room_id, check_in_date, "
                    + "check_out_date, reservation_status FROM reservation";
        } else {
            fileName = "reports/payments.csv";
            sql = "SELECT payment_id, reservation_id, amount, payment_method, "
                    + "payment_time, payment_status FROM payment";
        }
        Connection connection = database.getConnection();
        Statement statement = connection.createStatement();
        ResultSet results = statement.executeQuery(sql);
        FileWriter writer = new FileWriter(fileName);
        int columnCount = results.getMetaData().getColumnCount();
        writeCsvHeader(writer, results, columnCount);
        while (results.next()) writeCsvRow(writer, results, columnCount);
        writer.close();
        results.close();
        statement.close();
        connection.close();
    }

    /** Writes the selected column names to the CSV file. */
    private void writeCsvHeader(FileWriter writer, ResultSet rows, int count)
            throws SQLException, IOException {
        for (int i = 1; i <= count; i++) {
            if (i > 1) writer.write(",");
            writer.write(rows.getMetaData().getColumnLabel(i));
        }
        writer.write(System.lineSeparator());
    }

    /** Writes one database row to the CSV file. */
    private void writeCsvRow(FileWriter writer, ResultSet rows, int count)
            throws SQLException, IOException {
        for (int i = 1; i <= count; i++) {
            if (i > 1) writer.write(",");
            String value = rows.getString(i);
            if (value != null) writer.write(value.replace(",", " "));
        }
        writer.write(System.lineSeparator());
    }
}
