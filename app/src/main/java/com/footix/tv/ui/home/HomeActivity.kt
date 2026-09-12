package com.footix.tv.ui.home

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.footix.tv.R

class HomeActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Le logo de demarrage n'est qu'un fond de fenetre : on repasse au theme
        // normal une fois la fenetre creee, le relais etant pris par le logo
        // pulsant de l'ecran d'accueil.
        setTheme(R.style.Theme_Footix)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }
}
