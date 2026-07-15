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

package com.skydoves.pokedex.ui.main

import androidx.annotation.MainThread
import androidx.databinding.Bindable
import androidx.lifecycle.viewModelScope
import com.skydoves.bindables.BindingViewModel
import com.skydoves.bindables.asBindingProperty
import com.skydoves.bindables.bindingProperty
import com.skydoves.pokedex.core.model.Pokemon
import com.skydoves.pokedex.core.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  private val mainRepository: MainRepository,
) : BindingViewModel() {

  @get:Bindable
  var isLoading: Boolean by bindingProperty(false)
    private set

  @get:Bindable
  var toastMessage: String? by bindingProperty(null)
    private set

  private val pokemonFetchingIndex: MutableStateFlow<Int> = MutableStateFlow(0)

  // Estado da barra de pesquisa
  private val searchQuery: MutableStateFlow<String> = MutableStateFlow("")

  // Nomes devolvidos pela PokeAPI para o tipo selecionado
  private val typeFilteredNames: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

  private val isFavoriteFilter: MutableStateFlow<Boolean> = MutableStateFlow(false)

  private val pokemonListFlow = pokemonFetchingIndex.flatMapLatest { page ->
    mainRepository.fetchPokemonList(
      page = page,
      onStart = { isLoading = true },
      onComplete = { isLoading = false },
      onError = { toastMessage = it },
    )
  }

  // Lista final: combina pesquisa por nome, filtro por tipo e filtro de favoritos.
  // Cada combine reage automaticamente sempre que o valor correspondente muda,
  // sem necessidade de recalcular ou sincronizar manualmente.
  private val filteredPokemonListFlow = pokemonListFlow
    .combine(searchQuery) { list, query ->
      if (query.isEmpty()) list
      else list.filter { it.name.contains(query, ignoreCase = true) }
    }
    .combine(typeFilteredNames) { list, typeNames ->
      if (typeNames.isEmpty()) list
      else list.filter { typeNames.contains(it.name) }
    }
    .combine(isFavoriteFilter) { list, favoritesOnly ->
      if (favoritesOnly) list.filter { it.isFavorite }
      else list
    }

  @get:Bindable
  val pokemonList: List<Pokemon> by filteredPokemonListFlow.asBindingProperty(viewModelScope, emptyList())

  @get:Bindable
  var isFavoriteFilterEnabled: Boolean by bindingProperty(false)
    private set

  init {
    Timber.d("init MainViewModel")
  }

  @MainThread
  fun fetchNextPokemonList() {
    if (!isLoading && !isFavoriteFilterEnabled) {
      pokemonFetchingIndex.value++
    }
  }

  fun toggleFavoriteFilter(favoritesOnly: Boolean) {
    isFavoriteFilterEnabled = favoritesOnly
    isFavoriteFilter.value = favoritesOnly
  }

  fun toggleFavorite(pokemon: Pokemon) {
    viewModelScope.launch {
      mainRepository.updateFavorite(pokemon.name, !pokemon.isFavorite)
    }
  }

  // Atualiza o texto de pesquisa a cada letra escrita
  fun searchPokemon(query: String) {
    searchQuery.value = query
  }

  // Pesquisa os Pokémon de um tipo na PokeAPI e atualiza o filtro.
  // Corre em Dispatchers.IO porque é uma chamada de rede síncrona.
  fun filterByType(type: String) {
    if (type.isEmpty()) {
      typeFilteredNames.value = emptyList()
      return
    }
    viewModelScope.launch {
      try {
        val names = withContext(Dispatchers.IO) {
          val client = OkHttpClient()
          val request = Request.Builder()
            .url("https://pokeapi.co/api/v2/type/$type")
            .build()
          val response = client.newCall(request).execute()
          val body = response.body?.string() ?: return@withContext emptyList()
          val json = JSONObject(body)
          val pokemonArray = json.getJSONArray("pokemon")
          val nameList = mutableListOf<String>()
          for (i in 0 until pokemonArray.length()) {
            val slot = pokemonArray.getJSONObject(i)
            val name = slot.getJSONObject("pokemon").getString("name")
            nameList.add(name)
          }
          nameList
        }
        typeFilteredNames.value = names
      } catch (e: Exception) {
        Timber.e(e, "Erro ao filtrar por tipo")
        toastMessage = "Erro ao filtrar por tipo"
      }
    }
  }
}