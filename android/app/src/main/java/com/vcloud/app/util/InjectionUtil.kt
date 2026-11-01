package com.vcloud.app.util

import com.vcloud.app.data.AppsRepository
import com.vcloud.app.data.GmsRepository

import com.vcloud.app.view.apps.AppsFactory
import com.vcloud.app.view.gms.GmsFactory
import com.vcloud.app.view.list.ListFactory



object InjectionUtil {

    private val appsRepository = AppsRepository()



    private val gmsRepository = GmsRepository()


    fun getAppsFactory() : AppsFactory {
        return AppsFactory(appsRepository)
    }

    fun getListFactory(): ListFactory {
        return ListFactory(appsRepository)
    }


    fun getGmsFactory():GmsFactory{
        return GmsFactory(gmsRepository)
    }

}