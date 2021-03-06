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

package smash.ks.com.domain.repositories

import io.reactivex.Completable
import io.reactivex.Single
import smash.ks.com.domain.Labels
import smash.ks.com.domain.models.KsModel
import smash.ks.com.domain.models.UploadResultModel
import smash.ks.com.domain.parameters.KsAnalyzeImageParam
import smash.ks.com.domain.parameters.KsParam
import smash.ks.com.domain.parameters.KsPhotoToCloudinaryParam
import smash.ks.com.domain.parameters.Parameterable
import smash.ks.com.domain.parameters.PhotoParam

/**
 * This interface will be the similar to [smash.ks.com.data.datastores.DataStore] .
 */
interface DataRepository {
    //region Fake
    /**
     * Retrieve a specific image/all images from the remote/local database.
     * @param params when `null` means that it'll retrieve all images, otherwise [KsParam],
     *               will be a specific image.
     * @return
     */
    fun fetchKsImage(params: Parameterable? = null): Single<KsModel>

    /**
     * Store an image to the database.
     *
     * @param params [KsParam] parameter.
     * @return [Completable]
     */
    fun storeKsImage(params: Parameterable = KsParam()): Completable
    //endregion

    /**
     * Upload an image to Firebase for analysis the image.
     *
     * @param params [PhotoParam] parameter.
     * @return [Completable]
     */
    fun uploadImage(params: Parameterable = PhotoParam()): Completable

    /**
     * Store an image to Cloudinary service.
     *
     * @param params [KsPhotoToCloudinaryParam]
     * @return Single<UploadResultModel>
     */
    fun storeImageToCloudinary(params: Parameterable = KsPhotoToCloudinaryParam()): Single<UploadResultModel>

    /**
     * Retrieve the tags of an image info after it passed ML model.
     *
     * @param params [KsAnalyzeImageParam]
     * @return
     */
    fun fetchImageTagsByML(params: Parameterable = KsAnalyzeImageParam()): Single<Labels>

    /**
     * Retrieve the key words of an image info after it passed ML model.
     *
     * @param params
     * @return
     */
    fun fetchImageWordContentByML(params: Parameterable): Single<String>
}
