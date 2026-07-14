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
  private val searchQuery: MutableStateFlow<String> = MutableStateFlow("")
  private val typeFilteredNames: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
  
  // StateFlow que controla se o ecrã deve mostrar apenas os favoritos ou a lista normal.
  private val isFavoriteFilter: MutableStateFlow<Boolean> = MutableStateFlow(false)

  private val pokemonListFlow = combine(pokemonFetchingIndex, isFavoriteFilter) { page, favoritesOnly ->
    page to favoritesOnly
  }.flatMapLatest { (page, favoritesOnly) ->
    if (favoritesOnly) {
      mainRepository.fetchFavoritePokemonList()
    } else {
      mainRepository.fetchPokemonList(
        page = page,
        onStart = { isLoading = true },
        onComplete = { isLoading = false },
        onError = { toastMessage = it },
      )
    }
  }

  // Lógica de filtragem combinada: aplica busca por nome e filtro por tipo.
  // O filtro de favoritos agora é gerido diretamente pela fonte de dados (Repository/DAO).
  private val filteredPokemonListFlow = pokemonListFlow
    .combine(searchQuery) { list, query ->
      if (query.isEmpty()) list
      else list.filter { it.name.contains(query, ignoreCase = true) }
    }
    .combine(typeFilteredNames) { list, typeNames ->
      if (typeNames.isEmpty()) list
      else list.filter { typeNames.contains(it.name) }
    }

  @get:Bindable
  val pokemonList: List<Pokemon> by filteredPokemonListFlow.asBindingProperty(viewModelScope, emptyList())

  /**
   * Propriedade observável pelo DataBinding para exibir a mensagem "No favorites yet".
   */
  @get:Bindable
  var isFavoriteFilterEnabled: Boolean by bindingProperty(false)
    private set

  init {
    Timber.d("init MainViewModel")
  }

  @MainThread
  fun fetchNextPokemonList() {
    // Evita carregar novas páginas da API se estivermos a visualizar apenas os favoritos.
    if (!isLoading && !isFavoriteFilterEnabled) {
      pokemonFetchingIndex.value++
    }
  }

  /**
   * Alterna o estado do filtro de favoritos e notifica a UI através do DataBinding.
   */
  fun toggleFavoriteFilter(favoritesOnly: Boolean) {
    isFavoriteFilterEnabled = favoritesOnly
    isFavoriteFilter.value = favoritesOnly
  }

  /**
   * Executa a inversão do estado de favorito de um Pokémon através do repositório.
   */
  fun toggleFavorite(pokemon: Pokemon) {
    viewModelScope.launch {
      mainRepository.updateFavorite(pokemon.name, !pokemon.isFavorite)
    }
  }

  fun searchPokemon(query: String) {
    searchQuery.value = query
  }

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
        toastMessage = "Erro ao filtrar por tipo"
      }
    }
  }
}
