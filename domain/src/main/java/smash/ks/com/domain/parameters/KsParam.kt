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

package smash.ks.com.domain.parameters

import smash.ks.com.ext.const.DEFAULT_LONG
import smash.ks.com.ext.const.DEFAULT_STR
import smash.ks.com.ext.const.UniqueId

data class KsParam(
    val imageId: UniqueId = DEFAULT_LONG,
    val name: String = DEFAULT_STR,
    val imageUri: String = DEFAULT_STR
) : Parameterable {
    companion object {
        const val PARAM_ID = "id"
        const val PARAM_NAME = "name"
        const val PARAM_URI = "uri"
    }

    override fun toParameter() = hashMapOf(
        PARAM_ID to imageId.toString(),
        PARAM_NAME to name,
        PARAM_URI to imageUri
    )

    override fun toAnyParameter() = throw UnsupportedOperationException()
}
