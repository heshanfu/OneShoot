/*
 * Copyright (C) 2018 The Smash Ks Open Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smash.ks.com.oneshoot.bases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.devrapid.kotlinshaver.cast
import org.kodein.di.generic.instance
import java.lang.reflect.ParameterizedType

/**
 * The basic activity is in MVVM architecture which prepares all necessary variables or functions.
 */
abstract class AdvActivity<out VM : ViewModel> : BaseActivity() {
    /** Add the AAC [ViewModel] for each fragments. */
    @Suppress("UNCHECKED_CAST")
    protected val vm
        get() = vmCreateMethod.invoke(vmProviders, vmConcreteClass) as? VM ?: throw ClassCastException()

    private val viewModelFactory by instance<ViewModelProvider.Factory>()
    /** [VM] is the first (index: 0) in the generic declare. */
    private val vmConcreteClass
        get() = cast<Class<*>>(cast<ParameterizedType>(this::class.java.genericSuperclass).actualTypeArguments[0])
    private val vmProviders get() = ViewModelProviders.of(this, viewModelFactory)
    /** The [ViewModelProviders.of] function for obtaining a [ViewModel]. */
    private val vmCreateMethod get() = vmProviders.javaClass.getMethod("get", vmConcreteClass.superclass.javaClass)
}
