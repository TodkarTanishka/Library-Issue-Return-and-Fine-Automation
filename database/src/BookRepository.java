package database;

import model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookRepository {

    public List<Book> findAll() throws SQLException {

        List<Book> books = new ArrayList<>();

        String sql =
                "SELECT id, book_id, title, author, isbn, category, " +
                "total_quantity, available_quantity " +
                "FROM books ORDER BY id";

        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {

            while (rs.next()) {
                books.add(map(rs));
            }
        }

        return books;
    }

    public Book findByBookId(String bookId) throws SQLException {

        String sql =
                "SELECT id, book_id, title, author, isbn, category, " +
                "total_quantity, available_quantity " +
                "FROM books WHERE book_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, bookId);

            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }

        return null;
    }

    public void updateAvailable(String bookId, int quantity)
            throws SQLException {

        String sql =
                "UPDATE books SET available_quantity=? WHERE book_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, quantity);
            p.setString(2, bookId);

            p.executeUpdate();
        }
    }

    public Book insert(
            String isbn,
            String title,
            String author,
            String category,
            int copies) throws SQLException {

        String bookId =
                "MMLIB" + (System.currentTimeMillis() % 100000);

        String sql =
                "INSERT INTO books " +
                "(book_id, title, author, isbn, category, " +
                "total_quantity, available_quantity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement p =
                     c.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            p.setString(1, bookId);
            p.setString(2, title);
            p.setString(3, author);
            p.setString(4, isbn);
            p.setString(5, category);
            p.setInt(6, copies);
            p.setInt(7, copies);

            p.executeUpdate();

            return findByBookId(bookId);
        }
    }

    public String[] addBookProcedure(String isbn, String title, String author, String category, String department, int totalQuantity) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_add_book(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, isbn != null ? isbn.trim() : "");
            cs.setString(2, title != null ? title.trim() : "");
            cs.setString(3, author != null ? author.trim() : "");
            cs.setString(4, category != null ? category.trim() : "General");
            cs.setString(5, department != null ? department.trim() : "Computer Engineering");
            cs.setInt(6, totalQuantity);
            cs.registerOutParameter(7, Types.BOOLEAN);
            cs.registerOutParameter(8, Types.VARCHAR);
            cs.registerOutParameter(9, Types.VARCHAR);
            cs.execute();
            boolean success = cs.getBoolean(7);
            String message = cs.getString(8);
            String bookId = cs.getString(9);
            return new String[]{String.valueOf(success), message, bookId};
        }
    }

    public String[] updateBookProcedure(String bookId, String title, String author, String isbn, String category, String department, int newTotal) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_update_book(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, bookId);
            cs.setString(2, title != null ? title.trim() : "");
            cs.setString(3, author != null ? author.trim() : "");
            cs.setString(4, isbn != null ? isbn.trim() : "");
            cs.setString(5, category != null ? category.trim() : "General");
            cs.setString(6, department != null ? department.trim() : "Computer Engineering");
            cs.setInt(7, newTotal);
            cs.registerOutParameter(8, Types.BOOLEAN);
            cs.registerOutParameter(9, Types.VARCHAR);
            cs.execute();
            boolean success = cs.getBoolean(8);
            String message = cs.getString(9);
            return new String[]{String.valueOf(success), message};
        }
    }

    public String[] deleteBookProcedure(String bookId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_delete_book(?, ?, ?)}")) {
            cs.setString(1, bookId);
            cs.registerOutParameter(2, Types.BOOLEAN);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();
            boolean success = cs.getBoolean(2);
            String message = cs.getString(3);
            return new String[]{String.valueOf(success), message};
        }
    }

    private Book map(ResultSet rs) throws SQLException {

        return new Book(
                rs.getInt("id"),
                rs.getString("book_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("isbn"),
                rs.getString("category"),
                rs.getInt("total_quantity"),
                rs.getInt("available_quantity")
        );
    }
}