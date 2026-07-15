package com.skydoves.pokedex.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import com.google.android.material.chip.Chip
import com.skydoves.bindables.BindingActivity
import com.skydoves.pokedex.R
import com.skydoves.pokedex.databinding.ActivityMainBinding
import com.skydoves.transformationlayout.onTransformationStartContainer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BindingActivity<ActivityMainBinding>(R.layout.activity_main) {

  @get:VisibleForTesting
  internal val viewModel: MainViewModel by viewModels()

  // Tipos disponíveis para o filtro, usados para gerar os chips dinamicamente
  private val pokemonTypes = listOf(
    "fire", "water", "grass", "electric", "ice",
    "fighting", "poison", "ground", "flying", "psychic",
    "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy", "normal"
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    onTransformationStartContainer()
    super.onCreate(savedInstanceState)
    binding {
      adapter = PokemonAdapter(viewModel)
      vm = viewModel

      // Cria um chip por tipo e liga-o ao filtro no ViewModel
      pokemonTypes.forEach { type ->
        val chip = Chip(this@MainActivity)
        chip.text = type.replaceFirstChar { it.uppercase() }
        chip.isCheckable = true
        chip.setOnCheckedChangeListener { _, isChecked ->
          if (isChecked) {
            viewModel.filterByType(type)
          } else {
            viewModel.filterByType("")
          }
        }
        typeChipGroup.addView(chip)
      }

      // Navegação inferior: Pokédex (estado inicial), Pesquisar, Favoritos (Daniel)
      bottomNavigation.setOnItemSelectedListener { item ->
        when (item.itemId) {
          R.id.nav_pokedex -> {
            searchCard.visibility = View.GONE
            typeFilterScroll.visibility = View.GONE
            searchView.setText("")
            viewModel.searchPokemon("")
            viewModel.filterByType("")
            viewModel.toggleFavoriteFilter(false)
            typeChipGroup.clearCheck()
            true
          }
          R.id.nav_search -> {
            searchCard.visibility = View.VISIBLE
            typeFilterScroll.visibility = View.VISIBLE
            viewModel.toggleFavoriteFilter(false)
            searchView.requestFocus()
            true
          }
          R.id.nav_favorites -> {
            searchCard.visibility = View.GONE
            typeFilterScroll.visibility = View.GONE
            viewModel.toggleFavoriteFilter(true)
            true
          }
          else -> false
        }
      }

      // Atualiza a pesquisa a cada letra escrita
      searchView.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
          viewModel.searchPokemon(s.toString())
        }
      })
    }
  }
}