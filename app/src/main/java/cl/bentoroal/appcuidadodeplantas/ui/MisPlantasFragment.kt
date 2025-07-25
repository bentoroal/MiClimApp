package cl.bentoroal.appcuidadodeplantas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cl.bentoroal.appcuidadodeplantas.R

class MisPlantasFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mis_plantas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 🌱 Aquí podrás agregar lógica para mostrar tus plantas
        // Por ahora es solo un placeholder visual
    }
}