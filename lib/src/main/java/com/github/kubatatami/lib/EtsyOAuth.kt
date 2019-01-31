package com.github.kubatatami.lib

import android.app.Activity
import android.app.Application
import android.net.Uri
import com.github.kubatatami.oauth.OAuth1Helper
import com.github.kubatatami.rxweb.RxLoginWebView
import com.github.kubatatami.web.LoginWebView
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLDecoder

object EtsyOAuth {

    private const val ACCCESS_TOKEN_URL = "https://openapi.etsy.com/v2/oauth/access_token?oauth_verifier="
    private const val REQUEST_TOKEN_URL = "https://openapi.etsy.com/v2/oauth/request_token"
    private val okHttpClient = OkHttpClient.Builder().build()
    private lateinit var oAuthHelper: OAuth1Helper
    var oauthToken: String? = null
        private set
    var oauthTokenSecret: String? = null
        private set

    private val loginSubject = BehaviorSubject.createDefault(false)
    val loginObservable: Observable<Boolean> = loginSubject.observeOn(AndroidSchedulers.mainThread())

    @JvmStatic
    val interceptor: Interceptor = Interceptor {
        val builder = it.request().newBuilder()
        if (isLogged()) builder.addHeader(
            "Authorization",
            "OAuth ${oAuthHelper.createHeader(oauthToken!!, oauthTokenSecret!!)}"
        )
        val result = it.proceed(builder.build())
        if (result.code() == 403) {
            loginSubject.onNext(false)
            logout()
        }
        return@Interceptor result
    }

    @JvmStatic
    fun initialize(app: Application) {
        RxLoginWebView.initialization(app)
    }

    @JvmStatic
    fun setConsumerCredentials(consumerKey: String, consumerSecret: String) {
        oAuthHelper = OAuth1Helper(consumerKey, consumerSecret)
    }

    @JvmStatic
    fun login(activity: Activity, vararg scope: String): Completable {
        return Single.fromCallable {
            oAuthHelper.requestToken(REQUEST_TOKEN_URL +
                    "?scope=${scope.joinToString("%20")}&oauth_callback=${LoginWebView.getCallbackUrl(activity)}")
        }
            .flatMap { okHttpClient.rxEnqueue(it) }
            .flatMap { response ->
                val uri = Uri.parse("?" + response.body()!!.string())
                val url = URLDecoder.decode(uri.getQueryParameter("login_url"), "utf-8")
                val oauthToken = URLDecoder.decode(uri.getQueryParameter("oauth_token"), "utf-8")
                val oauthTokenSecret = URLDecoder.decode(uri.getQueryParameter("oauth_token_secret"), "utf-8")
                RxLoginWebView.open(activity, url)
                    .flatMap {
                        val accessTokenRequest =
                            oAuthHelper.accessToken(
                                ACCCESS_TOKEN_URL + it["oauth_verifier"],
                                oauthToken,
                                oauthTokenSecret
                            )
                        okHttpClient.rxEnqueue(accessTokenRequest)
                    }
            }
            .doOnSuccess { response ->
                val uri = Uri.parse("?" + response.body()!!.string())
                oauthToken = URLDecoder.decode(uri.getQueryParameter("oauth_token"), "utf-8")
                oauthTokenSecret = URLDecoder.decode(uri.getQueryParameter("oauth_token_secret"), "utf-8")
                loginSubject.onNext(true)
            }
            .ignoreElement()
    }

    @JvmStatic
    fun setCredentials(oauthToken: String, oauthTokenSecret: String) {
        this.oauthToken = oauthToken
        this.oauthTokenSecret = oauthTokenSecret
        loginSubject.onNext(true)
    }

    @JvmStatic
    fun isLogged() = oauthToken != null && oauthTokenSecret != null

    @JvmStatic
    fun logout() {
        oauthToken = null
        oauthTokenSecret = null
    }
}

private fun OkHttpClient.rxEnqueue(request: Request): Single<Response> {
    return Single.fromCallable { newCall(request).execute() }
        .flatMap {
            if (it.isSuccessful) Single.just(it)
            else Single.error(Exception(it.code().toString()))
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
}