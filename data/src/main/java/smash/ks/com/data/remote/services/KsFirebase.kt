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

package smash.ks.com.data.remote.services

import io.reactivex.Completable
import io.reactivex.Single
import smash.ks.com.data.datas.KsData
import smash.ks.com.data.datas.LabelDatas
import smash.ks.com.domain.Label
import smash.ks.com.domain.parameters.Parameterable

/**
 * The access operations for Firebase service.
 * NOTE: if the number of parameters is more than five, just pass the parameterable.
 */
interface KsFirebase {
    fun retrieveImages(name: String): Single<KsData>

    fun uploadImage(params: Parameterable): Completable

    fun retrieveImageTagsByML(imageByteArray: ByteArray): Single<LabelDatas>

    fun retrieveImageWordContentByML(params: Parameterable): Single<Label>
}
