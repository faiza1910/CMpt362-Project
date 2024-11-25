package com.example.travelogue.ui.login_reg

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.travelogue.Globals.PREF_NAME
import com.example.travelogue.MainActivity
import com.example.travelogue.R
import com.example.travelogue.db_user.UserDao
import com.example.travelogue.db_user.UserDatabase
import com.example.travelogue.db_user.UserRepository
import com.example.travelogue.db_user.UserViewModel
import com.example.travelogue.db_user.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEmail: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var regButton: Button

    private lateinit var database: UserDatabase
    private lateinit var databaseDao: UserDao
    private lateinit var repository: UserRepository
    private lateinit var viewModelFactory: UserViewModelFactory
    private lateinit var userViewModel: UserViewModel

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        //initialize firebase authentication
        mAuth = FirebaseAuth.getInstance()

        usernameEmail = findViewById(R.id.user_name_email)
        password = findViewById(R.id.user_pw)
        loginButton = findViewById(R.id.login_button)
        regButton = findViewById(R.id.register_button)

        //db initialize
        database = UserDatabase.getInstance(this)
        databaseDao = database.userDao
        repository = UserRepository(databaseDao)
        viewModelFactory = UserViewModelFactory(repository)
        userViewModel =
            ViewModelProvider(this, viewModelFactory).get(UserViewModel::class.java)

        //display users in log cat
        userViewModel.allUsersLiveData.observe(this) { users ->
            if (users.isNotEmpty()) {
                users.forEach { user ->
                    Log.d("LoginActivity", "User : $user")
                }
            } else {
                Log.d("LoginActivity", "No users found in the database.")
            }
        }

        loginButton.setOnClickListener {
            val emailAUTH = usernameEmail.text.toString().trim()
            val userPasswordAUTH = password.text.toString().trim()

            if (emailAUTH.isEmpty() || userPasswordAUTH.isEmpty()) {
                Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.signInWithEmailAndPassword(emailAUTH, userPasswordAUTH)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        //db stuff
                        val intent = Intent(this, MainActivity::class.java)
                        val usernameOrEmail = usernameEmail.text.toString()
                        val userPassword = password.text.toString()
                        var isUserFound = false
                        var userId = -1L
                        userViewModel.allUsersLiveData.observe(this) { users ->
                            if (users.isNotEmpty()) {
                                users.forEach { user ->
                                    if ((user.userEmail == usernameOrEmail || user.userName == usernameOrEmail) && user.userPassword == userPassword) {
                                        isUserFound = true
                                        userId = user.userId
                                    }
                                }
                            }
                        }
                        if(isUserFound) {
                            //share user id across the app
                            val sharedPreferences: SharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.clear()
                            editor.putLong("user_id", userId)
                            Log.d("myuserid", "User : $userId")
                            editor.commit()
                            startActivity(intent)
                            finish()
                        }

                    }
                    else
                    {
                        // Login failed
                        Log.e("LoginActivity", "Login failed: ${task.exception?.message}")
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        regButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }
}