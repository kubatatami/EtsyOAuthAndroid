package com.github.kubatatami.etsyoauthandroid

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

fun OkHttpClient.rxEnqueue(request: Request): Single<Response> {
    return Single.fromCallable { newCall(request).execute() }
        .flatMap {
            if (it.isSuccessful) Single.just(it)
            else Single.error(Exception(it.code().toString()))
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
}