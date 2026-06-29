package com.medcare.app.data.db;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.medcare.app.data.entity.User;
import java.util.List;
@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(User user);
    @Update
    void update(User user);
    @Delete
    void delete(User user);
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(long id);
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User login(String email, String password);
    @Query("SELECT * FROM users")
    List<User> getAllUsers();
}
