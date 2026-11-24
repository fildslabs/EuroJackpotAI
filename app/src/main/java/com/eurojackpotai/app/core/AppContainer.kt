package com.eurojackpotai.app.core

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.eurojackpotai.app.BuildConfig
import com.eurojackpotai.app.data.local.AppDatabase
import com.eurojackpotai.app.data.local.CurrencyPreferences
import com.eurojackpotai.app.data.local.SyncThrottle
import com.eurojackpotai.app.data.ocr.TicketTextRecognizer
import com.eurojackpotai.app.data.remote.CurrencyRateRemoteDataSource
import com.eurojackpotai.app.data.remote.LottoOpenApiRemoteDataSource
import com.eurojackpotai.app.data.remote.RemoteLotteryDataSource
import com.eurojackpotai.app.data.remote.api.FrankfurterApiService
import com.eurojackpotai.app.data.remote.api.LottoOpenApiService
import com.eurojackpotai.app.data.repository.CurrencyRepositoryImpl
import com.eurojackpotai.app.data.repository.LotteryRepositoryImpl
import com.eurojackpotai.app.data.repository.SavedNumberRepositoryImpl
import com.eurojackpotai.app.domain.repository.SavedNumberRepository
import com.eurojackpotai.app.domain.usecase.CompareNumbersUseCase
import com.eurojackpotai.app.domain.usecase.ComputeStatisticsUseCase
import com.eurojackpotai.app.domain.usecase.GenerateNumbersUseCase
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual, no-framework dependency container. The app is intentionally small enough
 * (single module, five screens) that Hilt/Dagger would add build-time complexity
 * (annotation processors, generated code, a steeper contributor ramp-up) without a
 * matching benefit — see product spec section 1 ("don't add unnecessary complexity").
 * Everything here is a plain lazy singleton, built once in [EuroJackpotApp.onCreate]
 * and handed to ViewModel factories.
 */
class AppContainer(context: Context) {

    init {
        // Never logs the key itself — just whether the build actually picked one up
        // from local.properties, which is the #1 cause of "still showing sample data"
        // reports (edited local.properties without a full rebuild afterward). Filter
        // Logcat by "EuroJackpotSync" to see this alongside every sync attempt's result.
        if (BuildConfig.LOTTO_API_KEY.isBlank()) {
            Log.w("EuroJackpotSync", "AppContainer: BuildConfig.LOTTO_API_KEY is blank — this build has no API key baked in. Add LOTTO_API_KEY to local.properties and rebuild (a plain Run may not be enough if Gradle considers BuildConfig up-to-date; try Build > Clean Project first).")
        } else {
            Log.i("EuroJackpotSync", "AppContainer: LOTTO_API_KEY is configured (${BuildConfig.LOTTO_API_KEY.length} chars)")
        }
    }

    private val database: AppDatabase by lazy {
        // No real Migrations exist yet — every schema version bump (see AppDatabase's
        // version history) relies on this destructive fallback, which just wipes and
        // recreates local data (saved numbers, cached draw history) instead of crashing
        // when it finds an on-device DB whose schema no longer matches the compiled
        // entities. Fine for this dev-phase app; would need real Migrations before a
        // release build ships to users who'd lose data on every update.
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    private val syncThrottle: SyncThrottle by lazy { SyncThrottle(context) }

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
    }

    private val lottoOpenApiService: LottoOpenApiService by lazy {
        Retrofit.Builder()
            .baseUrl(LottoOpenApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(CONTENT_TYPE_JSON.toMediaType()))
            .build()
            .create(LottoOpenApiService::class.java)
    }

    private val remoteLotteryDataSource: RemoteLotteryDataSource by lazy {
        LottoOpenApiRemoteDataSource(api = lottoOpenApiService, apiKey = BuildConfig.LOTTO_API_KEY)
    }

    val lotteryRepository: LotteryRepositoryImpl by lazy {
        LotteryRepositoryImpl(dao = database.drawResultDao(), remote = remoteLotteryDataSource, syncThrottle = syncThrottle)
    }

    // Frankfurter needs no API key (see FrankfurterApiService) and no auth interceptor,
    // so this is a separate, simpler Retrofit client rather than reusing lottoOpenApiService.
    private val frankfurterApiService: FrankfurterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(FrankfurterApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(CONTENT_TYPE_JSON.toMediaType()))
            .build()
            .create(FrankfurterApiService::class.java)
    }

    private val currencyRateRemoteDataSource: CurrencyRateRemoteDataSource by lazy {
        CurrencyRateRemoteDataSource(api = frankfurterApiService)
    }

    private val currencyPreferences: CurrencyPreferences by lazy { CurrencyPreferences(context) }

    val currencyRepository: CurrencyRepositoryImpl by lazy {
        CurrencyRepositoryImpl(remote = currencyRateRemoteDataSource, prefs = currencyPreferences)
    }

    val savedNumberRepository: SavedNumberRepository by lazy {
        SavedNumberRepositoryImpl(dao = database.savedNumberDao())
    }

    // No API key, no config to check (unlike lottoOpenApiService/frankfurterApiService)
    // — ML Kit's on-device model needs no credentials at all. See TicketTextRecognizer.
    val ticketTextRecognizer: TicketTextRecognizer by lazy { TicketTextRecognizer() }

    val computeStatisticsUseCase: ComputeStatisticsUseCase by lazy { ComputeStatisticsUseCase() }
    val generateNumbersUseCase: GenerateNumbersUseCase by lazy { GenerateNumbersUseCase() }
    val compareNumbersUseCase: CompareNumbersUseCase by lazy { CompareNumbersUseCase() }

    private companion object {
        const val CONTENT_TYPE_JSON = "application/json"
    }
}
