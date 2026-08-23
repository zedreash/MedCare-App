package com.medcare.app.data.repository;

import android.content.Context;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.UserDao;
import com.medcare.app.data.entity.User;
import com.medcare.app.utils.PasswordUtils;

import java.util.List;

public class UserRepository {
    private final UserDao userDao;

    public interface Callback<T> {
        void onResult(T result);
    }

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
    }

    public long insert(User user) {
        return userDao.insert(user);
    }

    public void insert(User user, Callback<Long> callback) {
        AppDatabase.getExecutor().execute(() -> {
            long id = userDao.insert(user);
            AppDatabase.runOnMainThread(() -> callback.onResult(id));
        });
    }

    public void update(User user) {
        userDao.update(user);
    }

    public void update(User user, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            userDao.update(user);
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public void delete(User user) {
        userDao.delete(user);
    }

    public void delete(User user, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            userDao.delete(user);
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    public void getUserByEmail(String email, Callback<User> callback) {
        AppDatabase.getExecutor().execute(() -> {
            User user = userDao.getUserByEmail(email);
            AppDatabase.runOnMainThread(() -> callback.onResult(user));
        });
    }

    public void getUserByTzNumber(String tz, Callback<User> callback) {
        AppDatabase.getExecutor().execute(() -> {
            User user = userDao.getUserByTzNumber(tz);
            AppDatabase.runOnMainThread(() -> callback.onResult(user));
        });
    }

    public User getUserById(long id) {
        return userDao.getUserById(id);
    }

    public void getUserById(long id, Callback<User> callback) {
        AppDatabase.getExecutor().execute(() -> {
            User user = userDao.getUserById(id);
            AppDatabase.runOnMainThread(() -> callback.onResult(user));
        });
    }

    public void login(String email, String password, Callback<User> callback) {
        AppDatabase.getExecutor().execute(() -> {
            User user = userDao.getUserByEmail(email);
            if (user != null && PasswordUtils.verify(password, email, user.getPassword())) {
                AppDatabase.runOnMainThread(() -> callback.onResult(user));
            } else {
                AppDatabase.runOnMainThread(() -> callback.onResult(null));
            }
        });
    }

    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }
}
