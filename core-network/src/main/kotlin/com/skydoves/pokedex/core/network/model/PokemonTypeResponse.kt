/*
 * Designed and developed by 2022 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.pokedex.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PokemonTypeResponse(
  @field:Json(name = "pokemon") val pokemon: List<TypePokemonSlot>
)

@JsonClass(generateAdapter = true)
data class TypePokemonSlot(
  @field:Json(name = "pokemon") val pokemon: TypePokemonEntry
)

@JsonClass(generateAdapter = true)
data class TypePokemonEntry(
  @field:Json(name = "name") val name: String,
  @field:Json(name = "url") val url: String
)