package com.github.kubatatami.etsyoauthandroid

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.github.kubatatami.lib.EtsyOAuth
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(EtsyOAuth.interceptor)
        .build()

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EtsyOAuth.loginObservable.subscribe {
            user.isEnabled = it
        }
        user.setOnClickListener {
            // you can use retrofit as well. just use okhttp instance in retrofit builder.
            okHttpClient
                .rxEnqueue(Request.Builder().url("https://openapi.etsy.com/v2/shops/__SELF__").build())
                .subscribe({ response ->
                    if (response.isSuccessful) {
                        Toast.makeText(this, response.body()!!.string(), Toast.LENGTH_SHORT).show()
                    }
                }, Throwable::printStackTrace)
        }
        login.setOnClickListener {
            EtsyOAuth.login(this, "email_r").subscribe({}, Throwable::printStackTrace)
        }
    }
}
