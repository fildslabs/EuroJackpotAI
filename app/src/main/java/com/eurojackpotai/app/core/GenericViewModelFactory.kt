package com.eurojackpotai.app.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * A one-line `ViewModelProvider.Factory` for manual constructor injection, used instead
 * of a DI framework (see [AppContainer]'s doc comment for why). Each screen does:
 *
 * ```
 * val app = LocalContext.current.applicationContext as EuroJackpotApp
 * val viewModel: HomeViewModel = viewModel(
 *     factory = GenericViewModelFactory { HomeViewModel(app.container.lotteryRepository) }
 * )
 * ```
 */
class GenericViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}
