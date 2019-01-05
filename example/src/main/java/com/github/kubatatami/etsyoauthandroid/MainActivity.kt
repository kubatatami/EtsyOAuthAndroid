package com.github.kubatatami.etsyoauthandroid

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
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
        EtsyOAuth.logoutObservable.subscribe {
            user.isEnabled = false
        }
        user.setOnClickListener {
            // you can use retrofit as well. just use okhttp instance in retrofit builder.
            okHttpClient
                .rxEnqueue(Request.Builder().url("https://openapi.etsy.com/v2/shops/__SELF__").build())
                .subscribe({
                    if (it.isSuccessful) {
                        Toast.makeText(this, it.body()!!.string(), Toast.LENGTH_SHORT).show()
                    }
                }, Throwable::printStackTrace)
        }
        login.setOnClickListener {
            EtsyOAuth.login(this).subscribe({
                    user.isEnabled = true
                }, Throwable::printStackTrace)
        }
    }
}
