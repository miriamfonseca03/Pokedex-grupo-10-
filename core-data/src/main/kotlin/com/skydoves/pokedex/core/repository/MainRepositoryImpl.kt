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

package com.skydoves.pokedex.core.repository

import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.skydoves.pokedex.core.database.PokemonDao
import com.skydoves.pokedex.core.database.entity.mapper.asDomain
import com.skydoves.pokedex.core.database.entity.mapper.asEntity
import com.skydoves.pokedex.core.model.Pokemon
import com.skydoves.pokedex.core.network.Dispatcher
import com.skydoves.pokedex.core.network.PokedexAppDispatchers
import com.skydoves.pokedex.core.network.service.PokedexClient
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.message
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject

@VisibleForTesting
class MainRepositoryImpl @Inject constructor(
  private val pokedexClient: PokedexClient,
  private val pokemonDao: PokemonDao,
  @Dispatcher(PokedexAppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : MainRepository {

  @WorkerThread
  override fun fetchPokemonList(
    page: Int,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onError: (String?) -> Unit,
  ): Flow<List<Pokemon>> = flow {
    val pokemons = pokemonDao.getPokemonList(page)
    if (pokemons.isEmpty()) {
      val response = pokedexClient.fetchPokemonList(page = page)
      response.suspendOnSuccess {
        val results = data.results
        results.forEach { pokemon -> pokemon.page = page }
        pokemonDao.insertPokemonList(results.asEntity())
      }.onFailure {
        onError(message())
      }
    }
    
    // Notifica a UI que a carga inicial de dados terminou para esconder o ProgressBar.
    onComplete()
    
    // emitAll estabelece uma ponte reativa com a base de dados.
    // Sempre que a tabela PokemonEntity mudar (ex: marcar favorito), este Flow emite a lista atualizada automaticamente.
    emitAll(pokemonDao.getAllPokemonListFlow(page).map { it.asDomain() })
  }.onStart { onStart() }.flowOn(ioDispatcher)

  @WorkerThread
  override fun fetchFavoritePokemonList(): Flow<List<Pokemon>> =
    pokemonDao.getFavoritePokemons().map { it.asDomain() }.flowOn(ioDispatcher)

  /**
   * Atualiza o estado de favorito de um Pokémon na base de dados local.
   * Esta operação é executada na IO thread para não bloquear a interface.
   */
  @WorkerThread
  override suspend fun updateFavorite(name: String, isFavorite: Boolean) = withContext(ioDispatcher) {
    pokemonDao.updateFavorite(name, isFavorite)
  }
}
