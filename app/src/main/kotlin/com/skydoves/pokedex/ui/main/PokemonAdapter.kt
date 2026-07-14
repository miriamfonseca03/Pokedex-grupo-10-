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

import android.os.SystemClock
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.skydoves.bindables.BindingListAdapter
import com.skydoves.bindables.binding
import com.skydoves.pokedex.R
import com.skydoves.pokedex.core.model.Pokemon
import com.skydoves.pokedex.databinding.ItemPokemonBinding
import com.skydoves.pokedex.ui.details.DetailActivity

class PokemonAdapter(
  private val viewModel: MainViewModel,
) : BindingListAdapter<Pokemon, PokemonAdapter.PokemonViewHolder>(diffUtil) {

  private var onClickedAt = 0L

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder =
    parent.binding<ItemPokemonBinding>(R.layout.item_pokemon).let {
      it.vm = viewModel
      PokemonViewHolder(it)
    }

  override fun onBindViewHolder(holder: PokemonViewHolder, position: Int) =
    holder.bindPokemon(getItem(position))

  inner class PokemonViewHolder constructor(
    private val binding: ItemPokemonBinding,
  ) : RecyclerView.ViewHolder(binding.root) {

    init {
      // Abre o ecrã de detalhes ao clicar no cartão.
      binding.root.setOnClickListener {
        val position = bindingAdapterPosition.takeIf { it != NO_POSITION }
          ?: return@setOnClickListener
        val currentClickedAt = SystemClock.elapsedRealtime()
        if (currentClickedAt - onClickedAt > binding.transformationLayout.duration) {
          DetailActivity.startActivity(binding.transformationLayout, getItem(position))
          onClickedAt = currentClickedAt
        }
      }


      // Permite favoritar diretamente da lista através do ícone da estrela.
      //binding.vm é o MainViewModel passado pelo DataBinding.
      binding.favoriteIndicator.setOnClickListener {
        val position = bindingAdapterPosition.takeIf { it != NO_POSITION }
          ?: return@setOnClickListener
        binding.vm?.toggleFavorite(getItem(position))
      }
    }

    fun bindPokemon(pokemon: Pokemon) {
      binding.pokemon = pokemon
      binding.executePendingBindings()
    }
  }

  companion object {
    private val diffUtil = object : DiffUtil.ItemCallback<Pokemon>() {

      override fun areItemsTheSame(oldItem: Pokemon, newItem: Pokemon): Boolean =
        oldItem.name == newItem.name

      override fun areContentsTheSame(oldItem: Pokemon, newItem: Pokemon): Boolean =
        oldItem == newItem
    }
  }
}