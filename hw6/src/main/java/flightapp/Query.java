package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Checksum;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  //
  // Instance variables
  // 
  // Clearing Tables
  private static final String CLEAR_RESERVATIONS_SQL = "DELETE FROM RESERVATIONS_LUDINH";
  private PreparedStatement clearReservationsStmt;
  private static final String CLEAR_USERS_SQL = "DELETE FROM USERS_LUDINH";
  private PreparedStatement clearUsersStmt;

  // Login
  private static final String CHECK_LOGIN_SQL = "SELECT password FROM USERS_LUDINH WHERE username = ?";
  private PreparedStatement checkLoginStmt;

  // Create
  private static final String CREATE_USER_SQL = "INSERT INTO USERS_LUDINH(username, password, balance) VALUES(?, ?, ?)";
  private PreparedStatement createUserStmt;

  // Search
  private static final String SEARCH_SQL1 = 
    "SELECT TOP (?) fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price "
    + "FROM FLIGHTS "
    + "WHERE day_of_month = ? AND origin_city = ? AND dest_city = ? AND canceled = 0 "
    + "ORDER BY actual_time ASC";
  private PreparedStatement searchStmt1;
  private static final String SEARCH_SQL2 = "SELECT * FROM (SELECT TOP ( ? ) * FROM "
  + "(SELECT 1 AS num, F.actual_time AS actual_time, "
  + " F.fid AS fid1,  F.day_of_month AS day_of_month1, F.carrier_id AS cid1, "
  + "   F.flight_num AS flight_num1, F.origin_city AS origin_city1, "
  + "   F.dest_city AS dest_city,  F.capacity AS capacity1, F.price AS price1, F.actual_time AS actual_time1,"
  + " null AS fid2,  null AS  day_of_month2, null AS cid2, " + "   null AS flight_num2, null AS origin_city2, "
  + "   null AS dest_city2,  null AS capacity2, null AS price2, null AS actual_time2 " + "   from Flights F"
  + "   WHERE F.origin_city = ? AND F.dest_city = ?" + " AND F.day_of_month = ? AND F.canceled = 0" + " UNION "
  + " SELECT 2 AS num, (F1.actual_time + F2.actual_time) AS actual_time, "
  + "   F1.fid AS fid1, F1.day_of_month AS day_of_month1, F1.carrier_id AS cid1, "
  + "   F1.flight_num AS flight_num1, F1.origin_city AS origin_city1, "
  + "   F1.dest_city AS dest_city,  F1.capacity AS capacity1, F1.price AS price1, F1.actual_time AS actual_time1, "
  + "   F2.fid AS fid2, F2.day_of_month AS  day_of_month2, F2.carrier_id AS cid2, "
  + "   F2.flight_num AS flight_num2, F2.origin_city AS origin_city2, "
  + "   F2.dest_city AS dest_city2,  F2.capacity AS capacity2, F2.price AS price2, F2.actual_time AS actual_time2 "
  + "   FROM Flights F1, Flights F2 " + "   WHERE F1.dest_city = F2.origin_city AND "
  + "       F1.origin_city = ? AND F2.dest_city = ? " + "       AND F1.day_of_month = ? AND F2.day_of_month = ?"
  + " AND F1.canceled = 0 AND F2.canceled = 0 AND F1.month_id = F2.month_id" + ") AS t "
  + " ORDER BY num, actual_time ASC) AS m ORDER BY actual_time";
  private PreparedStatement searchStmt2;

  // Book
  private static final String FIND_DAY_SQL = "SELECT COUNT(*) AS n FROM RESERVATIONS_LUDINH R, FLIGHTS F WHERE R.fid1 = F.fid AND R.username = ? AND F.day_of_month = ?";
  private PreparedStatement findDayStmt;
  private static final String CHECK_RES_SQL = "SELECT COUNT(*) AS n FROM RESERVATIONS_LUDINH WHERE (fid1 = ? OR fid2 = ?)";
  private PreparedStatement checkResStmt;
  private static final String CREATE_RES_SQL = "INSERT INTO RESERVATIONS_LUDINH(username, fid1, fid2, paid) VALUES(?, ?, ?, 0)";
  private PreparedStatement createResStmt;

  // Pay
  private static final String FIND_RES_SQL = "SELECT SUM(PRICE) AS price FROM "
  + "(SELECT fid1, fid2 FROM RESERVATIONS_LUDINH WHERE rid = ? AND username = ? AND paid = 0) R, FLIGHTS F "
  + "WHERE R.fid1 = F.fid OR R.fid2 = F.fid";
  private PreparedStatement findResStmt;
  private static final String FIND_BAL_SQL = "SELECT balance FROM USERS_LUDINH WHERE username = ?";
  private PreparedStatement findBalStmt;
  private static final String UPDATE_BAL_SQL = "UPDATE USERS_LUDINH SET balance = ? WHERE username = ?";
  private PreparedStatement updateBalStmt;
  private static final String UPDATE_PAID_SQL = "UPDATE RESERVATIONS_LUDINH SET paid = 1 WHERE rid = ?";
  private PreparedStatement updatePaidStmt;

  // Reservation
  private static final String RESERVATIONS_SQL = "SELECT * FROM RESERVATIONS_LUDINH WHERE username = ?";
  private PreparedStatement reservationsStmt;
  private static final String FLIGHTS_SQL = "SELECT day_of_month,carrier_id,flight_num,origin_city,dest)city,actual_time,capacity,price FROM FLIGHTS WHERE fid = ?";
  private PreparedStatement flightsStmt;

  // Private Fields
  private boolean loggedIn;
  private List<List<Integer>> itineraries;
  private String userName;
  private int id;

  protected Query() throws SQLException, IOException {
    loggedIn = false;
    itineraries = null;
    userName = null;
    id = 0;
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      // TODO: YOUR CODE HERE
      clearReservationsStmt.executeUpdate();
      clearUsersStmt.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);

    // TODO: YOUR CODE HERE
    // Clearing Tables
    clearReservationsStmt = conn.prepareStatement(CLEAR_RESERVATIONS_SQL);
    clearUsersStmt = conn.prepareStatement(CLEAR_USERS_SQL);

    // Login
    checkLoginStmt = conn.prepareStatement(CHECK_LOGIN_SQL);

    // Create
    createUserStmt = conn.prepareStatement(CREATE_USER_SQL);

    // Search
    searchStmt1 = conn.prepareStatement(SEARCH_SQL1);
    searchStmt2 = conn.prepareStatement(SEARCH_SQL2);

    // Book
    findDayStmt = conn.prepareStatement(FIND_DAY_SQL);
    checkResStmt = conn.prepareStatement(CHECK_RES_SQL);
    createResStmt = conn.prepareStatement(CREATE_RES_SQL);

    // Pay
    findResStmt = conn.prepareStatement(FIND_RES_SQL);
    findBalStmt = conn.prepareStatement(FIND_BAL_SQL);
    updateBalStmt = conn.prepareStatement(UPDATE_BAL_SQL);
    updatePaidStmt = conn.prepareStatement(UPDATE_PAID_SQL);

    // Reservation
    reservationsStmt = conn.prepareStatement(RESERVATIONS_SQL);
    flightsStmt = conn.prepareStatement(FLIGHTS_SQL);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged in\n".  For all
   *         other errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    // TODO: YOUR CODE HERE
    if (loggedIn) {
      return "User already logged in\n";
    }
    try {
      checkLoginStmt.clearParameters();
      checkLoginStmt.setString(1, username);
      byte[] hashedPswd = null;
      ResultSet rs = checkLoginStmt.executeQuery();
      while (rs.next()) {
        hashedPswd = rs.getBytes("password");
      }
      if (hashedPswd != null && PasswordUtils.plaintextMatchesSaltedHash(password, hashedPswd)) {
        loggedIn = true;
        userName = username;
        return String.format("Logged in as %s\n", username);
      }
    } catch (Exception e) {
      e.printStackTrace();;
    }
    return "Login failed\n";
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE
    if (initAmount < 0) {
      return "Failed to create user\n";
    }
    try {
      checkLoginStmt.clearParameters();
      checkLoginStmt.setString(1, username);
      ResultSet rs = checkLoginStmt.executeQuery();
      boolean userExists = false;
      while (rs.next()) {
        String u = rs.getString("username");
        byte[] p = rs.getBytes("password");
        if (username == u && PasswordUtils.plaintextMatchesSaltedHash(password, p)) {
          userExists = true;
        }
      }
      if (userExists) {
        return "Failed to create user\n";
      }

      byte[] saltedHashed = PasswordUtils.saltAndHashPassword(password);
      
      createUserStmt.clearParameters();
      createUserStmt.setString(1, username);
      createUserStmt.setBytes(2, saltedHashed);
      createUserStmt.setInt(3, initAmount);
      createUserStmt.executeUpdate();
      return String.format("Created user %s\n", username);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "Failed to create user\n";
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given
   * day of the month. If {@code directFlight} is true, it only searches for direct flights,
   * otherwise is searches for direct flights and flights with two "hops." Only searches for up
   * to the number of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return, must be positive
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    // TODO: YOUR CODE HERE

    StringBuffer sbuf = new StringBuffer();

    if (directFlight) {
      try {
        // one hop itineraries
        searchStmt1.clearParameters();
        searchStmt1.setInt(1, numberOfItineraries);
        searchStmt1.setInt(2, dayOfMonth);
        searchStmt1.setString(3, originCity);
        searchStmt1.setString(4, destinationCity);
        
        ResultSet directFlightResults = searchStmt1.executeQuery();
        itineraries = new ArrayList<>();
        int i = 0;
        while (directFlightResults.next()) {
          int result_fid = directFlightResults.getInt("fid");
          int result_dayOfMonth = directFlightResults.getInt("day_of_month");
          String result_carrierId = directFlightResults.getString("carrier_id");
          String result_flightNum = directFlightResults.getString("flight_num");
          String result_originCity = directFlightResults.getString("origin_city");
          String result_destCity = directFlightResults.getString("dest_city");
          int result_time = directFlightResults.getInt("actual_time");
          int result_capacity = directFlightResults.getInt("capacity");
          int result_price = directFlightResults.getInt("price");

          itineraries.add(List.of(result_fid, result_capacity, -1, -1, result_dayOfMonth));

          sbuf.append("Itinerary " + (i) + ": 1 flight(s), " + result_time + " minutes\n");
          i++;
          sbuf.append("ID: " + result_fid + " Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: "
                    + result_flightNum + " Origin: " + result_originCity + " Dest: "
                    + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity
                    + " Price: " + result_price + "\n");
        }
        directFlightResults.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    } else {
      try {
        searchStmt2.clearParameters();
        searchStmt2.setInt(1, numberOfItineraries);
        searchStmt2.setString(2, originCity);
        searchStmt2.setString(3, destinationCity);
        searchStmt2.setInt(4, dayOfMonth);
        searchStmt2.setString(5, originCity);
        searchStmt2.setString(6, destinationCity);
        searchStmt2.setInt(7, dayOfMonth);
        searchStmt2.setInt(8, dayOfMonth);

        ResultSet oneHopResults = searchStmt2.executeQuery();
        itineraries = new ArrayList<>();
        int i = 0;
        while (oneHopResults.next()) {
          int result_sum_time = oneHopResults.getInt("actual_time");
          oneHopResults.getInt("fid2");
          int result_flightNum = oneHopResults.wasNull() ? 1 : 2;
          int result_fid1 = oneHopResults.getInt("fid1");
          int result_dayOfMonth1 = oneHopResults.getInt("day_of_month1");
          String result_carrierId1 = oneHopResults.getString("cid1");
          String result_flightNum1 = oneHopResults.getString("flight_num1");
          String result_originCity1 = oneHopResults.getString("origin_city1");
          String result_destCity1 = oneHopResults.getString("dest_city");
          int result_time1 = oneHopResults.getInt("actual_time1");
          int result_capacity1 = oneHopResults.getInt("capacity1");
          int result_price1 = oneHopResults.getInt("price1");

          sbuf.append(
              "Itinerary " + (i++) + ": " + result_flightNum + " flight(s), " + result_sum_time + " minutes\n");
          sbuf.append("ID: " + result_fid1 + " Day: " + result_dayOfMonth1 + " Carrier: " + result_carrierId1
              + " Number: " + result_flightNum1 + " Origin: " + result_originCity1 + " Dest: " + result_destCity1
              + " Duration: " + result_time1 + " Capacity: " + result_capacity1 + " Price: " + result_price1 + "\n");

          if (result_flightNum == 2) {
            int result_fid2 = oneHopResults.getInt("fid2");
            int result_dayOfMonth2 = oneHopResults.getInt("day_of_month2");
            String result_carrierId2 = oneHopResults.getString("cid2");
            String result_flightNum2 = oneHopResults.getString("flight_num2");
            String result_originCity2 = oneHopResults.getString("origin_city2");
            String result_destCity2 = oneHopResults.getString("dest_city2");
            int result_time2 = oneHopResults.getInt("actual_time2");
            int result_capacity2 = oneHopResults.getInt("capacity2");
            int result_price2 = oneHopResults.getInt("price2");

            itineraries.add(List.of(result_fid1, result_capacity1, result_fid2, result_capacity2, result_dayOfMonth1));

            sbuf.append("ID: " + result_fid2 + " Day: " + result_dayOfMonth2 + " Carrier: " + result_carrierId2
                + " Number: " + result_flightNum2 + " Origin: " + result_originCity2 + " Dest: " + result_destCity2
                + " Duration: " + result_time2 + " Capacity: " + result_capacity2 + " Price: " + result_price2
                + "\n");
          } else {
            itineraries.add(List.of(result_fid1, result_capacity1, -1, -1, result_dayOfMonth1));
          }

        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    if (sbuf.length() == 0) {
      return "No flights match your selection\n";
    }
    return sbuf.toString();
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search
   *                    in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged
   *         in\n". If the user is trying to book an itinerary with an invalid ID or without
   *         having done a search, then return "No such itinerary {@code itineraryId}\n". If the
   *         user already has a reservation on the same day as the one that they are trying to
   *         book now, then return "You cannot book two flights in the same day\n". For all
   *         other errors, return "Booking failed\n".
   *
   *         If booking succeeds, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from
   *         1 and increments by 1 each time a successful reservation is made by any user in
   *         the system.
   */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE
    if (loggedIn != true) {
      return "Cannot book reservations, not logged in\n";
    }

    if (itineraries != null && itineraryId >= 0 && itineraryId < itineraries.size()) {
      boolean deadlock = true;
      while (deadlock) {
        deadlock = false;
        try {
          conn.setAutoCommit(false);

          findDayStmt.clearParameters();
          findDayStmt.setString(1, userName);
          findDayStmt.setInt(2, itineraries.get(itineraryId).get(4));

          ResultSet rs = findDayStmt.executeQuery();
          rs.next();
          int count = rs.getInt("n");
          rs.close();

          if (count > 0) {
            conn.commit();
            conn.setAutoCommit(true);
            return "You cannot book two flights in the same day\n";
          }

          checkResStmt.clearParameters();
          checkResStmt.setInt(1, itineraries.get(itineraryId).get(0));
          checkResStmt.setInt(2, itineraries.get(itineraryId).get(0));

          ResultSet rs2 = checkResStmt.executeQuery();
          rs2.next();
          int remainingCap = itineraries.get(itineraryId).get(1) - rs2.getInt("n");
          rs2.close();

          if (itineraries.get(itineraryId).get(2) >= 0) {
            checkResStmt.clearParameters();
            checkResStmt.setInt(1, itineraries.get(itineraryId).get(2));
            checkResStmt.setInt(2, itineraries.get(itineraryId).get(2));

            rs2 = checkResStmt.executeQuery();
            rs2.next();
            remainingCap = Math.min(itineraries.get(itineraryId).get(3) - rs2.getInt("n"), remainingCap);
            rs2.close();
          }

          if (remainingCap > 0) {
            createResStmt.clearParameters();
            createResStmt.setString(1, userName);
            createResStmt.setInt(2, itineraries.get(itineraryId).get(0));

            int fid2 = itineraries.get(itineraryId).get(2);
            if (fid2 > 0) {
              createResStmt.setInt(3, fid2);
            } else {
              createResStmt.setNull(3, java.sql.Types.INTEGER);
            }

            createResStmt.executeUpdate();
            ResultSet rs3 = createResStmt.getGeneratedKeys();
            rs3.next();
            int rid = rs3.getInt(1);
            rs3.close();
            conn.commit();
            conn.setAutoCommit(true);
            id = rid - 1;
            return "Booked flight(s), reservation ID: " + rid + "\n";
          }

          conn.rollback();
          conn.setAutoCommit(true);
          return "Booking failed\n";
        } catch (SQLException e) {
          deadlock = isDeadlock(e);
          if (deadlock) {
            try {
              conn.rollback();
              conn.setAutoCommit(true);
            } catch (SQLException e2) {
              e2.printStackTrace();
            }
          }
        }
      }
    }
    return "No such itinerary " + itineraryId + "\n";
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n". If the
   *         reservation is not found / not under the logged in user's name, then return
   *         "Cannot find unpaid reservation [reservationId] under user: [username]\n".  If
   *         the user does not have enough money in their account, then return
   *         "User has only [balance] in account but itinerary costs [cost]\n".  For all other
   *         errors, return "Failed to pay for reservation [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE
    if (loggedIn != true) {
      return "Cannot pay, not logged in\n";
    }
    boolean deadlock = true;
    while (deadlock) {
      deadlock = false;
      try {
        conn.setAutoCommit(false);

        findResStmt.clearParameters();
        findResStmt.setInt(1, reservationId);
        findResStmt.setString(2, userName);

        ResultSet rs = findResStmt.executeQuery();
        rs.next();
        int price = rs.getInt("price");
        rs.close();
        if (price == 0) {
          conn.commit();
          conn.setAutoCommit(true);
          return "Cannot find unpaid reservation " + reservationId + " under user: " + userName + "\n";
        }

        findBalStmt.clearParameters();
        findBalStmt.setString(1, userName);
        ResultSet rs2 = findBalStmt.executeQuery();
        if (rs2.next() != true) {
          rs2.close();
          conn.commit();
          conn.setAutoCommit(true);
          return "Failed to pay for reservation " + reservationId + "\n";
        }

        int balance = rs2.getInt("balance");
        rs2.close();

        if (balance < price) {
          conn.commit();
          conn.setAutoCommit(true);
          return "User has only " + balance + " in account but itinerary costs " + price + "\n";
        }

        updateBalStmt.clearParameters();
        updateBalStmt.setInt(1, balance - price);
        updateBalStmt.setString(2, userName);
        updateBalStmt.executeUpdate();

        updatePaidStmt.clearParameters();
        updatePaidStmt.setInt(1, reservationId);
        updatePaidStmt.executeQuery();

        conn.commit();
        conn.setAutoCommit(true);

        return "Paid reservation: " + reservationId + " remaining balance: " + (balance - price) + "\n";
      } catch (SQLException e) {
        deadlock = isDeadlock(e);
        try {
          conn.rollback();
          conn.setAutoCommit(true);
        } catch (SQLException e2) {
          e2.printStackTrace();
        }
      }
    }
    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE
    if (loggedIn != true) {
      return "Cannot view reservations, not logged in\n";
    }

    boolean deadlock = true;
    while (deadlock) {
      deadlock = false;

      try {
        conn.setAutoCommit(false);
        reservationsStmt.clearParameters();
        reservationsStmt.setString(1, userName);

        ResultSet rs = reservationsStmt.executeQuery();
        int i = 0;
        StringBuffer sbuf = new StringBuffer();

        while (rs.next()) {
          i++;
          int rid = rs.getInt("rid");
          int fid1 = rs.getInt("fid1");
          boolean paid = rs.getBoolean("paid");

          flightsStmt.clearParameters();
          flightsStmt.setInt(1, fid1);
          ResultSet rs2 = flightsStmt.executeQuery();

          rs2.next();
          int result_dayOfMonth = rs2.getInt("day_of_month");
          String result_carrierId = rs2.getString("carrier_id");
          int result_flightNum = rs2.getInt("flight_num");
          String result_originCity = rs2.getString("origin_city");
          String result_destCity = rs2.getString("dest_city");
          int result_actualTime = rs2.getInt("actual_time");
          int result_capacity = rs2.getInt("capacity");
          int result_price = rs2.getInt("price");
          rs2.close();

          sbuf.append("Reservation " + rid + " paid: " + paid + ":\n" 
                      + "ID: " + fid1 + " Day: "
                      + result_dayOfMonth + " Carrier: " 
                      + result_carrierId + " Number: " 
                      + result_flightNum + " Origin: "
                      + result_originCity + " Dest: " 
                      + result_destCity + " Duration: " 
                      + result_actualTime + " Capacity: "
                      + result_capacity + " Price: " 
                      + result_price + "\n");
          
          int fid2 = rs.getInt("fid2");

          if (!rs.wasNull()) {
            flightsStmt.clearParameters();
            flightsStmt.setInt(1, fid2);

            rs2 = flightsStmt.executeQuery();
            rs2.next();

            int result_dayOfMonth2 = rs2.getInt("day_of_month");
            String result_carrierId2 = rs2.getString("carrier_id");
            int result_flightNum2 = rs2.getInt("flight_num");
            String result_originCity2 = rs2.getString("origin_city");
            String result_destCity2 = rs2.getString("dest_city");
            int result_actualTime2 = rs2.getInt("actual_time");
            int result_capacity2 = rs2.getInt("capacity");
            int result_price2 = rs2.getInt("price");
            rs2.close();

            sbuf.append("ID: " + fid2 + " Day: "
                      + result_dayOfMonth2 + " Carrier: " 
                      + result_carrierId2 + " Number: " 
                      + result_flightNum2 + " Origin: "
                      + result_originCity2 + " Dest: " 
                      + result_destCity2 + " Duration: " 
                      + result_actualTime2 + " Capacity: "
                      + result_capacity2 + " Price: " 
                      + result_price2 + "\n");
          }
        }
        rs.close();
        conn.commit();
        conn.setAutoCommit(true);

        if (i == 0) {
          return "No reservations found\n";
        }

        return sbuf.toString();

      } catch (SQLException e) {
        deadlock = isDeadlock(e);
          try {
            conn.rollback();
            conn.setAutoCommit(true);
          } catch (SQLException e2) {
            e2.printStackTrace();
          }
      }
    }
    return "Failed to retrieve reservations\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }
}
