package com.example.printxpress;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PrintXpress.db";
    private static final int DATABASE_VERSION = 8;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT," +
                        "email TEXT UNIQUE," +
                        "password TEXT," +
                        "address TEXT," +
                        "phone TEXT)"
        );

        db.execSQL(
                "CREATE TABLE orders (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "user_id INTEGER NOT NULL DEFAULT -1," +
                        "product TEXT," +
                        "quantity INTEGER," +
                        "order_method TEXT," +
                        "status TEXT," +
                        "file_name TEXT," +
                        "schedule TEXT," +
                        "paper_type TEXT," +
                        "paper_size TEXT," +
                        "print_type TEXT," +
                        "instructions TEXT)"
        );

        db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_nocase " +
                        "ON users(email COLLATE NOCASE)"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE users ADD COLUMN address TEXT");
            db.execSQL("ALTER TABLE users ADD COLUMN phone TEXT");
        }

        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE orders ADD COLUMN paper_type TEXT");
            db.execSQL("ALTER TABLE orders ADD COLUMN paper_size TEXT");
            db.execSQL("ALTER TABLE orders ADD COLUMN print_type TEXT");
        }

        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE orders ADD COLUMN instructions TEXT");
        }

        if (oldVersion < 8) {
            // Existing orders remain unassigned because their original
            // owners cannot be determined from the old database.
            db.execSQL(
                    "ALTER TABLE orders ADD COLUMN user_id " +
                            "INTEGER NOT NULL DEFAULT -1"
            );
        }

        ensureCaseInsensitiveEmailIndex(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        // Also check existing databases that are already version 8.
        ensureCaseInsensitiveEmailIndex(db);
    }

    private void ensureCaseInsensitiveEmailIndex(SQLiteDatabase db) {
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT 1 FROM users " +
                            "WHERE email IS NOT NULL " +
                            "GROUP BY email COLLATE NOCASE " +
                            "HAVING COUNT(*) > 1 LIMIT 1",
                    null
            );

            boolean hasDuplicates = cursor.moveToFirst();
            cursor.close();
            cursor = null;

            if (!hasDuplicates && db.isReadOnly() == false) {
                db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                                "idx_users_email_nocase " +
                                "ON users(email COLLATE NOCASE)"
                );
            }
        } catch (SQLException exception) {
            // Registration still checks for duplicate emails inside
            // a transaction if an index cannot be created.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean emailExists(SQLiteDatabase db, String email) {
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM users " +
                        "WHERE email = ? COLLATE NOCASE LIMIT 1",
                new String[]{normalizeEmail(email)}
        );

        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public boolean emailExists(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = getReadableDatabase();
        return emailExists(db, normalizedEmail);
    }

    public boolean addUser(
            String name,
            String email,
            String password
    ) {
        String normalizedEmail = normalizeEmail(email);

        if (name == null || name.trim().isEmpty()
                || normalizedEmail.isEmpty()
                || password == null || password.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        boolean registered = false;

        db.beginTransaction();

        try {
            // Check again inside the transaction to avoid relying
            // only on the earlier check in RegisterActivity.
            if (emailExists(db, normalizedEmail)) {
                return false;
            }

            ContentValues values = new ContentValues();
            values.put("name", name.trim());
            values.put("email", normalizedEmail);
            values.put("password", password.trim());

            long result = db.insertOrThrow("users", null, values);

            if (result != -1) {
                db.setTransactionSuccessful();
                registered = true;
            }
        } catch (SQLException exception) {
            // For example, a unique-email constraint violation.
            registered = false;
        } finally {
            db.endTransaction();
        }

        return registered;
    }

    public boolean checkUser(String email, String password) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail.isEmpty() || password == null) {
            return false;
        }

        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id FROM users " +
                        "WHERE email = ? COLLATE NOCASE AND password = ? " +
                        "LIMIT 1",
                new String[]{normalizedEmail, password.trim()}
        );

        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public int getUserId(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail.isEmpty()) {
            return -1;
        }

        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id FROM users " +
                        "WHERE email = ? COLLATE NOCASE LIMIT 1",
                new String[]{normalizedEmail}
        );

        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        } finally {
            cursor.close();
        }
    }

    public Cursor getUser(String email) {
        String normalizedEmail = normalizeEmail(email);

        return getReadableDatabase().rawQuery(
                "SELECT name, email, address, phone FROM users " +
                        "WHERE email = ? COLLATE NOCASE LIMIT 1",
                new String[]{normalizedEmail}
        );
    }

    public boolean addOrder(
            int userId,
            String product,
            int quantity,
            String orderMethod,
            String status,
            String fileName,
            String paperType,
            String paperSize,
            String printType,
            String instructions
    ) {
        if (userId <= 0) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("product", product);
        values.put("quantity", quantity);
        values.put("order_method", orderMethod);
        values.put("status", status);
        values.put("file_name", fileName);
        values.put("schedule", "Not scheduled");
        values.put("paper_type", paperType);
        values.put("paper_size", paperSize);
        values.put("print_type", printType);
        values.put("instructions", instructions);

        return getWritableDatabase().insert("orders", null, values) != -1;
    }

    public Cursor getOrdersForUser(int userId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM orders WHERE user_id = ? ORDER BY id DESC",
                new String[]{String.valueOf(userId)}
        );
    }

    public boolean cancelOrder(int orderId, int userId) {
        if (userId <= 0) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("status", "Cancelled");

        return getWritableDatabase().update(
                "orders",
                values,
                "id = ? AND user_id = ? AND status != ?",
                new String[]{
                        String.valueOf(orderId),
                        String.valueOf(userId),
                        "Printing"
                }
        ) > 0;
    }

    public boolean rescheduleOrder(
            int orderId,
            int userId,
            String schedule
    ) {
        if (userId <= 0 || schedule == null || schedule.trim().isEmpty()) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("schedule", schedule.trim());
        values.put("status", "Rescheduled");

        return getWritableDatabase().update(
                "orders",
                values,
                "id = ? AND user_id = ? AND status != ?",
                new String[]{
                        String.valueOf(orderId),
                        String.valueOf(userId),
                        "Printing"
                }
        ) > 0;
    }
}
