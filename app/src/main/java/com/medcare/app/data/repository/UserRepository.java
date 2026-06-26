package com.medcare.app.data.repository;

import android.content.Context;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.UserDao;
import com.medcare.app.data.entity.User;

import java.util.List;

public class UserRepository {

    private final UserDao userDao;

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
    }

    public long insert(User user) {
        return userDao.insert(user);
    }

    public void update(User user) {
        userDao.update(user);
    }

    public void delete(User user) {
        userDao.delete(user);
    }

    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    public User getUserById(long id) {
        return userDao.getUserById(id);
    }

    public User login(String email, String password) {
        return userDao.login(email, password);
    }

    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }
}
