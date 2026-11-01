package com.vcloud.app.view.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vcloud.app.data.AppsRepository


@Suppress("UNCHECKED_CAST")
class ListFactory(private val appsRepository: AppsRepository) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ListViewModel(appsRepository) as T
    }
}