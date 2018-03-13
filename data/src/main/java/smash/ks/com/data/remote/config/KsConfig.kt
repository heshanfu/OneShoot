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

package smash.ks.com.data.remote.config

class KsConfig : ApiConfig {
    companion object {
        const val API_REQUEST = "/ks/v1"
        // All basic http api url of Search Music.
        private val BASE_URL = "http://www.taiwan.no.one"
    }

    override val apiBaseUrl: String = BASE_URL
}